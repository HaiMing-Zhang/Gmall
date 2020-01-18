package com.atguigu.gmall.bean;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

@Data
public class SkuLsParams implements Serializable {
     String id;

     String keyword;

     String catalog3Id;

     List<String> valueId;

     int pageNo=1;

     int pageSize=2;
}
