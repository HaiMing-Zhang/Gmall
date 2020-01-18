package com.atguigu.gmall.list.service.impl;

import com.alibaba.dubbo.config.annotation.Service;
import com.atguigu.gmall.bean.SkuInfo;
import com.atguigu.gmall.bean.SkuLsInfo;
import com.atguigu.gmall.bean.SkuLsParams;
import com.atguigu.gmall.bean.SkuLsResult;
import com.atguigu.gmall.config.RedisUtil;
import com.atguigu.gmall.service.ListService;
import io.searchbox.client.JestClient;
import io.searchbox.core.Index;
import io.searchbox.core.Search;
import io.searchbox.core.SearchResult;
import io.searchbox.core.Update;
import io.searchbox.core.search.aggregation.MetricAggregation;
import io.searchbox.core.search.aggregation.TermsAggregation;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.search.MultiMatchQuery;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.TermsBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import redis.clients.jedis.Jedis;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ListServiceImpl implements ListService {
    @Autowired
    private JestClient jestClient;
    @Autowired
    private RedisUtil redisUtil;

    private static final String ES_INDEX = "gmall";
    private static final String ES_TYPE = "SkuInfo";

    /**
     * 商品上架,向es中存储数据
     * @param skuLsInfo
     */
    @Override
    public void saveSkuInfo(SkuLsInfo skuLsInfo) {
        //编写es的保存动作
        Index index = new Index.Builder(skuLsInfo).index(ES_INDEX).type(ES_TYPE)
                .id(skuLsInfo.getId()).build();
        //执行动作
        try {
            jestClient.execute(index);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    /**
     * 全文检索es并返回结果
     * @param skuLsParams
     * @return
     */
    @Override
    public SkuLsResult search(SkuLsParams skuLsParams) {
        //动态编写dsl语句
        String query = makeQueryStringForSearch(skuLsParams);
        //编写es的查询动作
        Search search = new Search.Builder(query).addIndex(ES_INDEX).addType(ES_TYPE).build();
        //执行动作
        SearchResult searchResult = null;
        try {
            searchResult  = jestClient.execute(search);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //转换结果集
        SkuLsResult skuLsResult = makeResultForSearch(searchResult,skuLsParams);
        return skuLsResult;
    }

    /**
     * 将结果集转换为自定义的bean对象的结果集
     * @param searchResult
     * @param skuLsParams
     * @return
     */
    private SkuLsResult makeResultForSearch(SearchResult searchResult, SkuLsParams skuLsParams) {
        SkuLsResult skuLsResult = new SkuLsResult();
        List<SkuLsInfo> skuLsInfoList = new ArrayList<>();
        //去除集合hits
        List<SearchResult.Hit<SkuLsInfo, Void>> hits = searchResult.getHits(SkuLsInfo.class);
        for (SearchResult.Hit<SkuLsInfo, Void> hit : hits) {
            SkuLsInfo skuLsInfo = hit.source;
            //判断高亮是否为空
            if(hit.highlight != null && hit.highlight.size()>0){
                //取出高亮的skuName
                List<String> skuName = hit.highlight.get("skuName");
                //将skuLsInfo中的skuName替换为带有高亮的skuName
                skuLsInfo.setSkuName(skuName.get(0));
            }
            //将从es中取出的结果放入集合中
            skuLsInfoList.add(skuLsInfo);
        }
        //将总条数放入定义好的结果集bean中
        skuLsResult.setTotal(searchResult.getTotal());
        //将source信息集合放入结果集中
        skuLsResult.setSkuLsInfoList(skuLsInfoList);
        //取记录个数并计算出总页数
        long totalPage= (searchResult.getTotal() + skuLsParams.getPageSize() -1) / skuLsParams.getPageSize();
        skuLsResult.setTotalPages(totalPage);
        //定义一个集合,装从聚合中去除的平台属性值id
        List<String> attrValueIdList = new ArrayList<>();
        //去除聚合中的平台属性值id
        MetricAggregation aggregations = searchResult.getAggregations();
        TermsAggregation groupby_attr = aggregations.getTermsAggregation("groupby_attr");
        List<TermsAggregation.Entry> buckets = groupby_attr.getBuckets();
        //循环遍历,取出平台属性值id
        for (TermsAggregation.Entry bucket : buckets) {
            attrValueIdList.add(bucket.getKey());
        }
        //将平台属性值id集合放入结果集中
        skuLsResult.setAttrValueIdList(attrValueIdList);

        return skuLsResult;
    }

    /**
     * 动态DSL语句
     * @param skuLsParams
     * @return
     */
    private String makeQueryStringForSearch(SkuLsParams skuLsParams) {
        //相当于dsl查询语句中最大的大括号{}
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        //创建query下的其他命令
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        //设置平台属性值,进行dsl中的过滤操作
        if(skuLsParams.getValueId()!=null && skuLsParams.getValueId().size()>0){
            for (String valueId : skuLsParams.getValueId()) {
                //创建term
                TermQueryBuilder termQueryBuilder = new TermQueryBuilder("skuAttrValueList.valueId",valueId);
                //过滤
                boolQueryBuilder.filter(termQueryBuilder);
            }
        }
        //判断搜索框检索关键字是否为空
        if(skuLsParams.getKeyword()!=null && skuLsParams.getKeyword().length()>0){
            //设置查询的关键字
            MatchQueryBuilder matchQueryBuilder = new MatchQueryBuilder("skuName",skuLsParams.getKeyword());
            boolQueryBuilder.must(matchQueryBuilder);
            //高亮
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            //设置高亮前缀
            highlightBuilder.preTags("<span style=color:red>");
            //设置高亮后缀
            highlightBuilder.postTags("</span>");
            //设置高亮的字段
            highlightBuilder.field("skuName");
            //进行高亮
            searchSourceBuilder.highlight(highlightBuilder);
        }
        // 设置三级分类
        if (skuLsParams.getCatalog3Id()!=null && skuLsParams.getCatalog3Id().length()>0){
            TermQueryBuilder termQueryBuilder = new TermQueryBuilder("catalog3Id", skuLsParams.getCatalog3Id());
            boolQueryBuilder.filter(termQueryBuilder);
        }
        //dsl中的query
        searchSourceBuilder.query(boolQueryBuilder);
        //设置排序,根据hotScore进行降序
        searchSourceBuilder.sort("hotScore", SortOrder.DESC);
        //设置分页
        //计算第几条数据的公式
        int form = (skuLsParams.getPageNo()-1)*skuLsParams.getPageSize();
        //第几条数据
        searchSourceBuilder.from(form);
        //每页有几条
        searchSourceBuilder.size(skuLsParams.getPageSize());
        //设置聚合
        TermsBuilder groupby_attr = AggregationBuilders.terms("groupby_attr").field("skuAttrValueList.valueId");
        searchSourceBuilder.aggregation(groupby_attr);

        return searchSourceBuilder.toString();
    }

    /**
     * 修改商品的热度,热度越高,排名越靠前
     * 思路: redis中有一个数据类型为Zset,zset中有一个命令为zincrby,可以自动累加访问次数,
     *       如果达到指定值,则修改es中的hostScore值
     */
    @Override
    public void UpdateHotScore(String skuId) {
        //获取连接redis的工具,jedis
        Jedis jedis = redisUtil.getJedis();
        Double hotScore = jedis.zincrby("hotScore", 1, "skuId:" + skuId);

        if(hotScore % 10 == 0){
            //执行的DSL语句
            String updateDsl = "{\n" +
                    "  \"doc\": {\n" +
                    "    \"hotScore\":"+hotScore+"\n" +
                    "  }\n" +
                    "}";
            //修改热度字段,hotScore
            Update build = new Update.Builder(updateDsl).index(ES_INDEX).type(ES_TYPE).id(skuId).build();
            try {
                jestClient.execute(build);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

}
