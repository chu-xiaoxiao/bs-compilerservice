package com.zyc;

import com.zyc.compile.CompileService;
import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by YuChen Zhang on 18/01/08.
 */

public class TestCompile {
    @Test
    public void testCompile() throws IOException {
        CompileService compileService = new CompileService();

        // 將源码写入文件中
        String src = "package com.test.cp;"
                + "public class TestCompiler {"
                + " public static void main(String[] args) {"
                + " System.out.println(1/1);"
                + "}}";
        List<String> codes = new ArrayList<String>();
        codes.add(src);
        System.out.println(compileService.compile(codes));
    }
}
