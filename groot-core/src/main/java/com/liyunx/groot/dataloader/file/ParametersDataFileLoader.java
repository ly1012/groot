package com.liyunx.groot.dataloader.file;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.TypeReference;
import com.liyunx.groot.dataloader.DataLoadException;
import com.liyunx.groot.exception.InvalidDataException;
import com.liyunx.groot.testelement.ParametersData;
import com.liyunx.groot.util.FileUtil;
import com.liyunx.groot.util.YamlUtil;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.poi.ss.usermodel.*;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 参数化数据：本地文件加载
 *
 * <ul>
 *   <li>加载速度：字面量 > json > yml/yaml > csv > xls/xlsx</li>
 *   <li>加载时间（2 行数据，单次测试）：0ms > 2ms > 5ms > 16ms > 800ms</li>
 *   <li>推荐形式：字面量 > json/yml/yaml/csv > xls/xlsx</li>
 * </ul>
 * <p>
 * 不推荐 Excel 文件的理由：
 * <ul>
 *   <li>二进制文件，不方便 diff</li>
 *   <li>文件大，空文件都有 6KB+，而文本文件大小仅取决于文本内容多少</li>
 *   <li>必须使用 Excel 打开，打开速度慢，特别是打开多个 Excel 文件时更麻烦</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
public class ParametersDataFileLoader extends LocalDataLoader {

    @Override
    protected <T> T next(String text, String textType, Class<T> clazz) {
        if (ParametersData.class.equals(clazz)) {
            if (FileType.isJSONFile(textType))
                return (T) getParametersDataFromJSON(text);
            if (FileType.isYamlFile(textType))
                return (T) getParametersDataFromYaml(text);
        }
        return null;
    }

    @Override
    protected <T> T nextByID(String identifier, Class<T> clazz) {
        if (ParametersData.class.equals(clazz)) {
            int questionMark = identifier.lastIndexOf("?");
            Map<String, String> params = null;
            if (questionMark > 0) {
                String paramsString = identifier.substring(questionMark + 1);
                params = getParams(paramsString);
                identifier = identifier.substring(0, questionMark);
            }
            Path path = getAbsolutePath(identifier);
            String fileType = getFileType(path);

            // 文本文件（JSON/YML/YAML/XML）：读取文本内容，传递给 next 方法处理
            if (FileType.isTextFile(fileType))
                return next(FileUtil.readFile(path), getFileType(path), clazz);

            // 非文本文件（如 XLS）
            if (FileType.isCSVFile(fileType))
                return (T) getParametersDataFromCSV(path, params);
            if (FileType.isExcelFile(fileType))
                return (T) getParametersDataFromExcel(path, params);
        }
        return null;
    }

    private Map<String, String> getParams(String params) {
        Map<String, String> paramsMap = new HashMap<>();
        for (String param : params.split("&")) {
            String[] kv = param.split("=");
            paramsMap.put(kv[0], kv[1]);
        }
        return paramsMap;
    }

    private ParametersData getParametersDataFromJSON(String jsonText) {
        try {
            return new ParametersData(new TypeReference<Map<String, Object>>() {
            }.parseArray(jsonText));
        } catch (Exception e) {
            throw new InvalidDataException("JSON 参数化文件，JSON 必须是 Array -> Map 结构", e);
        }
    }

    private ParametersData getParametersDataFromYaml(String yamlText) {
        try {
            JSONArray jsonArray = YamlUtil.getYaml().loadAs(yamlText, JSONArray.class);
            return getParametersDataFromJSON(JSON.toJSONString(jsonArray));
        } catch (Exception e) {
            throw new InvalidDataException("Yaml 参数化文件，Yaml 必须是 Array -> Map 结构", e);
        }
    }

    private ParametersData getParametersDataFromCSV(Path path, Map<String, String> params) {
        try (FileReader fileReader = new FileReader(path.toFile())) {
            CSVFormat csvFormat = getCSVFormat(params);
            try (CSVParser csvParser = csvFormat.parse(fileReader)) {
                List<String> headerNames = csvParser.getHeaderNames();
                List<Map<String, Object>> data = new ArrayList<>();
                csvParser.forEach(record -> {
                    Map<String, Object> rowMap = new HashMap<>();
                    for (int i = 0, len = record.size(); i < len; i++) {
                        rowMap.put(headerNames.get(i), record.get(i));
                    }
                    data.add(rowMap);
                });
                return new ParametersData(data);
            }
        } catch (FileNotFoundException e) {
            throw new DataLoadException("文件 %s 不存在", path.toAbsolutePath().toString());
        } catch (IOException e) {
            throw new DataLoadException("文件 %s 读取失败", e, path.toAbsolutePath().toString());
        }
    }

    private CSVFormat getCSVFormat(Map<String, String> params) {
        if (params == null) {
            return CSVFormat.Predefined.Default.getFormat().builder().setHeader().build();
        }

        CSVFormat csvFormat = CSVFormatFactory.getCSVFormat(params.get("format"));
        CSVFormat.Builder builder = csvFormat.builder().setHeader();
        params.forEach((key, value) -> {
            if ("delimiter".equalsIgnoreCase(key)) {
                builder.setDelimiter(value);
            }
            if ("escape".equalsIgnoreCase(key) && value.length() == 1) {
                builder.setEscape(value.charAt(0));
            }
            if ("quote".equalsIgnoreCase(key) && value.length() == 1) {
                builder.setQuote(value.charAt(0));
            }
        });
        return builder.build();
    }

    static class CSVFormatFactory {

        private static final Map<String, CSVFormat> CSV_FORMAT_MAP = new HashMap<>();

        static {
            CSV_FORMAT_MAP.put("DEFAULT", CSVFormat.Predefined.Default.getFormat());
            CSV_FORMAT_MAP.put("EXCEL", CSVFormat.Predefined.Excel.getFormat());
            CSV_FORMAT_MAP.put("MYSQL", CSVFormat.Predefined.MySQL.getFormat());
            CSV_FORMAT_MAP.put("ORACLE", CSVFormat.Predefined.Oracle.getFormat());
            CSV_FORMAT_MAP.put("MONGODBCSV", CSVFormat.Predefined.MongoDBCsv.getFormat());
            CSV_FORMAT_MAP.put("MONGODBTSV", CSVFormat.Predefined.MongoDBTsv.getFormat());
            CSV_FORMAT_MAP.put("POSTGRESQLCSV", CSVFormat.Predefined.PostgreSQLCsv.getFormat());
            CSV_FORMAT_MAP.put("POSTGRESQLTEXT", CSVFormat.Predefined.PostgreSQLText.getFormat());
            CSV_FORMAT_MAP.put("INFORMIXUNLOAD", CSVFormat.Predefined.InformixUnload.getFormat());
            CSV_FORMAT_MAP.put("INFORMIXUNLOADCSV", CSVFormat.Predefined.InformixUnloadCsv.getFormat());
            CSV_FORMAT_MAP.put("RFC4180", CSVFormat.Predefined.RFC4180.getFormat());
            CSV_FORMAT_MAP.put("TDF", CSVFormat.Predefined.TDF.getFormat());

            // CSV 语法约束（适合文本编辑器或 IDEA 编辑）
            // 1. 自动忽略两边的空格，如果需要保留，使用双引号包裹，如：" start tomcat "
            // 2. 使用 \ 转义特殊字符，如换行：\n \"
            // 3. 一行中书写可以不加双引号，如 cat\ndog，这里 \n 表示换行
            // 4. 多行写法需要加双引号，如：
            // name,comment
            // tom,"音乐家
            // 美术家
            // 歌手"
            // 5. 第一行为表头，不可省略
            //
            // 如果希望使用 Excel 软件编辑，推荐使用 xls 或 xlsx 文件，
            // csv 文件推荐文本编辑器编辑，或者 IDEA 编辑（表格预览中可自定义 CSV 配置）
            // 这里 csv 未使用 " 作为转义字符，而是使用 \ 作为转义字符，这样方便直接书写 JSON 字符串
            // 比如：
            // name, comment
            // cat, hellokity
            // dog, "{\"data\": \"ddd\"}"
            CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setEscape('\\')
                .setIgnoreSurroundingSpaces(true)
                .build();
            CSV_FORMAT_MAP.put("GROOT", csvFormat);
        }

        public static CSVFormat getCSVFormat(String format) {
            if (format == null || format.trim().isEmpty()) {
                return CSVFormat.Predefined.Default.getFormat();
            }
            return CSVFormatFactory.CSV_FORMAT_MAP.get(format.toUpperCase());
        }
    }

    // sheetIndex 1-based
    private ParametersData getParametersDataFromExcel(Path path, Map<String, String> params) {
        // ERROR StatusLogger Log4j2 could not find a logging implementation.
        // Please add log4j-core to the classpath. Using SimpleLogger to log to the console...
        System.setProperty("log4j2.loggerContextFactory", "org.apache.logging.log4j.simple.SimpleLoggerContextFactory");

        // 解析参数
        String sheetName = null;
        int sheetIndex = 1;
        if (params != null) {
            sheetName = params.get("name");
            if (sheetName == null || sheetName.trim().isEmpty()) {
                sheetName = null;
                sheetIndex = Integer.parseInt(params.getOrDefault("index", "1"));
            }
        }

        // 一个 Excel 文件对应一个工作薄，一个工作薄对应多个工作表
        // 打开工作薄：支持读取 .xls 和 .xlsx 文件
        try (Workbook wb = WorkbookFactory.create(path.toFile())) {
            Sheet sheet;
            if (sheetName != null) {
                sheet = wb.getSheet(sheetName);
            } else {
                sheet = wb.getSheetAt(sheetIndex - 1);
            }
            int total = sheet.getPhysicalNumberOfRows();
            if (total <= 1)   //无数据或仅有表头数据
                return new ParametersData(new ArrayList<>());

            // 读取工作表
            List<String> headerNames = new ArrayList<>();
            List<Map<String, Object>> data = new ArrayList<>();
            boolean isFirst = true;
            for (Row row : sheet) {
                // 读取表头
                if (isFirst) {
                    isFirst = false;
                    row.forEach(cell -> headerNames.add(cell.getStringCellValue()));
                    continue;
                }
                // 读取数据
                Map<String, Object> rowMap = new HashMap<>();
                for (Cell cell : row) {
                    rowMap.put(headerNames.get(cell.getColumnIndex()), cell.getStringCellValue());
                }
                data.add(rowMap);
            }
            return new ParametersData(data);
        } catch (IOException e) {
            throw new DataLoadException("读取Excel文件失败，文件路径：%s", e, path.toAbsolutePath().toString());
        }
    }

}
