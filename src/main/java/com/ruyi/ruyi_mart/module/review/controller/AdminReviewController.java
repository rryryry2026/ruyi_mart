package com.ruyi.ruyi_mart.module.review.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.ruyi.ruyi_mart.common.result.Result;
import com.ruyi.ruyi_mart.module.log.annotation.OpLog;
import com.ruyi.ruyi_mart.module.review.dto.AdminReplyDTO;
import com.ruyi.ruyi_mart.module.review.dto.ReviewAdminQueryDTO;
import com.ruyi.ruyi_mart.module.review.service.ProductCommentService;
import com.ruyi.ruyi_mart.module.review.vo.ReviewAdminVO;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端评价接口：全量评价（含已隐藏）分页、显隐、删除、商家回复
 */
@RestController
@RequestMapping("/admin/review")
@PreAuthorize("hasRole('ADMIN')")
public class AdminReviewController {

    @Autowired
    private ProductCommentService productCommentService;

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Long) {
            return (Long) authentication.getPrincipal();
        }
        return null;
    }

    /**评价分页（商品/内容/状态/好评差评筛选，含已隐藏的评论）*/
    @GetMapping("/page")
    public Result<Page<ReviewAdminVO>> page(ReviewAdminQueryDTO dto) {
        return Result.success(productCommentService.adminPageComments(dto));
    }

    /**显示/隐藏评论（隐藏后消费端不可见）*/
    @PutMapping("/{id}/status")
    @OpLog(module = "评价管理", action = "显示/隐藏评价")
    public Result<Void> updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        productCommentService.updateCommentStatus(id, status);
        return Result.success();
    }

    /**删除评论（一级评论会连带删除其二级回复、追评与点赞记录）*/
    @DeleteMapping("/{id}")
    @OpLog(module = "评价管理", action = "删除评价")
    public Result<Void> delete(@PathVariable Long id) {
        productCommentService.deleteComment(id);
        return Result.success();
    }

    /**商家回复（以当前管理员身份回复，昵称取数据库真实值）*/
    @PostMapping("/reply")
    @OpLog(module = "评价管理", action = "商家回复")
    public Result<Long> reply(@Valid @RequestBody AdminReplyDTO dto) {
        return Result.success(productCommentService.adminReply(currentUserId(), dto));
    }
}
