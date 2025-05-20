package com.liyunx.groot.testelement.controller;

import com.liyunx.groot.GrootTestNGTestCase;
import com.liyunx.groot.support.Ref;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static com.liyunx.groot.DefaultVirtualRunner.*;
import static com.liyunx.groot.SessionRunner.getSession;
import static com.liyunx.groot.support.Ref.ref;
import static org.assertj.core.api.Assertions.assertThat;

public class ForEachControllerTest extends GrootTestNGTestCase {

    @Test
    public void testForEachUsingFile() {
        Ref<Integer> count = ref(0);
        foreach("使用 3 个不同权限的账号操作", "testdata/testelement/foreach/user.csv", () -> {
            count.value++;
            if (count.value == 3) {
                String username = lv("username");
                String password = lv("password");
                assertThat(username).isEqualTo("groot");
                assertThat(password).isEqualTo("grootPassword");
            }
        });
        assertThat(count.value).isEqualTo(3);
    }

    @Test
    public void testForEachUsingFileByYaml() {
        getSession().run("testcases/controller/foreach/foreach_file.yml");
    }

    @Test
    public void testForEachUsingData() {
        sv("grootPwd", "grootPassword");

        List<Map<String, Object>> data = List.of(
            Map.of("username", "admin", "password", "admin123"),
            Map.of("username", "guest", "password", "guest123"),
            Map.of("username", "groot", "password", "${grootPwd}")
        );
        Ref<Integer> count = ref(0);
        foreach("使用 3 个不同权限的账号操作", data, () -> {
            count.value++;
            if (count.value == 3) {
                String username = lv("username");
                String password = lv("password");
                assertThat(username).isEqualTo("groot");
                assertThat(password).isEqualTo("grootPassword");
            }
        });
        assertThat(count.value).isEqualTo(3);
    }

    @Test
    public void testForEachUsingDataWithExpression() {
        List<Map<String, Object>> data = List.of(
            Map.of("name", "cat", "comment", "HelloKitty"),
            Map.of("name", "dog", "comment", "Snoopy")
        );
        sv("data", data);
        foreach("使用表达式返回的数据", settings -> settings.expression("${data}"), () -> {

            onIf("如果是猫", "${name == 'cat'}", () -> {
                assertThat((String)v("comment")).isEqualTo("HelloKitty");
            });

        });
    }

    @Test
    public void testForEachUsingDataWithTableByYaml() {
        getSession().run("testcases/controller/foreach/foreach_table.yml");
    }

    @Test
    public void testForEachUsingDataWithRowByYaml() {
        getSession().run("testcases/controller/foreach/foreach_row.yml");
    }

    @Test
    public void testForEachUsingDataWithColumnByYaml() {
        getSession().run("testcases/controller/foreach/foreach_column.yml");
    }

    @Test
    public void testForEachUsingDataWithExpressionByYaml() {
        getSession().run("testcases/controller/foreach/foreach_expression.yml");
    }

    @Test
    public void testForEachWithFilterByYaml() {
        getSession().run("testcases/controller/foreach/foreach_filter.yml");
    }

    @Test
    public void testForEachUsingBuilderWithCondition() {
        Ref<Integer> count = ref(0);
        foreach("使用 groot 账号操作", it -> it
            .file("testdata/testelement/foreach/user.csv")
            .filter(filter -> filter.condition("${username == 'groot'}")), () ->
        {
            count.value++;
            String username = lv("username");
            String password = lv("password");
            assertThat(username).isEqualTo("groot");
            assertThat(password).isEqualTo("grootPassword");
        });
        assertThat(count.value).isEqualTo(1);
    }

    @Test
    public void testForEachUsingBuilderWithSlice() {
        Ref<Integer> count = ref(0);
        foreach("使用 groot 账号操作", it -> it
            .file("testdata/testelement/foreach/user.csv")
            .filter(filter -> filter.slice("[-1]")), () ->
        {
            count.value++;
            String username = lv("username");
            String password = lv("password");
            assertThat(username).isEqualTo("groot");
            assertThat(password).isEqualTo("grootPassword");
        });
        assertThat(count.value).isEqualTo(1);
    }

    @Test
    public void testForEachUsingBuilderWithNames() {
        Ref<Integer> count = ref(0);
        foreach("使用 groot 账号操作", it -> it
            .file("testdata/testelement/foreach/user.csv")
            .filter(filter -> filter.slice("[-1]").names("password")), () ->
        {
            count.value++;
            String username = lv("username");
            String password = lv("password");
            assertThat(username).isNull();
            assertThat(password).isEqualTo("grootPassword");
        });
        assertThat(count.value).isEqualTo(1);
    }

    @Test
    public void testForEachUsingBuilderWithFilter() {
        Ref<Integer> count = ref(0);
        foreach("ForEachController 过滤器示例", it -> it
            .file("testcases/controller/foreach/data_filter.csv?ignoreSurroundingSpaces=true")
            .filter(filter -> filter
                .slice("[2..-1]")
                .condition("${role == 'guest' && username == 'tom'}")
                .names("role", "username", "password")), () ->
        {
            noopWith("变量值断言", action -> action
                .validate(validate -> validate
                    .equalTo("${password}", "guest999")
                    .equalTo("${comment}", null)));
            count.value++;
        });
        assertThat(count.value).isEqualTo(1);
    }

}
