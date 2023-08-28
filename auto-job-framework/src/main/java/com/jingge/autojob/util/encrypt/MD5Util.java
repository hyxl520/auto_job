package com.jingge.autojob.util.encrypt;

import com.jingge.autojob.util.convert.StringUtils;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Random;

/**
 * MD5散列加密算法
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/22 15:35
 */
public class MD5Util {
    private String saltKey;
    private String algorithmName = "md5";

    private MD5Util() {
    }

    /**
     * 构建一个工具实例，使用MD5算法进行加密
     *
     * @param saltKey 盐值
     * @return com.example.ammetercommon.utils.password.PasswordHelper
     * @author Huang Yongxiang
     * @date 2022/2/17 16:43
     */
    public static MD5Util build(String saltKey) {
        if (StringUtils.isEmpty(saltKey)) {
            throw new NullPointerException("请指定盐值");
        }
        MD5Util passwordHelper = new MD5Util();
        passwordHelper.saltKey = saltKey;
        return passwordHelper;
    }

    /**
     * 构建一个工具实例
     *
     * @param saltKey       盐值
     * @param algorithmName 算法名，不指定或为空则默认md5算法
     * @return com.example.ammetercommon.utils.password.PasswordHelper
     * @author Huang Yongxiang
     * @date 2022/2/17 16:44
     */
    public static MD5Util build(String saltKey, String algorithmName) {
        if (StringUtils.isEmpty(saltKey)) {
            throw new NullPointerException("请指定盐值");
        }
        algorithmName = StringUtils.isEmpty(algorithmName) ? "md5" : algorithmName;
        MD5Util passwordHelper = new MD5Util();
        passwordHelper.saltKey = saltKey;
        passwordHelper.algorithmName = algorithmName;
        return passwordHelper;
    }

    public String encryptPassword(String password) {
        if (StringUtils.isEmpty(password)) {
            return null;
        }
        byte[] passwordBytes = null;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(algorithmName);
            passwordBytes = messageDigest.digest(messageDigest.digest((password + saltKey).getBytes()));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(String.format("没有这个md5算法：%s！", algorithmName));
        }
        StringBuilder md5code = new StringBuilder(new BigInteger(1, passwordBytes).toString(16));
        for (int i = 0; i < 32 - md5code.length(); i++) {
            md5code.insert(0, "0");
        }
        return md5code.toString();
    }

    public boolean isEqual(String encryptedPassword, String password) {
        if (StringUtils.isEmpty(encryptedPassword) || StringUtils.isEmpty(password)) {
            throw new NullPointerException("请指定参数");
        }
        return encryptPassword(password).equals(encryptedPassword);
    }

    public static String randomSaltKey(int count) {
        if (count < 0) {
            throw new IllegalArgumentException("长度非法");
        }
        return StringUtils.getRandomStr(count);
    }

    public static String randomSaltKey(int minLength, int maxLength) {
        if (minLength < 0 || maxLength < minLength) {
            throw new IllegalArgumentException("长度区间非法");
        }
        int count = new Random().nextInt(maxLength) + minLength + 1;
        return StringUtils.getRandomStr(count);
    }

    public static void main(String[] args) {
        String password = "hyxl0706";
        MD5Util passwordHelper = MD5Util.build("acefkg");
        String encryptedPassword = passwordHelper.encryptPassword(password);
        System.out.println("加密前密码：" + password + "，加密后密码：" + encryptedPassword + "，长度：" + encryptedPassword.length());
        System.out.println("验证结果：" + passwordHelper.isEqual(encryptedPassword, password));
    }
}
