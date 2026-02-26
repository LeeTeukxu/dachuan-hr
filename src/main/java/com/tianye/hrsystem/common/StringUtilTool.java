package com.tianye.hrsystem.common;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.poi.util.StringUtil;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

public class StringUtilTool {
    public static  String trim(String source, List<Integer> CharCodes){
        List<String> Res=new ArrayList<>();
        char[] Codes=source.toCharArray();
        for(char c:Codes){
            int N=c;
            if(CharCodes.contains(N)==false){
                Res.add(String.valueOf(c));
            }
        }
        return StringUtil.join(Res.toArray()).trim();
    }
    public static String getValue(Object val) {
        if (val == null) return "";
        if (StringUtils.isEmpty(val) == true) return "";
        try {
            return val.toString();
        } catch (Exception ax) {
            return "";
        }
    }
    public static String readAll(String filePath) throws Exception{
        File f=new File(filePath);
        try (FileInputStream inputStream = new FileInputStream(f)) {
            byte[] BB = new byte[(int) f.length()];
            int bytesRead = inputStream.read(BB);
            return new String(BB, Charset.forName("utf-8"));
        }
    }
    public static  String createByTemplate(String templateText,Object obj)  throws  Exception{
        Configuration configuration = new Configuration();
        StringWriter writer = new StringWriter();
        configuration.setDefaultEncoding("utf-8");
        Template tt = new Template("Template", new StringReader(templateText), configuration);
        tt.process(obj, writer);
        return writer.toString();
    }
}
