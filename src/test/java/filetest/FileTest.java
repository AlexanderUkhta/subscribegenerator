package filetest;

import com.a1s.file.ExcelReadService;

public class FileTest {
    @org.junit.Test
    public void test() {
        ExcelReadService read = new ExcelReadService();
        read.sout();
    }
}
