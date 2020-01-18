package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;

public interface ListService {
    /**
     * 商品上架
     * @param skuLsInfo
     */
     void saveSkuInfo(SkuLsInfo skuLsInfo);

    /**
     * 全文检索es并返回结果
     * @param skuLsParams
     * @return
     */
     SkuLsResult search(SkuLsParams skuLsParams);

    /**
     * 修改商品的热度,热度越高,排名越靠前
     */
    void UpdateHotScore(String skuId);
}
