package com.atguigu.gmall.manage.mapper;

import com.atguigu.gmall.bean.BaseAttrInfo;
import org.apache.ibatis.annotations.Param;
import tk.mybatis.mapper.common.Mapper;

import java.util.List;

public interface BaseAttrInfoMapper extends Mapper<BaseAttrInfo> {
    List<BaseAttrInfo> getAttrInfoListByCatalog3Id(String catalog3Id);
    //根据多个attrValueId查询多个平台属性
    List<BaseAttrInfo> selectAttrInfoListByIds(@Param("valueIds") String valueIdString);
}
