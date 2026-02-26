package com.tianye.hrsystem.common;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;

import java.util.*;

/**
 * @ClassName: TagUtil
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月24日 7:26
 **/
public class TagUtil {

    private static final String SEPARATOR =",";

    public static Set<Long> toSet(String tagStr){
        Set<Long> tag=new LinkedHashSet<>();
        if(null==tagStr){
            return tag;
        }
        for (String str : tagStr.split(SEPARATOR)) {
            if(StrUtil.isEmpty(str) || ObjectUtil.equal("null",str)){
                continue;
            }
            tag.add(Long.valueOf(str));
        }
        return tag;
    }

    public static List<Long> toLongSet(String tagStr){
        List<Long> tag=new ArrayList<>();
        if(null==tagStr){
            return tag;
        }
        for (String str : tagStr.split(SEPARATOR)) {
            if(StrUtil.isEmpty(str)){
                continue;
            }
            tag.add(Long.valueOf(str));
        }
        return tag;
    }



    public static String fromSet(Collection<Long> tag){
        if(CollectionUtil.isEmpty(tag)){
            return "";
        }
        StringBuilder sb=new StringBuilder(SEPARATOR);
        for (Long value : tag) {
            if(value==null){
                continue;
            }
            sb.append(value).append(SEPARATOR);
        }
        return sb.toString();
    }

    public static String fromStringSet(Collection<String> tag){
        if(CollectionUtil.isEmpty(tag)){
            return "";
        }
        StringBuilder sb=new StringBuilder(SEPARATOR);
        for (String str : tag) {
            if(str==null){
                continue;
            }
            sb.append(str).append(SEPARATOR);
        }
        return sb.toString();
    }

    public static String fromLongSet(Collection<Long> tag){
        if(CollectionUtil.isEmpty(tag)){
            return "";
        }
        StringBuilder sb=new StringBuilder(SEPARATOR);
        for (Long integer : tag) {
            if(integer==null){
                continue;
            }
            sb.append(integer).append(SEPARATOR);
        }
        return sb.toString();
    }

    public static String fromString(String tagStr){
        if(StrUtil.isEmpty(tagStr)){
            return "";
        }
        StringBuilder sb=new StringBuilder();
        if(!tagStr.substring(0,1).equals(SEPARATOR)){
            sb.append(SEPARATOR);
        }
        sb.append(tagStr);
        if(!tagStr.substring(tagStr.length()-1).equals(SEPARATOR)){
            sb.append(SEPARATOR);
        }
        return sb.toString();
    }
    public static boolean isSetEqual(Set set1, Set set2) {

        if (set1 == null && set2 == null) {
            return true;
        }

        if (set1 == null || set2 == null || set1.size() != set2.size()
                || set1.size() == 0 || set2.size() == 0) {
            return false;

        }


        Iterator ite2 = set2.iterator();
        boolean isFullEqual = true;

        while (ite2.hasNext()) {
            if (!set1.contains(ite2.next())) {
                isFullEqual = false;
            }
        }

        return isFullEqual;
    }
}

