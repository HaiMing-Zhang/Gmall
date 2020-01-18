package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.SkuService;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class SkuController {
    @Reference
    private SkuService skuService;
    @Reference
    private ListService listService;

    /**
     * sku中加载spu的图片
     * @param spuImage
     * @return
     */
    @GetMapping("/spuImageList")
    public List<SpuImage> spuImageList(SpuImage spuImage){
        List<SpuImage> spuImageList=skuService.getSpuImageList(spuImage);
        return spuImageList;
    }
    /**
     * sku中加载销售属性下拉列表
     * @param spuId
     * @return
     */
    @GetMapping("/spuSaleAttrList")
    public List<SpuSaleAttr> spuSaleAttrList(String spuId){
        List<SpuSaleAttr> spuSaleAttrList=skuService.getSpuSaleAttrList(spuId);
        return spuSaleAttrList;
    }

    /**
     * 保存sku
     * @param
     */
    @PostMapping("/saveSkuInfo")
    public void saveSkuInfo(@RequestBody SkuInfo skuInfo){
        skuService.saveSkuInfo(skuInfo);
    }

    /**
     *上架商品,将数据保存在es中
     */
    @RequestMapping("/onSale")
    public void onSale(String skuId){
        SkuLsInfo skuLsInfo = new SkuLsInfo();
        SkuInfo skuInfo = skuService.getSkuInfo(skuId);
        //从数据库中查询拿出数据拷贝到skuLsInfo中
        BeanUtils.copyProperties(skuInfo,skuLsInfo);
        listService.saveSkuInfo(skuLsInfo);
    }
}
