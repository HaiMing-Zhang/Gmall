package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.BaseSaleAttr;
import com.atguigu.gmall.bean.SpuInfo;
import com.atguigu.gmall.service.SpuService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class SpuController {
    @Reference
    private SpuService spuService;
    /**
     * 查询spu
     * @return
     */
    @GetMapping("/spuList")
    public List<SpuInfo> spuList(String catalog3Id){
        return spuService.getSpuList(catalog3Id);
    }
    /**
     * 获取spu中销售属性名称
     * @return
     */
    @PostMapping("/baseSaleAttrList")
    public List<BaseSaleAttr> baseSaleAttrList(){
        //根据业务逻辑,应先获取属性,在利用属性,查询属性值
        List<BaseSaleAttr> baseSaleAttrList = spuService.getBaseSaleAttrList();
        return baseSaleAttrList;
    }
    /**
     * 保存spu
     * 涉及的表:
     *      spu_info,spu_image,spuSaleAttrValue
     * @return
     */
    @PostMapping("/saveSpuInfo")
    public void saveSpuInfo(@RequestBody SpuInfo spuInfo){
        spuService.saveSpuInfo(spuInfo);
    }

}
