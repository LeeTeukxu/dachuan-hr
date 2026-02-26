package com.tianye.hrsystem.common;

import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * @ClassName: MD5Utils
 * @Author: 肖新民
 * @*TODO:
 * @CreateTime: 2024年03月06日 17:05
 **/
public class MD5Utils {

    public static String enCode(String Content){
        String H1= DigestUtils.md5DigestAsHex(Content.getBytes());
        return DigestUtils.md5DigestAsHex(H1.getBytes());
    }

    public static String encryptMD5(String input) {
        try {
            String md5Hex = DigestUtils.md5DigestAsHex(input.getBytes());
            return md5Hex;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
