package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.bean.SpuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SpuSaleAttrMapper extends Mapper<SpuSaleAttr> {

    List<SpuSaleAttr> selectSpuSaleAttrBySpuId(String spuId);
    //加载页面时显示销售属性与销售属性值
    List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(String id, String spuId);
}
