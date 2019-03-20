package com.a1s.subscribegeneratorapp.test;

import com.a1s.subscribegeneratorapp.controller.MainController;
import com.a1s.subscribegeneratorapp.service.ExcelReadService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class ReadFromExcelTest {

    @Autowired
    private ExcelReadService excelReadService;

    @MockBean
    private MainController mainController;


    @Test
    public void test() {
//        ExcelReadService read = new ExcelReadService();
//        excelReadService.sout();
    }
}
