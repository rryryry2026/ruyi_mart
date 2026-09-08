package com.ruyi.ruyi_mart.module.refund.vo;

import com.ruyi.ruyi_mart.module.refund.entity.Refund;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 管理端退款视图：补充关联订单号与买家昵称，供审核列表展示
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class RefundAdminVO extends Refund {

    /**关联订单号*/
    private String orderNo;

    /**买家昵称*/
    private String buyerNickname;
}
