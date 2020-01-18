package com.atguigu.gmall.list.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.bean.*;
import com.atguigu.gmall.service.ListService;
import com.atguigu.gmall.service.ManageService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Controller
public class ListController {
    @Reference
    private ListService listService;
    @Reference
    private ManageService manageService;

    @RequestMapping("/list.html")
    public String getList(SkuLsParams skuLsParams, HttpServletRequest request){
        SkuLsResult search = listService.search(skuLsParams);
        //从结果中获取商品数据
        List<SkuLsInfo> skuLsInfoList = search.getSkuLsInfoList();
        //从结果中取出平台属性值列表
        List<String> attrValueIdList = search.getAttrValueIdList();
        //查询平台属性和属性值
        List<BaseAttrInfo> valueList = manageService.getAttrValueList(attrValueIdList);
        //拼接平台属性值筛选条件的url地址
        String urlParam = makeUrlParam(skuLsParams);
        /*
         *itco 在点击一个平台属性值进行筛选后,此平台属性值则在页面上消失,用迭代器进行迭代,之后在判断
         * 参数中传过来的平台属性值id是否与查询出来的平台属性值id一样,如果一样,则在查询出来的平台属性值id中
         * 移除此平台属性并将选择好的valueName放到筛选框
        *
         * */
        List<BaseAttrValue> baseAttrValueList = new ArrayList<>();

        for (Iterator<BaseAttrInfo> iterator = valueList.iterator(); iterator.hasNext(); ) {
            BaseAttrInfo baseAttrInfo =  iterator.next();
            //获取查询出来的平台属性id集合
            List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
            for (BaseAttrValue baseAttrValue : attrValueList) {
                //获取参数中的平台属性值id
                List<String> valueIdList = skuLsParams.getValueId();
                if(valueIdList!=null && valueIdList.size()>0){
                    for (String valueId : valueIdList) {
                        //进行判断,如果一样则移除
                        if(valueId.equals(baseAttrValue.getId())){
                            iterator.remove();
                            BaseAttrValue attrValue = new BaseAttrValue();
                            //将面包屑取下时需要传回一个url地址,setUrlParam
                            String makeUrlParam  = makeUrlParam(skuLsParams, valueId);
                            attrValue.setUrlParam(makeUrlParam);
                            //将选中的筛选条件放入BaseAttrValue对象,传到前台,放入筛选框
                            attrValue.setValueName(baseAttrInfo.getAttrName()+":"+baseAttrValue.getValueName());
                            baseAttrValueList.add(attrValue);
                        }
                    }
                }
            }
        }

        //将商品属性数据存入作用域中
        request.setAttribute("skuLsInfoList",skuLsInfoList);

        request.setAttribute("valueList",valueList);
        request.setAttribute("urlParam",urlParam);
        request.setAttribute("keyWord",skuLsParams.getKeyword());
        request.setAttribute("baseAttrValueList",baseAttrValueList);
        //存放总页数
        request.setAttribute("totalPages",search.getTotalPages());
        //存放当前页
        request.setAttribute("pageNo",skuLsParams.getPageNo());

        return "list";
    }

    /**
     * 拼接平台属性值筛选条件的url地址
     * @param skuLsParams
     * @return
     */
    private String makeUrlParam(SkuLsParams skuLsParams,String...excludeValueIds) {
        String urlParam = "";
        //判断是否是全文检索
        if(skuLsParams.getKeyword() != null && skuLsParams.getKeyword().length()>0){
            urlParam+="keyWord="+skuLsParams.getKeyword();
        }
        if(skuLsParams.getCatalog3Id()!= null && skuLsParams.getCatalog3Id().length()>0){
            urlParam+="catalog3Id="+skuLsParams.getCatalog3Id();
        }
        if(skuLsParams.getValueId()!=null&&skuLsParams.getValueId().size()>0){
            for (String valueId : skuLsParams.getValueId()) {
                //如果skuLsParams中的valueId与excludeValueIds中的值一样则推出档次拼串
                if(excludeValueIds!=null && excludeValueIds.length>0){
                    String excludeValueId = excludeValueIds[0];
                    if(valueId.equals(excludeValueId)){
                        continue;
                    }
                }
                if(urlParam.length()>0){
                    urlParam+="&";
                }

                urlParam+="valueId="+valueId;
            }
        }
        return urlParam;
    }
}
