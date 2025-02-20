package com.liyunx.groot.dataloader.file;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.liyunx.groot.testelement.TestCase;
import com.liyunx.groot.testelement.TestElement;
import com.liyunx.groot.util.FileUtil;
import com.liyunx.groot.util.YamlUtil;

import java.nio.file.Path;

/**
 * 测试用例数据：本地文件加载
 */
public class TestCaseFileLoader extends LocalDataLoader {

    @Override
    @SuppressWarnings({"unchecked"})
    protected <T> T next(String text, String textType, Class<T> clazz) {
        if (TestCase.class.equals(clazz)) {
            requireNotEmpty(text, textType, clazz);
            if (FileType.isJSONFile(textType)) {
                JSONObject jsonObject = JSONObject.parseObject(text, JSONObject.class);
                jsonObject.put(TestCase.KEY, "");
                String jsonStr = JSON.toJSONString(jsonObject);
                return (T) JSON.parseObject(jsonStr, TestElement.class);
            }
            if (FileType.isYamlFile(textType)) {
                // Yaml 为非线程安全类，应为每个 Stream 创建各自的 Yaml 对象。
                // 或许应该强制每个 TestCaseRunner 使用各自的 Configuration 实例？
                JSONObject jsonObject = YamlUtil.getYaml().loadAs(text, JSONObject.class);
                jsonObject.put(TestCase.KEY, "");
                String jsonStr = JSON.toJSONString(jsonObject);
                return (T) JSON.parseObject(jsonStr, TestElement.class);
            }
            return null;
        }
        return null;
    }

    @Override
    protected <T> T nextByID(String identifier, Class<T> clazz) {
        if (TestCase.class.equals(clazz)) {
            Path path = getAbsolutePath(identifier);
            // 读取文本内容，传递给 next 方法处理
            return next(FileUtil.readFile(path), getFileType(path), clazz);
        }
        return null;
    }

}
