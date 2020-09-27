package com.atguigu.gmall.search.service;

import com.atguigu.gmall.search.pojo.SearchParamVo;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class SearchService {

    @Autowired
    private RestHighLevelClient restHighLevelClient;

    public void search(SearchParamVo paramVo) {

        try {
            SearchRequest searchRequest = new SearchRequest(new String[]{"goods"}, this.buildDsl(paramVo));
            SearchResponse searchResponse = this.restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
            System.out.println(searchResponse);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private SearchSourceBuilder buildDsl(SearchParamVo paramVo){

        SearchSourceBuilder sourceBuilder = new SearchSourceBuilder();

        String keyword = paramVo.getKeyword();
        if (StringUtils.isBlank(keyword)){
            // TODO：打广告
            return sourceBuilder;
        }

        // 1. 构建搜索条件
        BoolQueryBuilder boolQueryBuilder = QueryBuilders.boolQuery();
        sourceBuilder.query(boolQueryBuilder);

        // 1.1. 构建匹配查询
        boolQueryBuilder.must(QueryBuilders.matchQuery("title", keyword).operator(Operator.AND));

        // 1.2. 构建过滤条件
        // 1.2.1. 品牌过滤

        // 1.2.2. 分类过滤

        // 1.2.3. 价格区间过滤

        // 1.2.4. 库存过滤

        // 1.2.5. 规格参数的嵌套过滤

        // 2. 构建排序条件

        // 3. 构建分页条件

        // 4. 构建高亮

        // 5. 构建聚合
        // 5.1. 品牌聚合

        // 5.2. 分类聚合

        // 5.3. 规格参数的嵌套聚合

        System.out.println(sourceBuilder);
        return sourceBuilder;
    }
}
