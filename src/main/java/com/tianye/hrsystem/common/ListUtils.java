package com.tianye.hrsystem.common;

import org.springframework.beans.BeanUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class ListUtils {
    public static <T> List<T> parse(String text, Class<T> tClass) {
        List<T> res = new ArrayList<T>();
        if (StringUtils.isEmpty(text) == false) {
            String[] vs = text.split(",");
            for (int i = 0; i < vs.length; i++) {
                String v = vs[i];
                if (tClass == Integer.class) {
                    Integer X = Integer.parseInt(v);
                    res.add((T) X);
                } else if (tClass == String.class) {
                    res.add((T) v);
                }
            }
        }
        return res;
    }
    public static <T> List<T> clone(List<T> source) throws Exception{
        List<T> res=new ArrayList<T>();
        if(source==null) return new ArrayList<T>();
        for(int i=0;i<source.size();i++){
            T t=source.get(i);
            T x=(T)t.getClass().newInstance();
            BeanUtils.copyProperties(t,x);
            res.add(x);
        }
        return res ;
    }
}
