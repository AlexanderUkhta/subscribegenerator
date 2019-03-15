package filetest;

import com.a1s.file.ExcelReadService;
import org.springframework.beans.factory.annotation.Autowired;

public class FileTest {

    @org.junit.Test
    public void test() {
        ExcelReadService read = new ExcelReadService();
        read.sout();
    }
}
