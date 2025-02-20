package com.liyunx.groot.context.variables;


import com.liyunx.groot.context.Context;

import java.util.List;

/**
 * 环境变量包装类
 */
public class EnvironmentVariablesWrapper extends AbstractVariablesWrapper {

  public EnvironmentVariablesWrapper(List<Context> contextChain) {
    super(contextChain);
  }

}
