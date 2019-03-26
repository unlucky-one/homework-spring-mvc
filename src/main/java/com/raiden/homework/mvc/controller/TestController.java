package com.raiden.homework.mvc.controller;

import com.raiden.homework.mvc.annotation.MyController;
import com.raiden.homework.mvc.annotation.MyRequestMapping;
import com.raiden.homework.mvc.annotation.MyRequestParam;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;

/**
 * Created with IntelliJ IDEA.
 * Description:
 * Author: Raiden
 * Date: 2019/3/25
 */

@MyController
@MyRequestMapping("/test")
public class TestController {

    @MyRequestMapping("/do")
    public String test(HttpServletRequest request, @MyRequestParam("a") String a) {
        return "hello world!"+a;
    }
}
