package com.liyunx.groot.util;

import com.liyunx.groot.util.FileUtil;
import org.testng.annotations.Test;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class FileUtilTest {

    private static final String TEST_DATA_PATH = "src/test/resources/testdata/util/file_util";

    @Test
    public void testGetFilesStartWith() {
        String prefix = "my";
        List<File> files = FileUtil.getFilesStartWith(Paths.get(TEST_DATA_PATH), prefix);
        files.forEach(file -> assertThat(file.getName()).startsWith(prefix));
    }

}
