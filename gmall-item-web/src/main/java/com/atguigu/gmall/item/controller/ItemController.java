package com.atguigu.gmall.item.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuSaleAttrValue;
import com.atguigu.gmall.bean.SpuSaleAttr;
import com.atguigu.gmall.bean.SpuSaleAttrValue;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import com.atguigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ItemController {
    @Reference
    private SkuService skuService;
    @Reference
    private ListService listService;
    @RequestMapping("{skuId}.html")
    public String item(@PathVariable String skuId, HttpServletRequest request){
        //查询sku基本信息与sku图片
       SkuInfo skuInfo = skuService.getSkuInfo(skuId);
       request.setAttribute("skuInfo",skuInfo);
       //如果访问此skuId的商品,则增加热度
       // listService.UpdateHotScore(skuId);
       //加载页面时显示销售属性与销售属性值
       List<SpuSaleAttr> saleAttrList = skuService.getSpuSaleAttrListCheckBySku(skuInfo);
       request.setAttribute("saleAttrList",saleAttrList);
       //选择不同销售属性值进行跳转,根据spuid查询销售属性值
       List<SkuSaleAttrValue> skuSaleAttrValueList = skuService.getSkuSaleAttrValueListBySpu(skuInfo);

        String valueIdsKey="";
        Map<String,String> valuesSkuMap=new HashMap<>();
        //判断查询除来的销售属性值是否为空
       if(skuSaleAttrValueList !=null && skuSaleAttrValueList.size()>0){
           for (int i = 0; i < skuSaleAttrValueList.size(); i++) {
               SkuSaleAttrValue skuSaleAttrValue = skuSaleAttrValueList.get(i);
               //如果valueIdsKey的长度不为零则加分隔符
               if(valueIdsKey.length()!= 0){
                   valueIdsKey+="|" ;
               }
                valueIdsKey+=skuSaleAttrValue.getSaleAttrValueId();
               //如果下一个是最后一个值或者下一个销售属性值和当前销售属性值不同,则放入map中,后续转成json字符串
               if((i+1)==skuSaleAttrValueList.size() || !skuSaleAttrValue.getSkuId().equals(skuSaleAttrValueList.get(i+1).getSkuId())){
                   valuesSkuMap.put(valueIdsKey,skuSaleAttrValue.getSkuId());
                   valueIdsKey="";
               }
           }
           String skuJsonValue = JSON.toJSONString(valuesSkuMap);
           System.out.println(skuJsonValue);
           request.setAttribute("skuJsonValue",skuJsonValue);
       }
       return "item";
    }
}
