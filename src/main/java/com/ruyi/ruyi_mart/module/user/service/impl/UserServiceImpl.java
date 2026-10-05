package com.ruyi.ruyi_mart.module.user.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ruyi.ruyi_mart.common.enums.ResultCode;
import com.ruyi.ruyi_mart.common.exception.BusinessException;
import com.ruyi.ruyi_mart.module.user.dto.RegisterRequest;
import com.ruyi.ruyi_mart.module.user.dto.UpdateProfileDTO;
import com.ruyi.ruyi_mart.module.user.dto.UserAdminQueryDTO;
import com.ruyi.ruyi_mart.module.user.entity.User;
import com.ruyi.ruyi_mart.module.user.mapper.UserMapper;
import com.ruyi.ruyi_mart.module.user.service.RefreshTokenStore;
import com.ruyi.ruyi_mart.module.user.service.UserService;
import com.ruyi.ruyi_mart.module.user.vo.UserAdminVO;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**用户模块：账号资料自助管理 + 管理端管人。*/
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    /**用户类型：1=系统管理员，2=普通买家（与 user.user_type 的取值一致）*/
    private static final int USER_TYPE_NORMAL = 2;

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private RefreshTokenStore refreshTokenStore;

    /**注册：用户名查重、密码加密后落库*/
    @Override
    public void register(RegisterRequest req){
        long count = this.lambdaQuery()
                .eq(User::getUsername, req.getUsername())
                .count();
        if(count > 0){
            throw new BusinessException(ResultCode.FAIL,"用户名已存在");
        }

        User user = new User();
        user.setUsername(req.getUsername());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setNickname(req.getNickname());
        user.setPhone(req.getPhone());
        //注册一律是普通买家，不给注册接口留造管理员的口子
        user.setUserType(USER_TYPE_NORMAL);
        try {
            this.save(user);
        } catch (DuplicateKeyException e) {
            // 并发注册同名/同手机号时，查重和插入之间有窗口，唯一索引是最后一道闸。
            // 这里转成业务提示而不是 500。
            // （本方法没有 @Transactional，catch 不会碰到"事务已被标记回滚"的问题）
            throw new BusinessException(ResultCode.FAIL,"用户名或手机号已被使用");
        }
    }

    /**修改密码*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(Long userId, String oldPassword, String newPassword){
        User user = baseMapper.selectById(userId);
        if(user == null){
            throw new BusinessException(ResultCode.NOT_FIND, "用户不存在");
        }
        if(!passwordEncoder.matches(oldPassword, user.getPassword())){
            throw new BusinessException(ResultCode.FAIL, "原密码错误");
        }
        // 只更新密码和更新时间两列：拿来做比对的 user 是快照，
        // updateById 会把快照里的其它列（如管理员并发改过的 status）原样写回，覆盖别人的修改
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getPassword, passwordEncoder.encode(newPassword))
                .set(User::getUpdateTime, LocalDateTime.now())
                .update();

        // 改完密码必须把 refreshToken 作废。
        // 否则账号被盗时改了密码也踢不掉对方：他手里那张 refreshToken 还能一直续期，
        // 等于密码白改（要拖到令牌自然过期为止）。
        refreshTokenStore.remove(userId);
    }

    /**修改个人资料*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(Long userId, UpdateProfileDTO dto){
        User user = baseMapper.selectById(userId);
        if(user == null){
            throw new BusinessException(ResultCode.NOT_FIND, "用户不存在");
        }
        if(dto.getNickname() == null && dto.getPhone() == null){
            throw new BusinessException(ResultCode.FAIL, "至少填写一项");
        }

        if(dto.getNickname() != null){
            user.setNickname(dto.getNickname());
        }
        if(dto.getPhone() != null){
            user.setPhone(dto.getPhone());
        }
        // 只 set 传了的两列 + 更新时间，不整行写回（同 changePassword 的理由）
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(dto.getNickname() != null, User::getNickname, dto.getNickname())
                .set(dto.getPhone() != null, User::getPhone, dto.getPhone())
                .set(User::getUpdateTime, LocalDateTime.now())
                .update();
    }

    /**管理端：用户分页（脱敏，不含密码字段）*/
    @Override
    public Page<UserAdminVO> adminPageUsers(UserAdminQueryDTO dto){
        Page<User> page = new Page<>(dto.getPageNum(), dto.getPageSize());

        var query = this.lambdaQuery();
        if(StringUtils.hasText(dto.getKeyword())){
            // 用 and(...) 把三个 OR 条件包成一组括号，
            // 否则会和后面的类型/状态条件混在一起，变成 "类型=x AND 用户名 LIKE y OR 昵称 LIKE y" 的错误优先级
            query.and(w -> w.like(User::getUsername, dto.getKeyword())
                    .or().like(User::getNickname, dto.getKeyword())
                    .or().like(User::getPhone, dto.getKeyword()));
        }
        if(dto.getUserType() != null){
            query.eq(User::getUserType, dto.getUserType());
        }
        if(dto.getStatus() != null){
            query.eq(User::getStatus, dto.getStatus());
        }
        query.orderByDesc(User::getCreateTime);
        //链式包装器要用它自己的 page()：当参数传给 this.page(...) 会报
        //"can not use this method for getSqlFirst"（链式包装器不支持那几个方法）
        query.page(page);

        Page<UserAdminVO> voPage = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        List<UserAdminVO> vos = new ArrayList<>();
        for(User u : page.getRecords()){
            UserAdminVO vo = new UserAdminVO();
            //按属性名搬：VO 的字段清单就是脱敏白名单，
            //实体里的 password / deleted 因为 VO 没有对应字段，搬不出去
            BeanUtils.copyProperties(u, vo);
            vos.add(vo);
        }
        voPage.setRecords(vos);
        return voPage;
    }

    /**管理端：启用/禁用用户*/
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserStatus(Long userId, Integer status){
        if(status == null || (status != 0 && status != 1)){
            throw new BusinessException(ResultCode.FAIL, "status 必须为 0 或 1");
        }
        User user = baseMapper.selectById(userId);
        if(user == null){
            throw new BusinessException(ResultCode.NOT_FIND, "用户不存在");
        }
        //防止把管理员自己禁掉，导致后台再没人能进
        if(user.getUserType() != null && user.getUserType() == 1){
            throw new BusinessException(ResultCode.FAIL, "管理员账号不可禁用");
        }
        // 只更新状态和更新时间两列，不整行写回
        lambdaUpdate()
                .eq(User::getId, userId)
                .set(User::getStatus, status)
                .set(User::getUpdateTime, LocalDateTime.now())
                .update();

        // 禁用账号时顺手作废 refreshToken：否则对方手上的令牌还能续期，
        // 要等令牌自然过期才算真的禁用掉。
        if(status == 0){
            refreshTokenStore.remove(userId);
        }
    }
}
