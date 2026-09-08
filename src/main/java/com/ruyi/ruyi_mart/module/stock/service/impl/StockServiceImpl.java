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

@Slf4j
@Service
public class StockServiceImpl implements StockService {

    @Autowired
    private StockMapper stockMapper;
    @Autowired
    private ProductMapper productMapper;
    @Autowired
    private CategoryMapper categoryMapper;

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
    public void refund(Long productId, Integer count){
        stockMapper.refundBack(productId,count);
    }

    @Override
    public Stock getByProductId(Long productId){
        return stockMapper.selectById(productId);
    }

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
