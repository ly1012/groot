package com.liyunx.groot.protocol.http.model;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONReader;
import com.alibaba.fastjson2.reader.ObjectReader;
import com.liyunx.groot.common.Computable;
import com.liyunx.groot.common.Copyable;
import com.liyunx.groot.common.Mergeable;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.protocol.http.support.HttpModelSupport;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 查询参数集合，支持多值表示，如 <code>?name=tom&name=mary</code>
 */
public class QueryParamManager
    extends ArrayList<QueryParam>
    implements Copyable<QueryParamManager>, Mergeable<QueryParamManager>, Computable<QueryParamManager> {

    public static class QueryParamManagerObjectReader implements ObjectReader<QueryParamManager> {

        @SuppressWarnings("rawtypes")
        @Override
        public QueryParamManager readObject(JSONReader jsonReader, Type fieldType, Object fieldName, long features) {
            List partsList = jsonReader.readArray();
            QueryParamManager queryParams = new QueryParamManager();
            for (Object partMap : partsList) {
                if (!(partMap instanceof Map)) {
                    throw new InvalidDataException("http.params 数据结构非法，当前值：%s", JSON.toJSONString(partMap));
                }
                queryParams.add(JSON.parseObject(JSON.toJSONString(partMap), QueryParam.class));
            }
            return queryParams;
        }
    }

    public static QueryParamManager of(Map<String, String> map) {
        QueryParamManager queryParamManager = new QueryParamManager();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String k = entry.getKey();
            String v = entry.getValue();
            queryParamManager.add(new QueryParam(k, v));
        }
        return queryParamManager;
    }

    @Override
    public QueryParamManager copy() {
        QueryParamManager res = new QueryParamManager();
        forEach(p -> res.add(p.copy()));
        return res;
    }

    @Override
    public QueryParamManager merge(QueryParamManager other) {
        return HttpModelSupport.multiValueManagerMerge(this, other, e -> e.name);
    }

    @Override
    public QueryParamManager eval(ContextWrapper ctx) {
        forEach(ctx::eval);
        return this;
    }

}
