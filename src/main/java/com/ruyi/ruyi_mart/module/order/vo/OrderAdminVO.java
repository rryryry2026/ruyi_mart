package com.ruyi.ruyi_mart.module.order.vo;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端订单视图：在用户端 OrderVO 基础上补充买家信息
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class OrderAdminVO extends OrderVO {

    /**买家登录名*/
    private String buyerUsername;

    /**买家昵称*/
    private String buyerNickname;
}
