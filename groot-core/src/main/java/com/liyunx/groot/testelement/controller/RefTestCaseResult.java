package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.testelement.TestResult;

import java.util.HashMap;
import java.util.Map;

public class RefTestCaseResult extends TestResult<RefTestCaseResult> {

    /**
     * 执行引用用例后的变量结果（仅记录当前层级声明的变量）
     */
    private Map<String, Object> variables = new HashMap<>();

    public Map<String, Object> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Object> variables) {
        this.variables = variables;
    }

}
