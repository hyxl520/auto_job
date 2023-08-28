package com.jingge.autojob.util.encrypt;

import com.jingge.autojob.util.convert.StringUtils;
import org.apache.commons.codec.binary.Base64;
import sun.misc.BASE64Decoder;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * AES对称加密算法
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/22 15:33
 */
public class AESUtil {
    private String KEY;
    private final String ALGORITHMSTR = "AES/ECB/PKCS5Padding";

    public static AESUtil build(String key) {
        if (StringUtils.isEmpty(key) || key.length() != 16) {
            throw new IllegalArgumentException("AES算法秘钥长度需为16");
        }
        AESUtil encryptUtil = new AESUtil();
        encryptUtil.setKEY(key);
        return encryptUtil;
    }

    private AESUtil() {
    }

    private void setKEY(String KEY) {
        this.KEY = KEY;
    }

    public String base64Encode(byte[] bytes) {
        return Base64.encodeBase64String(bytes);
    }

    public byte[] base64Decode(String base64Code) throws Exception {
        return new BASE64Decoder().decodeBuffer(base64Code);
    }

    /**
     * 加密
     *
     * @param content 加密内容
     * @return byte[]
     * @author Huang Yongxiang
     * @date 2022/2/16 15:21
     */
    public byte[] aesEncryptToBytes(String content) throws Exception {
        return aesEncryptToBytes(content.getBytes(StandardCharsets.UTF_8));
    }

    public byte[] aesEncryptToBytes(byte[] content) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(KEY.getBytes(), "AES"));

        return cipher.doFinal(content);
    }

    public String aesEncrypt(String content) throws Exception {
        return base64Encode(aesEncryptToBytes(content));
    }

    /**
     * 解密
     *
     * @param encryptBytes 解密密文
     * @return java.lang.String
     * @author Huang Yongxiang
     * @date 2022/2/16 15:22
     */
    public String aesDecryptByBytes(byte[] encryptBytes) throws Exception {
        return new String(aesDecryptByBytesToBytes(encryptBytes), StandardCharsets.UTF_8);
    }

    public byte[] aesDecryptByBytesToBytes(byte[] encryptBytes) throws Exception {
        KeyGenerator kgen = KeyGenerator.getInstance("AES");
        kgen.init(128);
        Cipher cipher = Cipher.getInstance(ALGORITHMSTR);
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(KEY.getBytes(), "AES"));
        return  cipher.doFinal(encryptBytes);
    }

    public String aesDecrypt(String encryptStr) throws Exception {
        return aesDecryptByBytes(base64Decode(encryptStr));
    }


    /**
     * 测试
     */
    public static void main(String[] args) throws Exception {

        AESUtil encryptUtil = AESUtil.build("H83US9J1LF6DQ2ZP");
        AESUtil aesUtil = AESUtil.build("autoJob!@#=123.?");
        String decryptKey = aesUtil.aesDecrypt("7kv12mxGw0a+3ZStMR08Q3Q3xqqrd20PIU3SLa6rH60=");
        System.out.println(URLEncoder.encode("ue8wjdeu0+ldsi==", "utf-8"));
        System.out.println(decryptKey);
        System.out.println(AESUtil.build("GNB6VBSTODCF20H4").aesDecrypt("8mvI3MbeSijkCd1y7IIQ=="));

    }

}
