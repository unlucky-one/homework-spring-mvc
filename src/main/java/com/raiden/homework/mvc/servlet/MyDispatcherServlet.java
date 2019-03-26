package com.raiden.homework.mvc.servlet;

import com.raiden.homework.mvc.annotation.MyController;
import com.raiden.homework.mvc.annotation.MyRequestMapping;
import com.raiden.homework.mvc.annotation.MyRequestParam;
import com.sun.deploy.util.ArrayUtil;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.*;
import java.util.Map.Entry;


public class MyDispatcherServlet extends HttpServlet {

    private Properties properties = new Properties();
    private List<String> classNames = new ArrayList<>();
    private Map<String, Object> beanMap = new HashMap<>();
    private Map<String, Method> handlerMapping = new HashMap<>();
    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        loadConfig(config.getInitParameter("contextConfigLocation"));
        scannerPackage(properties.getProperty("scanPackage"));
        instance();
        initHandlerMapping();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (handlerMapping.isEmpty()) {
            return;
        }
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replace(contextPath, "").replaceAll("/+", "/");
        if (!this.handlerMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND!");
            return;
        }
        Method method = this.handlerMapping.get(url);
        Parameter[] parameters = method.getParameters();
        Map<String, String[]> parameterMap = req.getParameterMap();
        Object[] paramValues = new Object[parameters.length];
        for (int i = 0; i < parameters.length; i++) {
            Parameter p = parameters[i];
            if (p.getType().isAssignableFrom(ServletRequest.class)) {
                paramValues[i] = req;
                continue;
            }
            if (p.getType().isAssignableFrom(ServletResponse.class)) {
                paramValues[i] = resp;
                continue;
            }
            if (p.isAnnotationPresent(MyRequestParam.class)) {
                MyRequestParam param = p.getAnnotation(MyRequestParam.class);
                String[] pv = parameterMap.get(param.value());
                if (pv != null) {
                    paramValues[i] = arrayToString(pv);
                }
            }
        }
        try {
            Object result = method.invoke(this.controllerMap.get(url), paramValues);
            if (result instanceof String) {
                resp.getWriter().write(result.toString());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfig(String location) {
        InputStream resourceAsStream = null;
        try {
            if (location.startsWith("classpath:")) {
                resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location.substring("classpath:".length()));
            } else
                resourceAsStream = new FileInputStream(location);
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != resourceAsStream) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void scannerPackage(String packageName) {
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(url.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                scannerPackage(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
            }
        }
    }


    private void instance() {
        if (classNames.isEmpty()) {
            return;
        }
        for (String className : classNames) {
            try {
                Class<?> clazz = Class.forName(className);
                if (clazz.isAnnotationPresent(MyController.class)) {
                    beanMap.put(firstWordToLower(clazz.getSimpleName()), clazz.newInstance());
                } else {
                    continue;
                }
            } catch (Exception e) {
                e.printStackTrace();
                continue;
            }
        }
    }


    private void initHandlerMapping() {
        if (beanMap.isEmpty()) {
            return;
        }
        try {
            for (Entry<String, Object> entry : beanMap.entrySet()) {
                Class<? extends Object> clazz = entry.getValue().getClass();
                if (!clazz.isAnnotationPresent(MyController.class)) {
                    continue;
                }
                String baseUrl = "";
                if (clazz.isAnnotationPresent(MyRequestMapping.class)) {
                    MyRequestMapping annotation = clazz.getAnnotation(MyRequestMapping.class);
                    baseUrl = annotation.value();
                }
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    if (!method.isAnnotationPresent(MyRequestMapping.class)) {
                        continue;
                    }
                    MyRequestMapping annotation = method.getAnnotation(MyRequestMapping.class);
                    String url = annotation.value();
                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    handlerMapping.put(url, method);
                    controllerMap.put(url, clazz.newInstance());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private String firstWordToLower(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    public static String arrayToString(String[] var0) {
        if (var0 == null) {
            return null;
        } else {
            StringBuffer var1 = new StringBuffer();

            for (int var2 = 0; var2 < var0.length; ++var2) {
                var1.append((var0[var2] != null ? var0[var2].replaceAll("\\s", "") : "") + " ");
            }
            return var1.toString();
        }
    }

}
