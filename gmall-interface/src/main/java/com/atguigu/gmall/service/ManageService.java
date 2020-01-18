package com.atguigu.gmall.service;

import com.atguigu.gmall.bean.*;

import java.util.List;

public interface ManageService {
    /**
     * 查找一级分类
     * @return
     */
    List<BaseCatalog1> getCatalog1();

    /**
     * 查找二级分类节点
     * @param baseCatalog2
     * @return
     */
    List<BaseCatalog2> getCatalog2(BaseCatalog2 baseCatalog2);
    /**
     * 查找三级分类节点
     * @param baseCatalog3
     * @return
     */
    List<BaseCatalog3> getCatalog3(BaseCatalog3 baseCatalog3);

    /**
     * 查询属性名称
     * @param catalog3Id
     * @return
     */
    List<BaseAttrInfo> getAttrInfoList(String catalog3Id);

    /**
     * 查询属性值名称
     * @param attrId
     * @return
     */
    BaseAttrInfo getAttrValueList(String attrId);

    /**
     * 保存属性名称与属性值
     * @param baseAttrInfo
     */
    void saveAttrInfo(BaseAttrInfo baseAttrInfo);

    /**
     *根据多个attrValueId查询多个平台属性
     * @param valueId
     * @return
     */
    List<BaseAttrInfo> getAttrValueList(List<String> valueId);
}
