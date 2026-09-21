package com.ruyi.ruyi_mart.module.stock.vo;

import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import lombok.Data;

/**
 * 用户端库存视图：只暴露"还剩几件"。
 *
 * 原先 /stock/info 直接返回 Stock 实体，而这个接口在 SecurityConfig 里是 permitAll、
 * 又没有 @PreAuthorize，等于未登录就能拿到 total（总进货量）、locked（未支付订单的预扣量）
 * 和 version —— 其中 locked 约等于"此刻有多少人正在下单还没付款"，
 * 属于经营数据，定时抓一遍就能推断销量走势。前端商品详情只读 available（"仅剩 N 件"），
 * 所以这里只回这一个字段。
 */
/**用户端库存VO。*/
@Data
public class StockInfoVO {

    private Long productId;

    /**可用库存；null=该商品尚未初始化库存*/
    private Integer available;

    /**由实体转视图；无库存记录时返回 null（前端按"未初始化"处理）*/
    public static StockInfoVO from(Stock stock){
        if(stock == null){
            return null;
        }
        StockInfoVO vo = new StockInfoVO();
        vo.setProductId(stock.getProductId());
        vo.setAvailable(stock.getAvailable());
        return vo;
    }
}
