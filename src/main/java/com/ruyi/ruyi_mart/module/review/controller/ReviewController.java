package com.ruyi.ruyi_mart.module.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.review.dto.AppendProductFirstCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.FirstProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.dto.SecondProductCommentDTO;
import com.ruyi.ruyi_mart.module.review.service.ProductCommentService;
import com.ruyi.ruyi_mart.module.review.vo.ProductAppendCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductFirstCommentVO;
import com.ruyi.ruyi_mart.module.review.vo.ProductSecondCommentVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/review")
public class ReviewController {

    @Autowired
    private ProductCommentService productCommentService;

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    // ============ 1. 发表一级评论（首评） ============
    @PostMapping("/first")
    public Result<Long> saveFirstComment(@Valid @RequestBody FirstProductCommentDTO dto){
        Long id = productCommentService.saveProductFirstComment(currentUserId(),dto);
        return Result.success(id);
    }

    // ============ 2. 发表二级回复 ============
    @PostMapping("/second")
    public Result<Long> saveSecondComment(@Valid @RequestBody SecondProductCommentDTO dto){
        Long id = productCommentService.saveProductSecondComment(currentUserId(),dto);
        return Result.success(id);
    }

    // ============ 3. 追评 ============
    @PostMapping("/first/append")
    public Result<Void> appendFirstComment(@Valid @RequestBody AppendProductFirstCommentDTO dto){
        productCommentService.appendProductFirstComment(currentUserId(),dto);
        return Result.success();
    }

    // ============ 4. 按商品查一级评论（分页 + 排序） ============
    @GetMapping("/first/page")
    public Result<Page<ProductFirstCommentVO>> firstCommentPage(
            @RequestParam Long productId,
            @RequestParam(required = false) Integer sortType,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize){
        Page<ProductFirstCommentVO> page = productCommentService.getProductFirstCommentPage(
                currentUserId(),productId,sortType,pageNum,pageSize);
        return Result.success(page);
    }

    // ============ 5. 查某一级评论下的二级回复（分页） ============
    @GetMapping("/second/page")
    public Result<Page<ProductSecondCommentVO>> secondCommentPage(
            @RequestParam Long firstCommentId,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize){
        Page<ProductSecondCommentVO> page = productCommentService.getSecondCommentPage(
                currentUserId(),firstCommentId,pageNum,pageSize);
        return Result.success(page);
    }

    // ============ 6. 查某一级评论的追评（一对一） ============
    @GetMapping("/append")
    public Result<ProductAppendCommentVO> appendComment(@RequestParam Long firstCommentId){
        return Result.success(productCommentService.getAppendComment(firstCommentId));
    }

    // ============ 7. 统计某商品评论总数 ============
    @GetMapping("/count")
    public Result<Long> commentCount(@RequestParam Long productId){
        return Result.success(productCommentService.getProductCommentCount(productId));
    }

    // ============ 8. 点赞 / 取消点赞 ============
    @PutMapping("/like")
    public Result<Void> like(@RequestParam Long commentId, @RequestParam Integer isLike){
        productCommentService.updateProductCommentLike(currentUserId(),commentId,isLike);
        return Result.success();
    }

}
