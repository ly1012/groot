package com.liyunx.groot.util;

import com.liyunx.groot.exception.GrootException;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @Test(expectedExceptions = GrootException.class)
    public void testReadFileWithNotExists() {
        FileUtil.readFile(Paths.get("no/no.txt"));
    }

    @Test
    public void testCreateFile() throws IOException {
        Path path = Paths.get("target/temp/abc/my_file.txt");
        // 文件不存在时创建，父文件夹不存在时创建
        FileUtil.createFileOrDirectory(path.toString(), false, true);
        assert Files.exists(path);
        // 文件已存在时先删除再创建
        FileUtil.createFileOrDirectory(path.toString(), false, true);
        assert Files.exists(path);
        // 删除创建的文件和文件夹
        Files.deleteIfExists(path);
        Files.deleteIfExists(path.getParent());
        Files.deleteIfExists(path.getParent().getParent());
    }

    @Test
    public void testCreateDirectory() throws IOException {
        Path path = Paths.get("target/temp/abc");
        FileUtil.createFileOrDirectory(path.toString(), true, true);
        assert Files.exists(path);
        Files.deleteIfExists(path);
        Files.deleteIfExists(path.getParent());
    }

    @Test
    public void testGetExtension() {
        String extension = FileUtil.getExtension("src/test/resources/testdata/util/file_util/cat.txt");
        assertThat(extension).isEqualTo("txt");
    }

}
