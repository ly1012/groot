package com.liyunx.groot.config;

import com.liyunx.groot.AbstractGrootTestNGTestCase;
import com.liyunx.groot.Configuration;
import com.liyunx.groot.DefaultVirtualRunner;
import com.liyunx.groot.Groot;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import org.testng.annotations.Test;

import static com.liyunx.groot.SessionRunner.getSession;

public class VariableConfigItemTest extends AbstractGrootTestNGTestCase {

    private static final Groot groot;

    static {
        Configuration configuration = Configuration.generateDefaultConfiguration();
        configuration.setGlobalConfigLoader(() -> {
            GlobalConfig globalConfig = new GlobalConfig();
            globalConfig.put(VariableConfigItem.KEY, new VariableConfigItem.Builder()
                .var("k1", "k1_gValue")
                .var("k2", "k2_gValue")
                .build());
            return globalConfig;
        });
        configuration.setEnvironmentLoader(environmentName -> {
            EnvironmentConfig environmentConfig = new EnvironmentConfig();
            environmentConfig.put(VariableConfigItem.KEY, new VariableConfigItem.Builder()
                .var("k2", "k2_eValue")
                .var("k3", "k3_eValue")
                .build());
            return environmentConfig;
        });
        groot = new Groot(configuration, "test");
    }

    @Test(description = "变量访问")
    public void testNameAndTypeByYaml() {
        DefaultVirtualRunner.tv("k3", "k3_tValue");
        DefaultVirtualRunner.tv("k4", "k4_tValue");
        DefaultVirtualRunner.sv("k4", "k4_sValue");
        DefaultVirtualRunner.sv("k5", "k5_sValue");
        getSession().run("testcases/config/variables_name_type.yml");
    }

    @Test(description = "变量更新与删除")
    public void testUpdateAndRemoveByYaml() {
        getSession().run("testcases/config/variables_update_delete.yml");
    }

    @Override
    protected Groot getGroot() {
        return groot;
    }
}
