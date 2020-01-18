package com.atguigu.gmall.manage.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ManageService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@CrossOrigin
public class ManageController {
    @Reference
    private ManageService manageService;

    /**
     * 查询第一个分类节点
     * @return
     */
    @PostMapping("/getCatalog1")
    public List<BaseCatalog1> getCatalog1(){
        return manageService.getCatalog1();
    }

    /**
     * 查询第二个分类节点
     * @return
     */
    @PostMapping("/getCatalog2")
    public List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2){
        return manageService.getCatalog2(baseCatalog2);
    }
    /**
     * 查询第三个分类节点
     * @return
     */
    @PostMapping("/getCatalog3")
    public List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3){
        return manageService.getCatalog3(baseCatalog3);
    }
    /**
     * 查询属性名称
     * @return
     */
    @GetMapping("/attrInfoList")
    public List<BaseAttrInfo> attrInfoList(String catalog3Id){
        return manageService.getAttrInfoList(catalog3Id);
    }
    /**
     * 查询属性值名称,按照业务逻辑,应该先查出属性,在查属性值
     * @return
     */
    @PostMapping("/getAttrValueList")
    public List<BaseAttrValue> getAttrValueList(String attrId){
        BaseAttrInfo baseAttrInfo=manageService.getAttrValueList(attrId);
        return baseAttrInfo.getAttrValueList();
    }

    /**
     * 保存属性名称与属性值
     * @param baseAttrInfo
     */
    @PostMapping("/saveAttrInfo")
    public void saveAttrInfo(@RequestBody BaseAttrInfo baseAttrInfo){
        manageService.saveAttrInfo(baseAttrInfo);
    }



}
