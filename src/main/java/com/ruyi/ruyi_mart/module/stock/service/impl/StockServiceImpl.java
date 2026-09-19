package com.ruyi.ruyi_mart.module.stock.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.module.category.entity.Category;
import com.ruyi.ruyi_mart.module.category.mapper.CategoryMapper;
import com.ruyi.ruyi_mart.module.product.entity.Product;
import com.ruyi.ruyi_mart.module.product.mapper.ProductMapper;
import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import com.ruyi.ruyi_mart.module.stock.mapper.StockMapper;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import com.ruyi.ruyi_mart.module.stock.vo.ProductStockVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

//商品库存业务真实实现。
@Slf4j
@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;//库存表
    @Autowired
    private ProductMapper productMapper;//商品表
    @Autowired
    private CategoryMapper categoryMapper;//分类表

    //初始化商品库存。
    @Override
    public void initStock(Long productId, Integer total){
        Stock existing = stockMapper.selectById(productId);
        if(existing == null){
            Stock s = new Stock();
            s.setProductId(productId);
            s.setTotal(total);
            s.setAvailable(total);
            s.setLocked(0);
            s.setCreateTime(LocalDateTime.now());
            s.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(s);
        } else {
            /**
             * 重设总量走单条原子 SQL，不用 updateById。
             * 原因只有一个：updateById 只能写"Java 里算好的值"，
             * 表达不了 available + (新总量 - 旧总量) 这种"基于数据库当前值"的增量计算；
             * 先查出来算好再写回就是读-改-写，并发下会丢更新。
             * （与乐观锁无关：实体上没有 @Version，项目也没注册
             *   OptimisticLockerInnerInterceptor，不存在版本号拦截。）
             */
            stockMapper.resetTotal(productId, total);
        }
    }


    //预扣商品库存。
    @Override
    public boolean tryLock(Long productId,Integer count){
        int rows = stockMapper.preDeduct(productId,count);
        boolean ok = rows > 0;
        if(!ok){
            log.warn("库存预扣失败 productId={} count={} 库存不足", productId, count);
        }
        return ok;
    }

    //确认扣减商品库存。
    @Override
    public void confirm(Long productId,Integer count){
        if(stockMapper.confirmDeduct(productId,count) == 0){
            // SQL 里的 locked >= n 只是边界保护，不是业务幂等：
            // 同一笔订单被确认两次、或确认与退款回补撞在一起时就会拿不到行。
            // 不抛异常（此时订单已完成支付，抛了会回滚一笔正常业务），但要留下痕迹便于对账。
            log.warn("库存确认未生效（locked 不足，疑似重复确认或与退款冲突）productId={} count={}", productId, count);
        }
    }

    //回补商品库存。
    @Override
    public void release(Long productId,Integer count){
        if(stockMapper.rollback(productId,count) == 0){
            // 回补方向出错会让可用库存虚增（比少卖更危险，会导致超卖），必须可观测
            log.warn("库存回补未生效（locked 不足，疑似重复回补）productId={} count={}", productId, count);
        }
    }

    //退款。
    @Override
    public void refund(Long productId, Integer count){
        if(stockMapper.refundBack(productId,count) == 0){
            // 原样回补都会超过 total，说明这单的库存早被补过一次了
            log.warn("退款回补未生效（超过库存总量，疑似重复回补）productId={} count={}", productId, count);
        }
    }

    //查看商品库存明细。
    @Override
    public Stock getByProductId(Long productId){
        return stockMapper.selectById(productId);
    }

    //商品维度库存分页
    @Override
    public Page<ProductStockVO> adminStockPage(String keyword, Long categoryId, int pageNum, int pageSize){
        // 以商品为主表分页，再左连库存——没初始化过库存的商品也能列出来
        Page<Product> productPage = new Page<>(pageNum, pageSize);
        QueryWrapper<Product> qw = new QueryWrapper<>();
        if(StringUtils.hasText(keyword)){
            qw.like("name", keyword);
        }
        if(categoryId != null){
            qw.eq("category_id", categoryId);
        }
        qw.orderByDesc("create_time");
        productMapper.selectPage(productPage, qw);

        Page<ProductStockVO> voPage = new Page<>(productPage.getCurrent(), productPage.getSize(), productPage.getTotal());
        List<Product> products = productPage.getRecords();
        if(products.isEmpty()){
            voPage.setRecords(new ArrayList<>());
            return voPage;
        }

        Set<Long> productIds = products.stream().map(Product::getId).collect(Collectors.toSet());
        Map<Long, Stock> stockMap = stockMapper.selectBatchIds(productIds).stream()
                .collect(Collectors.toMap(Stock::getProductId, Function.identity()));
        Set<Long> categoryIds = products.stream()
                .map(Product::getCategoryId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, String> categoryNameMap = categoryIds.isEmpty() ? Collections.emptyMap()
                : categoryMapper.selectBatchIds(categoryIds).stream()
                        .collect(Collectors.toMap(Category::getId, Category::getName));

        List<ProductStockVO> vos = new ArrayList<>();
        for(Product p : products){
            ProductStockVO vo = new ProductStockVO();
            vo.setProductId(p.getId());
            vo.setProductName(p.getName());
            vo.setImageUrl(p.getImageUrl());
            vo.setCategoryId(p.getCategoryId());
            vo.setCategoryName(categoryNameMap.get(p.getCategoryId()));
            Stock s = stockMap.get(p.getId());
            if(s != null){
                vo.setTotal(s.getTotal());
                vo.setAvailable(s.getAvailable());
                vo.setLocked(s.getLocked());
                vo.setUpdateTime(s.getUpdateTime());
            }
            vos.add(vo);
        }
        voPage.setRecords(vos);
        return voPage;
    }
}
