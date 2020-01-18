package com.atguigu.gmall.cart.controller;

import com.alibaba.dubbo.config.annotation.Reference;
import com.atguigu.gmall.bean.CartInfo;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.config.CheckPriceRequire;
import com.atguigu.gmall.config.CookieUtil;
import com.atguigu.gmall.config.LoginRequire;
import com.atguigu.gmall.service.CartService;
import com.atguigu.gmall.service.SkuService;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Controller
public class CartController {
    @Reference
    private CartService cartService;
    @Reference
    private SkuService skuService;

    /**
     * 将商品保存到数据库
     * @param request
     * @param response
     * @return
     */
    @RequestMapping("/addToCart")
    @LoginRequire(autoRedirect = false)
    public String addToCart(HttpServletRequest request, HttpServletResponse response){
        String skuId = request.getParameter("skuId");
        String skuNum = request.getParameter("num");
        //从作用域中获取到userId
        String userId = (String)request.getAttribute("userId");
        //如果userId为null则说明没有登录
        if(userId == null){
            //从cookie中获取临时userId
            userId = CookieUtil.getCookieValue(request,"userKey",false);
           //cookie中如果也不存在,则用uuid创建一个临时userId,然后存入cookie
            if(userId == null){
                //创建临时userId
                userId = UUID.randomUUID().toString().replace("-", "");
                //存入cookie
                CookieUtil.setCookie(request,response,"userKey",userId,7*24*3600,false);
            }
        }
        cartService.addToCart(skuId,userId,Integer.parseInt(skuNum));
        SkuInfo skuInfo = skuService.getSkuInfo(skuId);
        request.setAttribute("skuInfo",skuInfo);
        return "success";
    }

    /**
     * 获取数据库列表
     * @param request
     * @return
     */
    @RequestMapping("/cartList")
    @LoginRequire(autoRedirect = false)
    @CheckPriceRequire
    public String CartList(HttpServletRequest request){
        //从作用域中获取userId,如果不为空则说明已经登录
        String userId = (String)request.getAttribute("userId");
        List<CartInfo> cartInfoList = null;
        //如果已经登陆
        if(userId != null){
            //根据登陆的用户的id查询购物车商品
            cartInfoList = cartService.getCartList(userId);
            String userKey = CookieUtil.getCookieValue(request, "userKey", false);
            if(!StringUtils.isEmpty(userKey)){
                List<CartInfo> cartList = cartService.getCartList(userKey);
                //将未登录的购物车商品合并到已登录中
                if(cartList != null && cartList.size()>0){
                    //合并
                    cartInfoList = cartService.mergeToCartList(cartList,userId);
                    //删除未登录的购物车数据
                    cartService.deleteCartList(userKey);
                }
            }
        }else {
            //如果没有登录,则用cookie中临时的userId进行查询商品
            userId = CookieUtil.getCookieValue(request, "userKey", false);
            cartInfoList = cartService.getCartList(userId);

        }
        //将商品列表存入作用域
        request.setAttribute("cartInfoList",cartInfoList);
        return "cartList";
    }

    /**
     * 修改选中状态
     * @param request
     */
    //http://cart.gmall.com/checkCart
    @RequestMapping("/checkCart")
    @CheckPriceRequire
    @LoginRequire(autoRedirect = false)
    public void checkCart(HttpServletRequest request){
        String isChecked = request.getParameter("isChecked");
        String skuId = request.getParameter("skuId");

        String userId = (String)request.getAttribute("userId");

        if(userId == null){
            userId=CookieUtil.getCookieValue(request,"userKey",false);
        }
        cartService.checkCart(isChecked,skuId,userId);
    }

    /**
     * 合并购物车,将已选中状态的登陆后也变为已选中
     * @return
     */
    @RequestMapping("/toTrade")
    @LoginRequire
   public String toTrade(HttpServletRequest request){
        String userId = (String)request.getAttribute("userId");
        String userKey = CookieUtil.getCookieValue(request, "userKey", false);
        if(!StringUtils.isEmpty(userKey)){
            List<CartInfo> cartList = cartService.getCartList(userKey);
            //将未登录的购物车商品合并到已登录中
            if(cartList != null && cartList.size()>0){
                //合并
                List<CartInfo> cartInfoList = cartService.mergeToCartList(cartList,userId);
                //删除未登录的购物车数据
                cartService.deleteCartList(userKey);
            }
        }

        return "redirect://trade.gmall.com/trade";
   }

   @RequestMapping("checkPrice")
   @ResponseBody
   public String checkPrice(HttpServletRequest request){

       String userId = (String)request.getAttribute("userId");

       if(userId == null){
           userId=CookieUtil.getCookieValue(request,"userKey",false);
       }

       Double price = cartService.checkPrice(userId);
       request.setAttribute("checkPrice",price.doubleValue());
       return "success";
   }
}
