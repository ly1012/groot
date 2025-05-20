package com.liyunx.groot.testelement;

import com.liyunx.groot.common.Validatable;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.exception.InvalidDataException;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 参数化数据包装类，方便用例或步骤的参数化。
 */
public class ParametersData implements Validatable, Iterator<Map<String, Object>> {

    // [1..3]
    private static final Pattern REGEX_SEQ_RANGE = Pattern.compile("\\[(-?\\d+)\\.\\.(-?\\d+)]");
    // [1..]
    private static final Pattern REGEX_SEQ_RANGE_LEFT = Pattern.compile("\\[(-?\\d+)\\.\\.]");
    // [..5]
    private static final Pattern REGEX_SEQ_RANGE_RIGHT = Pattern.compile("\\[\\.\\.(-?\\d+)]");
    // [1, 2, 3] [1, 3, -4, -1] [-1]
    private static final Pattern REGEX_SEQ_LIST = Pattern.compile("\\[(-?\\d+)(,(-?\\d+))*]");
    private static final Pattern REGEX_SEQ_LIST_EXTRACT = Pattern.compile(",?(-?\\d+)");

    public enum DataSourceType {
        DATA,
        FILE,
        EXPRESSION;

        public static boolean needCopy(DataSourceType dataSourceType) {
            return !DataSourceType.FILE.equals(dataSourceType);
        }

    }

    // 参数化数据
    private final List<Map<String, Object>> data;

    // 当前迭代计数
    private int loopCount;
    // 数据集大小
    private final int size;

    // 数据源描述，如 file: testcases/data/user.json
    private DataSourceType dataSourceType;
    private String dataSourceDescription = "unknown";

    public ParametersData(List<Map<String, Object>> data) {
        if (data == null)
            throw new IllegalArgumentException("参数化数据不能为 null");
        this.data = data;
        this.size = data.size();
    }

    public ParametersData updateDataSourceType(DataSourceType dataSourceType) {
        this.dataSourceType = dataSourceType;
        return this;
    }

    public ParametersData updateDataSourceDescription(String dataSourceDescription) {
        this.dataSourceDescription = dataSourceDescription;
        return this;
    }

    /**
     * 返回数据集大小，即有多少组数据
     *
     * @return 行数
     */
    public int length() {
        return size;
    }

    /**
     * 返回数据集
     *
     * @return 数据集
     */
    public List<Map<String, Object>> getData() {
        return data;
    }

    /**
     * 获取所有列名（不保证列名顺序和原数据列名顺序相同）
     *
     * @return 所有列名
     */
    public List<String> getNames() {
        if (size < 1) {
            return Collections.emptyList();
        }
        return Arrays.asList(data.get(0).keySet().toArray(new String[0]));
    }

    /**
     * 解析序列字符串为序列列表。
     *
     * <p><br>合法的序列字符串：
     * <pre><code>
     *   [1..3]
     *   [1..]
     *   [..5]
     *   [1, 2, 3]
     *   [1, 3, -4, -1]
     * </code></pre>
     *
     * @param seq 序列字符串
     * @return 序列字符串对应的序列列表
     */
    public static List<Integer> parseSeq(String seq, int size) {
        // 默认返回所有
        if (seq == null || seq.trim().isEmpty()) {
            // 空数据集
            if (size == 0) {
                return new ArrayList<>();
            }
            return getSeqListByRange(1, size, size,"[1..-1]");
        }

        // 返回符合序列字符串的序列列表
        seq = seq.replaceAll(" ", "");
        Matcher matcher;
        //[1..3]
        if ((matcher = REGEX_SEQ_RANGE.matcher(seq)).matches()) {
            // 提取上下边界值
            int left = Integer.parseInt(matcher.group(1));
            int right = Integer.parseInt(matcher.group(2));

            // 计算上下边界值
            left = left < 0 ? left + size + 1 : left;
            right = right < 0 ? right + size + 1 : right;

            return getSeqListByRange(left, right, size, seq);
        }
        // [1..]
        else if ((matcher = REGEX_SEQ_RANGE_LEFT.matcher(seq)).matches()) {
            // 提取下边界
            int left = Integer.parseInt(matcher.group(1));

            // 计算上下边界值
            left = left < 0 ? left + size + 1 : left;

            return getSeqListByRange(left, size, size, seq);
        }
        // [..5]
        else if ((matcher = REGEX_SEQ_RANGE_RIGHT.matcher(seq)).matches()) {
            // 提取上边界
            int right = Integer.parseInt(matcher.group(1));

            // 计算上下边界值
            int left = 1;
            right = right < 0 ? right + size + 1 : right;

            return getSeqListByRange(left, right, size, seq);
        }
        // [1, 3, -4, -1]
        else if (REGEX_SEQ_LIST.matcher(seq).matches()) {
            List<Integer> seqList = new ArrayList<>();

            // 提取列表值
            Matcher extractMatcher = REGEX_SEQ_LIST_EXTRACT.matcher(seq);
            int matcher_start = 0;
            while (extractMatcher.find(matcher_start)) {
                seqList.add(Integer.parseInt(extractMatcher.group(1)));
                matcher_start = extractMatcher.end();
            }

            // 计算列表值
            int value;
            for (int i = 0; i < seqList.size(); i++) {
                value = seqList.get(i);
                if (value < 0) {
                    seqList.set(i, value + size + 1);
                }
            }

            // 去重
            seqList = seqList.stream().distinct().collect(Collectors.toList());
            // 排序
            Collections.sort(seqList);

            // 边界值验证
            int left = seqList.get(0);
            int right = seqList.get(seqList.size() - 1);   //注：size 一定是大于 0 的，否则正则匹配通不过
            checkBoundaryValue(left, right, size, seq);

            return seqList;
        } else {
            String builder = "序列字符串非法，当前值：" +
                seq +
                "，当前仅支持范围序列和列表序列，如下所示：\n" +
                "左右边界：[1..8] 或 [3..-2]\n左边界：[2..] 或 [-5..]\n右边界：[..6] 或 [..-3]\n" +
                "指定列表：[1, 3, 6] 或 [-5, -1] 或 [1, 5, -2]";
            throw new InvalidDataException(builder);
        }
    }

    private static List<Integer> getSeqListByRange(int left, int right, int size, String rawSeq) {
        List<Integer> seqList = new ArrayList<>();

        // 边界值验证
        checkBoundaryValue(left, right, size, rawSeq);

        // 计算序列列表
        for (int i = left; i <= right; i++) {
            seqList.add(i);
        }
        return seqList;
    }

    private static void checkBoundaryValue(int left, int right, int size, String rawSeq) {
        if (left < 1 || right > size)
            throw new InvalidDataException(String.format("序列范围超出边界限制，数据集大小：%d，最大区间 [1, %d], 当前区间：%s", size, size, rawSeq));
        if (left > right)
            throw new InvalidDataException(String.format("序列范围非法，左边界应不大于右边界，数据集大小：%d，当前区间：%s", size, rawSeq));
    }

    @Override
    public ValidateResult validate() {
        // TODO 报错信息待优化，只提示不同的部分。
        // 深度检查？当前只检查了第一级
        ValidateResult r = new ValidateResult();

        // 无数据，默认验证成功
        if (size == 0) return r;

        r.appendDescription("参数化数据加载失败，数据源：").appendDescription(dataSourceDescription);

        // 元素类型验证
        for (int i = 0; i < data.size(); i++) {
            Object o = data.get(i);
            if (!(o instanceof Map)) {
                r.append("\n第 %d 行数据：元素类型错误，期望类型：Map<String, Object>，当前类型： %s",
                    i + 1,
                    o.getClass().getName());
            }
        }
        if (!r.isValid()) return r;

        // 列名验证
        Set<String> baselineColumnNames = data.get(0).keySet();   //基准列名
        for (int i = 1; i < data.size(); i++) {
            Map<String, Object> map = data.get(i);
            if (!baselineColumnNames.equals(map.keySet())) {
                r.append("\n第 %d 行数据：列名不一致，期望列名集合：%s，当前列名集合：%s",
                    i + 1,
                    baselineColumnNames.toString(),
                    map.keySet().toString());
            }
        }
        if (!r.isValid()) return r;

        // 值类型验证（现在每组数据的列名都是一致的了）
        Map<String, Class> baselineTypes = getValueTypes(data.get(0));     //基准类型
        for (int i = 1; i < data.size(); i++) {
            Map<String, Class> dataTypes = getValueTypes(data.get(i));
            if (!baselineTypes.equals(dataTypes)) {
                r.append("\n第 %d 行数据：值类型不一致，\n期望值类型：%s，\n当前值类型：%s",
                    i + 1,
                    baselineTypes,
                    dataTypes);
            }
        }
        return r;
    }

    private Map<String, Class> getValueTypes(Map<String, Object> map) {
        Map<String, Class> clazzMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            clazzMap.put(entry.getKey(), entry.getValue().getClass());
        }
        return clazzMap;
    }

    @Override
    public boolean hasNext() {
        // 为性能优化留个口子? 比如一百万行的 Excel/CSV 参数化数据，如果一次全部加载，内存占用会较高，自动化有这可能？
        throw new UnsupportedOperationException("参数化数据暂不支持迭代器");
    }

    @Override
    public Map<String, Object> next() {
        throw new UnsupportedOperationException("参数化数据暂不支持迭代器");
    }

    public DataSourceType getDataSourceType() {
        return dataSourceType;
    }

}
