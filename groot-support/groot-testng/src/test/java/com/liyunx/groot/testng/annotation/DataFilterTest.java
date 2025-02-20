package com.liyunx.groot.testng.annotation;

import com.liyunx.groot.testng.annotation.DataFilter;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DataFilterTest {

    /* ------------------------------------------------------------ */
    // slice 测试

    @DataProvider(name = "sliceDataProvider")
    public Object[][] sliceDataProvider() {
        return new Object[][]{
            {"mary", 111111},
            {"admin", 222222},
            {"tomcat", 333333},
            {"jack", 222222},
            {"admin", 111111}
        };
    }

    private int testSlice1InvokeCount = 0;
    private int testSlice2InvokeCount = 0;
    private int testSlice3InvokeCount = 0;
    private int testSlice4InvokeCount = 0;
    private int testSlice5InvokeCount = 0;
    private int testSlice6InvokeCount = 0;

    @DataFilter(slice = "[1..-1]")
    @Test(dataProvider = "sliceDataProvider", description = "test slice [1..-1]")
    public void testSlice1(String username, int password) {
        testSlice1InvokeCount++;
    }

    @DataFilter(slice = "[2..4]")
    @Test(dataProvider = "sliceDataProvider", description = "test slice [2..4]")
    public void testSlice2(String username, int password) {
        testSlice2InvokeCount++;
    }

    @DataFilter(slice = "[4..]")
    @Test(dataProvider = "sliceDataProvider", description = "test slice [4..]")
    public void testSlice3(String username, int password) {
        testSlice3InvokeCount++;
    }

    @DataFilter(slice = "[..1]")
    @Test(dataProvider = "sliceDataProvider", description = "test slice [..1]")
    public void testSlice4(String username, int password) {
        testSlice4InvokeCount++;
    }

    @DataFilter(slice = "[1, 4, 5]")
    @Test(dataProvider = "sliceDataProvider", description = "test slice [1, 4, 5]")
    public void testSlice5(String username, int password) {
        testSlice5InvokeCount++;
    }

    @DataFilter(slice = "[-1, 3, -1]")
    @Test(dataProvider = "sliceDataProvider", description = "test slice [-1, 3, -1]")
    public void testSlice6(String username, int password) {
        testSlice6InvokeCount++;
    }

    @Test(dependsOnMethods = {"testSlice1", "testSlice2", "testSlice3", "testSlice4", "testSlice5", "testSlice6"})
    public void testAssertTestSliceInvokeCount() {
        assertThat(testSlice1InvokeCount).isEqualTo(5);
        assertThat(testSlice2InvokeCount).isEqualTo(3);
        assertThat(testSlice3InvokeCount).isEqualTo(2);
        assertThat(testSlice4InvokeCount).isEqualTo(1);
        assertThat(testSlice5InvokeCount).isEqualTo(3);
        assertThat(testSlice6InvokeCount).isEqualTo(2);
    }

    /* ------------------------------------------------------------ */
    // expr 测试（仅有一个参数且参数类型为 Map）

    @DataProvider(name = "mapDataProvider")
    public Object[][] mapDataProvider() {
        return new Object[][]{
            { toMap("jackson", "100") },
            { toMap("jack", "222") },
            { toMap("jackie", "555") },
            { toMap("jacklin", "222") },
            { toMap("michael", "301") }
        };
    }

    private Map<String, Object> toMap(String username, String password) {
        Map<String, Object> map = new HashMap<>();
        map.put("username", username);
        map.put("password", password);
        return map;
    }

    private int testExprWithMapInvokeCount = 0;

    @DataFilter(expr = "username.contains('jack') && password == '222'")
    @Test(dataProvider = "mapDataProvider", description = "expr 测试，参数类型为 Map")
    public void testExprWithMap(Map<String, Object> data) {
        String username = (String) data.get("username");
        String password = (String) data.get("password");
        assertThat(username).contains("jack");
        assertThat(password).isEqualTo("222");
        testExprWithMapInvokeCount++;
    }

    @Test(dependsOnMethods = {"testExprWithMap"})
    public void testTestExprWithMapInvokeCount() {
        assertThat(testExprWithMapInvokeCount).isEqualTo(2);
    }

    /* ------------------------------------------------------------ */
    // expr 测试（有多个参数）

    @DataProvider(name = "arrayDataProvider")
    public Object[][] arrayDataProvider() {
        return new Object[][]{
            { "jackson", "100" },
            { "jack", "222" },
            { "jackie", "555" },
            { "jacklin", "222" },
            { "michael", "301" }
        };
    }

    private int testExprWithOtherInvokeCount = 0;

    @DataFilter(expr = "p1.contains('jack') && p2 == '222'")
    @Test(dataProvider = "arrayDataProvider", description = "expr 测试，参数类型为 Map")
    public void testExprWithOther(String username, String password) {
        assertThat(username).contains("jack");
        assertThat(password).isEqualTo("222");
        testExprWithOtherInvokeCount++;
    }

    @Test(dependsOnMethods = {"testExprWithOther"})
    public void testTestExprWithOtherInvokeCount() {
        assertThat(testExprWithOtherInvokeCount).isEqualTo(2);
    }


}
