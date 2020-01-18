package com.atguigu.gmall.manage.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.manage.mapper.*;
import com.atguigu.gmall.service.SpuService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
@Service
public class SpuServiceImpl implements SpuService {
    @Autowired
    private SpuInfoMapper spuInfoMapper;
    @Autowired
    private BaseSaleAttrMapper baseSaleAttrMapper;
    @Autowired
    private SpuSaleAttrMapper spuSaleAttrMapper;
    @Autowired
    private SpuSaleAttrValueMapper spuSaleAttrValueMapper;
    @Autowired
    private SpuImageMapper spuImageMapper;
    /**
     * 查询spu
     * @param catalog3Id
     * @return
     */
    @Override
    public List<SpuInfo> getSpuList(String catalog3Id) {
        SpuInfo spuInfo = new SpuInfo();
        spuInfo.setCatalog3Id(catalog3Id);
        List<SpuInfo> spuInfoList = spuInfoMapper.select(spuInfo);
        return spuInfoList;
    }
    /**
     * 获取spu中所有销售属性名称
     * @return
     */
    @Override
    public List<BaseSaleAttr> getBaseSaleAttrList() {
        List<BaseSaleAttr> baseSaleAttrList = baseSaleAttrMapper.selectAll();
        return baseSaleAttrList;
    }

    /**
     * 保存spu
     * @param spuInfo
     */
    @Override
    public void saveSpuInfo(SpuInfo spuInfo) {
        if(spuInfo.getId() != null){
            spuInfoMapper.updateByPrimaryKeySelective(spuInfo);
        }else{
            spuInfoMapper.insertSelective(spuInfo);
        }
        //插入之前先删除SpuSaleAttr,这样的话修改也能用此方法
        SpuSaleAttr spuSaleAttr1 = new SpuSaleAttr();
        spuSaleAttr1.setSpuId(spuInfo.getId());
        spuSaleAttrMapper.delete(spuSaleAttr1);
        List<SpuSaleAttr> spuSaleAttrList = spuInfo.getSpuSaleAttrList();
        //判断spuSaleAttrList是否为空,spuSaleAttrList为销售属性的集合
        if( spuSaleAttrList != null && spuSaleAttrList.size() > 0){
            //循环遍历将spuSaleAttrList中的销售属性添加至数据库
            for (SpuSaleAttr spuSaleAttr : spuSaleAttrList) {
                //设置spuSaleAttr是属于哪个spu
                spuSaleAttr.setSpuId(spuInfo.getId());
                spuSaleAttrMapper.insertSelective(spuSaleAttr);
                //插入之前先删除SpuSaleAttrValue,这样的话修改也能用此方法
                SpuSaleAttrValue spuSaleAttrValue1 = new SpuSaleAttrValue();
                spuSaleAttrValue1.setSaleAttrId(spuSaleAttr.getId());
                spuSaleAttrValueMapper.delete(spuSaleAttrValue1);
                //获取此销售属性的属性值,是一个集合
                List<SpuSaleAttrValue> spuSaleAttrValueList = spuSaleAttr.getSpuSaleAttrValueList();
                //判断是否为空,不为空的话就遍历添加至数据库
                if(spuSaleAttrValueList!=null && spuSaleAttrValueList.size()>0){
                    for (SpuSaleAttrValue spuSaleAttrValue : spuSaleAttrValueList) {
                        //设置此销售属性值是属于哪个spu和SaleAttr销售属性
                        spuSaleAttrValue.setSpuId(spuInfo.getId());
                        spuSaleAttrValue.setSaleAttrId(spuSaleAttr.getSaleAttrId());
                        spuSaleAttrValueMapper.insertSelective(spuSaleAttrValue);
                    }
                }
            }
        }
        //插入之前先删除SpuImage,这样的话修改也能用此方法
        SpuImage spuImage1 = new SpuImage();
        spuImage1.setSpuId(spuInfo.getId());
        spuImageMapper.delete(spuImage1);
        List<SpuImage> spuImageList = spuInfo.getSpuImageList();
        if(spuImageList!= null && spuImageList.size()>0){
            for (SpuImage spuImage : spuImageList) {
                spuImage.setSpuId(spuInfo.getId());
                spuImageMapper.insertSelective(spuImage);
            }
        }
    }
}
