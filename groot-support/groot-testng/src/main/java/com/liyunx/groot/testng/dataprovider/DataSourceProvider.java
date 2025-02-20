package com.liyunx.groot.testng.dataprovider;

import com.liyunx.groot.dataloader.file.ParametersDataFileLoader;
import com.liyunx.groot.testelement.ParametersData;
import com.liyunx.groot.testng.annotation.DataSource;
import org.testng.ITestNGMethod;
import org.testng.annotations.DataProvider;

import java.util.List;
import java.util.Map;

import static com.liyunx.groot.testng.support.AnnotationSupport.getDataSource;

public class DataSourceProvider {

    public static final String DATA_SOURCE = "data_source";
    public static final String DATA_SOURCE_PARALLEL = "data_source_parallel";

    private static final Object[][] EMPTY_DATA = new Object[][]{};

    @DataProvider(name = DATA_SOURCE_PARALLEL, parallel = true)
    public Object[][] dataSourceParallel(ITestNGMethod method) {
        return dataSource(method);
    }

    @DataProvider(name = DATA_SOURCE)
    public Object[][] dataSource(ITestNGMethod method) {
        DataSource dataSource = getDataSource(method.getConstructorOrMethod().getMethod());
        if (dataSource == null) {
            return EMPTY_DATA;
        }

        String filePath = dataSource.value();
        if (filePath.trim().isEmpty()) {
            return EMPTY_DATA;
        }

        // 参数列表（支持）：Map
        // 参数列表（暂不支持）：多个参数 / 多个参数（包含非基本类型），比如 Dubbo 请求对象
        ParametersDataFileLoader loader = new ParametersDataFileLoader();
        // 如果文件是相对路径，默认根目录为 src/test/resources
        ParametersData parametersData = loader.loadByID(filePath, ParametersData.class);
        List<Map<String, Object>> dataList;
        if (parametersData == null || (dataList = parametersData.getData()).isEmpty()) {
            return EMPTY_DATA;
        }
        Object[][] data = new Object[parametersData.length()][1];
        for (int i = 0; i < parametersData.length(); i++) {
            data[i][0] = dataList.get(i);
        }
        return data;
    }

}
