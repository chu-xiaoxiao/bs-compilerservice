package com.zyc.controller;

import com.zyc.compile.CompileService;
import com.zyc.util.JSONResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * Created by YuChen Zhang on 18/01/08.
 */
@RestController
@RequestMapping("compiler")
public class CompileController {
    @Autowired
    private CompileService compileService;

    @RequestMapping(value = "java",method = RequestMethod.POST)
    public String compileJava(@RequestBody Iterable codes){
        String result = compileService.compile(codes);
        return JSONResult.fillResultString("編譯成功",result);
    }
}
