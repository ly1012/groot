package com.liyunx.groot.testelement.controller;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.annotation.JSONField;
import com.liyunx.groot.annotation.KeyWord;
import com.liyunx.groot.builder.TestBuilder;
import com.liyunx.groot.common.ValidateResult;
import com.liyunx.groot.config.TestElementConfig;
import com.liyunx.groot.config.builtin.VariableConfigItem;
import com.liyunx.groot.context.ContextWrapper;
import com.liyunx.groot.context.TestStepContext;
import com.liyunx.groot.dataloader.DataLoadException;
import com.liyunx.groot.dataloader.DataLoader;
import com.liyunx.groot.support.Customizer;
import com.liyunx.groot.support.GroovySupport;
import com.liyunx.groot.testelement.DefaultTestResult;
import com.liyunx.groot.testelement.ParametersData;
import groovy.lang.Closure;
import groovy.lang.DelegatesTo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.liyunx.groot.config.builtin.VariableConfigItem.variableCopy;
import static com.liyunx.groot.testelement.ParametersData.DataSourceType.needCopy;

/**
 * 参数化控制器，每次使用一组新的参数进行循环。
 */
@KeyWord(ForEachController.KEY)
public class ForEachController extends AbstractContainerController<ForEachController, DefaultTestResult> {

    private static final Logger log = LoggerFactory.getLogger(ForEachController.class);

    public static final String KEY = "for";

    /* ------------------------------------------------------------ */
    // 声明时数据，不可修改

    @JSONField(name = KEY)
    private ForSettings forSettings;

    /* ------------------------------------------------------------ */
    // 运行时数据，对外开放

    private int currentIterationIndex;
    private int loopCount;
    private Map<String, Object> columnData;

    public ForEachController() {
    }

    private ForEachController(Builder builder) {
        super(builder);
        this.forSettings = builder.forSettings;
    }

    @Override
    protected void execute(ContextWrapper ctx, DefaultTestResult testResult) {
        // 加载参数化数据
        ParametersData data = getParametersData(ctx);    //data 不会为 null
        ValidateResult r = data.validate();
        if (!r.isValid())
            throw new DataLoadException(r.getReason());

        FilterSettings filterSettings = forSettings.getFilter();

        // seq 过滤：获取需要执行的行索引（从 1 开始）
        String seq = filterSettings == null
            ? null
            : filterSettings.getSlice();
        List<Integer> seqList = ParametersData.parseSeq(seq, data.length());

        // 循环参数化数据
        loopCount = 0;
        List<Map<String, Object>> rawData = data.getData();
        VariableConfigItem variables = ctx.getAllVariablesWrapper().getLastVariableConfigItem();
        for (int index : seqList) {
            Map<String, Object> row = rawData.get(index - 1);
            if (needCopy(data.getDataSourceType())) {
                // 获取本组数据并计算其中的表达式
                // 这里需要深拷贝，因为每组驱动数据相当于变量声明，而 value 可能是引用类型，不能直接使用声明数据，这样会导致声明数据可能被修改，
                // 导致程序出现不可预知的行为。
                // 比如下面这段伪代码：
                // for(int i = 0; i < 3; i++) {
                //     List<String> list = new ArrayList<>();
                //     list.add(ctx.eval("${uuid()}"));
                //     sendHttp(list);
                // }
                // 伪代码中的 list 就相当于驱动数据的一个变量，子元件对该变量进行修改，比如存储商品 ID，最后使用 list 作为参数，发送请求。
                // 可以看出，在代码中是循环内声明的 list，但在 Groot 中有所不同，Groot 中的 list 是提前声明的（构建用例对象时），即在循环外。
                // 因此每次循环体执行前，应该基于声明数据进行深拷贝，得到运行时数据，类似以下伪代码：
                // List<String> list = new ArrayList<>();
                // list.add("${uuid()}");
                // for(int i = 0; i < 3; i++) {
                //     List<String> runningList = list.copy();
                //     ctx.eval(runningList);
                //     sendHttp(runningList);
                // }
                // 当对象不需要深拷贝时（比如共享对象或每次返回新对象），需要使用  BeanSupplier 做特殊处理
                row = variableCopy(row);
            }
            try {
                row = (Map<String, Object>) ctx.eval(row);
            } catch (UnsupportedOperationException e) {
                String errorMessage = String.format("ForEachController 第 %d 组（行）数据计算失败，Map 本身或其内容中可能存在不可变类，无法原地更新", index);
                throw new UnsupportedOperationException(errorMessage, e);
            }

            // 计算本次循环的列数据
            Map<String, Object> col = new HashMap<>();
            List<String> names;
            if (filterSettings != null && (names = filterSettings.getNames()) != null) {   //names 过滤：列过滤
                for (String name : names) {
                    if (name != null && !name.trim().isEmpty()) {
                        col.put(name, row.get(name));
                    }
                }
            } else {
                col.putAll(row);
            }

            // 更新当前上下文的变量
            variables.putAll(col);

            // 执行子元件，condition 过滤：行过滤
            // TODO condition 使用脚本语言代替模板计算？如果使用脚本语言可能性能较差，也可能无法使用注册函数
            // 或者增加额外的字段，如 groovy: "expression"、javascript: "expression"
            // 或者用户使用函数，如 ${groovy("some code")}
            String condition;
            boolean shouldExecute =
                (filterSettings == null || (condition = filterSettings.getCondition()) == null)   //未声明 condition
                    || Boolean.parseBoolean(ctx.evalAsString(condition));                         //声明了 condition
            if (shouldExecute) {
                // 这里没用类似 running.currentIterationIndex，因为每次循环都会覆盖，所以不需要
                currentIterationIndex = index;
                columnData = col;
                loopCount++;
                withTestStepNumberLog(ctx, loopCount, () -> {
                    log.info("使用第 {} 行数据：{}", index, JSON.toJSONString(columnData));
                    executeSubSteps(ctx);
                });
            }
        }
    }

    /*
    数据读取优先级（靠前的优先级高）：
    直接声明数据 -> 文件数据 -> 表达式计算的数据
     */
    private ParametersData getParametersData(ContextWrapper contextWrapper) {
        if (forSettings.getData() != null) {                //尝试直接获取参数化数据
            return new ParametersData(forSettings.getData())
                .updateDataSourceType(ParametersData.DataSourceType.DATA)
                .updateDataSourceDescription("direct: 字面量（column/row/table）");
        } else if (hasValue(forSettings.getFile())) {       //尝试从文件中读取参数化数据（如果是平台，file 也可以是数据 ID）
            DataLoader dataLoader = contextWrapper.getSessionRunner().getTestRunner().getGroot()
                .getConfiguration()
                .getDataLoader();
            String identifier = contextWrapper.evalAsString(forSettings.getFile());
            return dataLoader
                .loadByID(identifier, ParametersData.class)
                .updateDataSourceType(ParametersData.DataSourceType.FILE)
                .updateDataSourceDescription(String.format("file: %s", identifier));
        } else if (hasValue(forSettings.getExpression())) {  //尝试获取函数返回值作为参数化数据
            // 表达式计算
            Object value = contextWrapper.eval(forSettings.getExpression());

            // 返回值验证
            if (value == null) {
                throw new DataLoadException(
                    String.format("参数化数据加载失败，表达式 %s 的返回值为 null", forSettings.getExpression()));
            }
            if (!(value instanceof List)) {
                throw new DataLoadException(
                    String.format("参数化数据加载失败，表达式 %s 的返回值不是 List<Map<String, Object>> 类型", forSettings.getExpression()));
            }

            // 构造参数化数据对象
            return new ParametersData((List) value)
                .updateDataSourceType(ParametersData.DataSourceType.EXPRESSION)
                .updateDataSourceDescription(String.format("expression: %s", forSettings.getExpression()));
        } else {
            throw new DataLoadException("字段：ForEachController.for，参数化设置缺失");
        }
    }

    @Override
    protected DefaultTestResult createTestResult() {
        return new DefaultTestResult();
    }

    @Override
    public ForEachController copy() {
        ForEachController self = super.copy();
        self.forSettings = forSettings;
        return self;
    }

    @Override
    protected TestStepContext createCurrentContext() {
        TestStepContext ctx = super.createCurrentContext();
        if (ctx.getConfigGroup() == null)
            ctx.setConfigGroup(new TestElementConfig());
        if (ctx.getConfigGroup().getVariableConfigItem() == null)
            ctx
                .getConfigGroup()
                .put(VariableConfigItem.KEY, new VariableConfigItem());
        return ctx;
    }

    @Override
    public ValidateResult validate() {
        ValidateResult r = super.validate();

        if (forSettings == null
            || (!hasValue(forSettings.getFile()) && !hasValue(forSettings.getExpression()) && forSettings.getData() == null)) {
            r.append("\n字段：ForEachController.for，参数化设置缺失");
            return r;
        }

        return r;
    }

    private boolean hasValue(String obj) {
        return obj != null && !obj.trim().isEmpty();
    }

    // ---------------------------------------------------------------------
    // Getter/Setter (ForEachController)
    // ---------------------------------------------------------------------

    public ForSettings getForSettings() {
        return forSettings;
    }

    public void setForSettings(ForSettings forSettings) {
        this.forSettings = forSettings;
    }

    public int getCurrentIterationIndex() {
        return currentIterationIndex;
    }

    public int getLoopCount() {
        return loopCount;
    }

    public Map<String, Object> getColumnData() {
        return columnData;
    }

    // ---------------------------------------------------------------------
    // Builder (ForEachController.Builder)
    // ---------------------------------------------------------------------

    public static class Builder
        extends AbstractContainerController.Builder<ForEachController, Builder, DefaultTestResult> {

        private ForSettings forSettings;

        /**
         * ForEach 控制器设置
         *
         * @param forSettings 控制器设置函数
         * @return 当前对象
         */
        public Builder forSettings(Customizer<ForSettings.Builder> forSettings) {
            ForSettings.Builder builder = new ForSettings.Builder();
            forSettings.customize(builder);
            this.forSettings = builder.build();
            return this;
        }

        /**
         * ForEach 控制器设置
         *
         * @param cl 控制器设置闭包
         * @return 当前对象
         */
        public Builder forSettings(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = ForSettings.Builder.class) Closure<?> cl) {
            ForSettings.Builder builder = new ForSettings.Builder();
            GroovySupport.call(cl, builder);
            this.forSettings = builder.build();
            return this;
        }

        @Override
        public ForEachController build() {
            return new ForEachController(this);
        }

    }

    /**
     * ForEachController 关键字属性类
     */
    public static class ForSettings {

        //数据读取优先级（靠前的优先级高）：
        //直接声明数据 -> 文件数据 -> 表达式计算的数据

        /**
         * 使用声明数据作为驱动数据
         */
        @JSONField(name = "data")
        private List<Map<String, Object>> data;

        /**
         * 使用文件作为驱动数据
         */
        @JSONField(name = "file")
        private String file;

        /**
         * 将表达式计算结果作为驱动数据
         */
        @JSONField(name = "expression")
        private String expression;

        @JSONField(name = "filter")
        private FilterSettings filter;

        // TODO 变量别名，可以自定义变量保存当前循环信息

        public ForSettings() {
        }

        private ForSettings(Builder builder) {
            this.data = builder.data;
            this.file = builder.file;
            this.expression = builder.expression;
            this.filter = builder.filter;
        }

        // ---------------------------------------------------------------------
        // Getter/Setter (ForEachController.ForSettings)
        // ---------------------------------------------------------------------

        public String getFile() {
            return file;
        }

        public void setFile(String file) {
            this.file = file;
        }

        public String getExpression() {
            return expression;
        }

        public void setExpression(String expression) {
            this.expression = expression;
        }

        public List<Map<String, Object>> getData() {
            return data;
        }

        public void setData(List<Map<String, Object>> data) {
            this.data = data;
        }

        public FilterSettings getFilter() {
            return filter;
        }

        public void setFilter(FilterSettings filter) {
            this.filter = filter;
        }

        // ---------------------------------------------------------------------
        // Builder (ForEachController.ForSettings.Builder)
        // ---------------------------------------------------------------------

        public static class Builder implements TestBuilder<ForSettings> {

            private List<Map<String, Object>> data;
            private String file;
            private String expression;
            private FilterSettings filter;

            /**
             * 使用参数值作为驱动数据
             *
             * @param data 驱动数据
             * @return 当前对象
             */
            public Builder data(List<Map<String, Object>> data) {
                this.data = data;
                return this;
            }

            /**
             * 从文件中读取驱动数据
             *
             * @param filePath 文件路径，相对路径或绝对路径
             * @return 当前对象
             */
            public Builder file(String filePath) {
                this.file = filePath;
                return this;
            }

            /**
             * 将表达式计算结果作为驱动数据
             *
             * @param expression 表达式
             * @return 当前对象
             */
            public Builder expression(String expression) {
                this.expression = expression;
                return this;
            }

            /**
             * 驱动数据过滤设置
             *
             * @param filter 过滤设置函数
             * @return 当前对象
             */
            public Builder filter(Customizer<FilterSettings.Builder> filter) {
                FilterSettings.Builder builder = new FilterSettings.Builder();
                filter.customize(builder);
                this.filter = builder.build();
                return this;
            }

            /**
             * 驱动数据过滤设置
             *
             * @param filter 过滤设置闭包
             * @return 当前对象
             */
            public Builder filter(@DelegatesTo(strategy = Closure.DELEGATE_ONLY, value = FilterSettings.Builder.class) Closure<?> filter) {
                FilterSettings.Builder builder = new FilterSettings.Builder();
                GroovySupport.call(filter, builder);
                this.filter = builder.build();
                return this;
            }

            @Override
            public ForSettings build() {
                return new ForSettings(this);
            }

        }
    }

    /**
     * 数据过滤设置：过滤行和列
     */
    public static class FilterSettings {

        /**
         * 过滤列：参数化数据仅包含以下列，示例：
         * <code>names: [role, username, password]</code>
         */
        @JSONField(name = "names")
        private List<String> names;

        /**
         * 过滤行：参数化数据仅包含以下行（1-based），注意值为字符串
         * <code>seq: "[2..-1]"</code>
         * <p>
         * 其他示例：
         * <pre><code>
         * [1..3]
         * [1..]
         * [..4]
         * [1, 2, 3]
         * [1, 3, -4, -1]
         * </code></pre>
         */
        @JSONField(name = "slice")
        private String slice;

        /**
         * 过滤行：参数化数据仅包含符合以下条件的行，示例：
         * <code>condition: ${role == 'guest'}</code>
         */
        @JSONField(name = "condition")
        private String condition;

        public FilterSettings() {
        }

        private FilterSettings(Builder builder) {
            this.names = builder.names;
            this.slice = builder.slice;
            this.condition = builder.condition;
        }

        // ---------------------------------------------------------------------
        // Getter/Setter (ForEachController.FilterSettings)
        // ---------------------------------------------------------------------

        public List<String> getNames() {
            return names;
        }

        public void setNames(List<String> names) {
            this.names = names;
        }

        public String getSlice() {
            return slice;
        }

        public void setSlice(String slice) {
            this.slice = slice;
        }

        public String getCondition() {
            return condition;
        }

        public void setCondition(String condition) {
            this.condition = condition;
        }

        // ---------------------------------------------------------------------
        // Builder (ForEachController.FilterSettings.Builder)
        // ---------------------------------------------------------------------

        public static class Builder implements TestBuilder<FilterSettings> {

            private List<String> names;
            private String slice;
            private String condition;

            /**
             * 过滤列：参数化数据仅包含以下列，示例：
             * <code>names: [role, username, password]</code>
             *
             * @param names 列名列表
             * @return 当前对象
             */
            public Builder names(List<String> names) {
                this.names = names;
                return this;
            }

            /**
             * 过滤列：参数化数据仅包含以下列，示例：
             * <code>names: [role, username, password]</code>
             *
             * @param names 列名数组
             * @return 当前对象
             */
            public Builder names(String... names) {
                this.names = Arrays.asList(names);
                return this;
            }

            /**
             * 过滤行：参数化数据仅包含以下行（1-based），注意值为字符串
             * <code>slice: "[2..-1]"</code>
             * <p>
             * 其他示例：
             * <pre><code>
             * [1..3]
             * [1..]
             * [..4]
             * [1, 2, 3]
             * [1, 3, -4, -1]
             * </code></pre>
             *
             * @param slice 切片
             * @return 当前对象
             */
            public Builder slice(String slice) {
                this.slice = slice;
                return this;
            }

            /**
             * 过滤行：参数化数据仅包含符合以下条件的行，示例：
             * <code>condition: ${role == 'guest'}</code>
             *
             * @param condition 行过滤表达式
             * @return 当前对象
             */
            public Builder condition(String condition) {
                this.condition = condition;
                return this;
            }

            @Override
            public FilterSettings build() {
                return new FilterSettings(this);
            }
        }

    }


}
