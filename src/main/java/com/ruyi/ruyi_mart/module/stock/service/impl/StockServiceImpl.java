package com.ruyi.ruyi_mart.module.stock.service.impl;

import com.ruyi.ruyi_mart.module.stock.entity.Stock;
import com.ruyi.ruyi_mart.module.stock.mapper.StockMapper;
import com.ruyi.ruyi_mart.module.stock.service.StockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;

    @Override
    public void initStock(Long productId, Integer total){
        Stock existing = stockMapper.selectById(productId);
        if(existing == null){
            Stock s = new Stock();
            s.setProductId(productId);
            s.setTotal(total);
            s.setAvailable(total);
            s.setLocked(0);
            s.setVersion(0);
            s.setCreateTime(LocalDateTime.now());
            s.setUpdateTime(LocalDateTime.now());
            stockMapper.insert(s);
        } else {
            int diff = total - existing.getTotal();
            existing.setTotal(total);
            existing.setAvailable(existing.getAvailable() + diff);
            existing.setUpdateTime(LocalDateTime.now());
            stockMapper.updateById(existing);
        }
    }


    @Override
    public boolean tryLock(Long productId,Integer count){
        int rows = stockMapper.preDeduct(productId,count);
        boolean ok = rows > 0;
        if(!ok){
            log.warn("库存预扣失败 productId={} count={} 库存不足", productId, count);
        }
        return ok;
    }

    @Override
    public void confirm(Long productId,Integer count){
        stockMapper.confirmDeduct(productId,count);
    }

    @Override
    public void release(Long productId,Integer count){
        stockMapper.rollback(productId,count);
    }

    @Override
    public Stock getByProductId(Long productId){
        return stockMapper.selectById(productId);
    }
}
