package com.liyunx.groot.template.freemarker


import org.testng.annotations.Test

import static org.assertj.core.api.Assertions.assertThat

class FreeMarkerTemplateEngineTest {

    @Test
    void testIgnore() {
        String originalTemplate = '{"test": "${json-unit.matches:isDivisibleBy}3", "groot": "${grootValue}"}'

        // 预处理：自动包裹 json-unit 表达式，这些表达式无需计算
        // https://github.com/lukas-krecan/JsonUnit
        String processedTemplate = originalTemplate.replaceAll(
            '\\$\\{json-unit\\.[^}]+}',
            '<#noparse>$0</#noparse>'
        )
        assertThat(processedTemplate)
            .isEqualTo('{"test": "<#noparse>${json-unit.matches:isDivisibleBy}</#noparse>3", "groot": "${grootValue}"}')

        // 配置 FreeMarker
        FreeMarkerTemplateEngine engine = new FreeMarkerTemplateEngine()
        // 数据模型
        Map<String, Object> data = new HashMap<>()
        data.put("grootValue", "I am Groot")
        // 渲染模板
        assertThat(engine.eval(data, processedTemplate))
            .isEqualTo('{"test": "${json-unit.matches:isDivisibleBy}3", "groot": "I am Groot"}')
    }

}