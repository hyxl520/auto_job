package com.jingge.autojob.util.encrypt;

import com.alibaba.druid.filter.config.ConfigTools;

/**
 * 使用Druid进行数据库加密的一个小工具
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-10 10:55
 * @email 1158055613@qq.com
 */
public class DBPasswordEncryptHelper {
    private static final String PASSWORD = "1234%^&*@qwer";

    public static void main(String[] args) {
        try {
            System.out.println(System.getProperty("spring.profiles.active"));
            //System.out.println(args.length);
            ConfigTools.main(new String[]{PASSWORD});
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
