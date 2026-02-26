package com.tianye.hrsystem.common;

import org.springframework.util.ResourceUtils;

/**
 * @ClassName: GlobalContext
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月05日 22:45
 **/
public class GlobalContext {
    public static final String Default = "hrsystem";
    public static final String Prefix = "hr_";

    public static String getStaticUrl() {
        String path = null;
        try {
            String serverpath = ResourceUtils.getURL("classpath:export").getPath().replace("%20", " ").replace('/',
                    '\\');
            path = serverpath.substring(1);//从路径字符串中取出工程路径
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }

    public static String getTemplateUrl() {
        String path = null;
        try {
            String serverpath = ResourceUtils.getURL("classpath:export").getPath().replace("%20", " ").replace
                    ('/', '\\');
            path = serverpath.substring(1);//从路径字符串中取出工程路径
        } catch (Exception e) {
            e.printStackTrace();
        }
        return path;
    }
}