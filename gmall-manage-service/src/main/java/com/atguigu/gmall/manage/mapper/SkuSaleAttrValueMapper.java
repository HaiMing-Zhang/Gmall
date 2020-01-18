package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.SkuSaleAttrValue;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface SkuSaleAttrValueMapper extends Mapper<SkuSaleAttrValue> {
    //选择不同销售属性值进行跳转
    List<SkuSaleAttrValue> getSkuSaleAttrValueListBySpu(String spuId);

}
