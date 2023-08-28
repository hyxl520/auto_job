package com.jingge.autojob.util.convert;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @Description 正则表达式工具类
 * @Auther Huang Yongxiang
 * @Date 2021/12/17 16:31
 */
@NoArgsConstructor
@Getter
@Accessors(chain = true)
@Setter(AccessLevel.PRIVATE)
public class RegexUtil {
    public enum Type {
        /**
         * 数字，包含小数
         */
        NUM(0, "^(\\-|\\+)?\\d+(\\.\\d+)?$", "数字，包含小数"),
        /**
         * 小数
         */
        FLOAT(1, "^(\\-|\\+)?\\d+(\\.\\d+){1}$", "小数"),
        /**
         * 整数，无位数限制
         */
        INTEGER(2, "^(\\-|\\+)?\\d+$", "整数"),
        /**
         * 数字字符串，不包含+-号，不限制位数
         */
        NUM_STRING(16, "^\\d+$", "数字字符串"),
        /**
         * 全中文
         */
        CHINESE(3, "^[\\u4e00-\\u9fa5]{0,}$", "全中文"),
        /**
         * 部分是中文
         */
        INCLUDE_CHINESE(4, "[\\u4e00-\\u9fa5]{0,}", "部分是中文"),
        /**
         * 包含英文和数字的字符串
         */
        ENGLISH_NUM(5, "^[A-Za-z0-9]+$", "英文和数字"),
        /**
         * 邮件地址
         */
        EMAIL(6, "^\\w+([-+.]\\w+)*@\\w+([-.]\\w+)*\\.\\w+([-.]\\w+)*$", "邮件地址"),
        /**
         * 手机号码
         */
        MOBILE_PHONE(7, "^(13[0-9]|14[5|7]|15[0|1|2|3|4|5|6|7|8|9]|18[0|1|2|3|5|6|7|8|9])\\d{8}$", "手机号码"),
        /**
         * 电话号码
         */
        CHINESE_PHONE(8, "\\d{3}-\\d{8}|\\d{4}-\\d{7}", "电话号码"),
        /**
         * 身份证号
         */
        IDENTITY_NUM(9, "(^\\d{15}$)|(^\\d{18}$)|(^\\d{17}(\\d|X|x)$)", "身份证号"),
        /**
         * 密码：以字母开头，长度在6~18之间，只能包含字母、数字和下划线
         */
        PASSWORD(10, "^[a-zA-Z]\\w{5,17}$", "密码：以字母开头，长度在6~18之间，只能包含字母、数字和下划线"),
        /**
         * 强密码：必须包含大小写字母和数字的组合，不能使用特殊字符，长度在 8-10 之间
         */
        STRONG_PASSWORD(11, "^(?=.*\\d)(?=.*[a-z])(?=.*[A-Z])[a-zA-Z0-9]{8,10}$", "强密码：必须包含大小写字母和数字的组合，不能使用特殊字符，长度在 8-10 之间"),
        /**
         * 日期：yyyy-MM-dd格式
         */
        DATE_YYYY_MM_DD(12, "^\\d{4}-\\d{1,2}-\\d{1,2}$", "日期：yyyy-MM-dd格式"),
        /**
         * 日期：yyyyMMdd格式
         */
        DATE_YYYYMMDD(13, "^\\d{4}\\d{2}\\d{2}$", "日期：yyyyMMdd"),
        /**
         * 日期时间格式：yyyy-MM-dd HH:mm:ss
         */
        DATE_TIME_YYYY_MM_DD_HH_MM_SS(14, "^\\d{4}-\\d{1,2}-\\d{1,2}\\s{1}\\d{2}:\\d{2}:\\d{2}$", "日期时间格式：yyyy-MM-dd " + "HH:mm:ss"),
        /**
         * 逗号分隔的数字，如111或111,222
         */
        STRING_SPLIT_COMMA(15, "^(\\d+,)*(\\d+){1}$", "逗号分隔的数字，如111或111,222");
        private Integer flag;
        private String regexString;
        private String description;

        Type(Integer flag, String regexString, String description) {
            this.flag = flag;
            this.regexString = regexString;
            this.description = description;
        }

        public Integer getFlag() {
            return flag;
        }

        public void setFlag(Integer flag) {
            this.flag = flag;
        }

        public String getRegexString() {
            return regexString;
        }

        public void setRegexString(String regexString) {
            this.regexString = regexString;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    private Pattern pattern;
    private Matcher matcher;
    private String regexString;

    /**
     * 生成可供重复调用的RegexUtil对象，多例
     *
     * @param regexString 正则表达式
     * @return com.sccl.common.utils.RegexUtil
     * @author Huang Yongxiang
     * @date 2021/12/29 11:27
     */
    public static RegexUtil build(String regexString) {
        RegexUtil regexUtil = new RegexUtil();
        regexUtil.setPattern(getPattern(regexString));
        regexUtil.setRegexString(regexString);
        return regexUtil;
    }

    public static RegexUtil build(Type type) {
        RegexUtil regexUtil = new RegexUtil();
        regexUtil.setPattern(getPattern(type.regexString));
        regexUtil.setRegexString(type.regexString);
        return regexUtil;
    }

    /**
     * 将通配符表达式转为正则表达式
     *
     * @param path 通配符路径，类似:a/b/**
     * @return java.lang.String
     * @author Huang Yongxiang
     * @date 2022/3/18 16:30
     */
    public static String wildcardToRegexString(String path) {
        if(StringUtils.isEmpty(path)){
            return "";
        }
        path=path.replace(".","\\.");
        char[] chars = path.toCharArray();
        int len = chars.length;
        StringBuilder sb = new StringBuilder();
        boolean preX = false;
        for (int i = 0; i < len; i++) {
            if (chars[i] == '*') {//遇到*字符
                if (preX) {//如果是第二次遇到*，则将**替换成.*
                    sb.append(".*");
                    preX = false;
                } else if (i + 1 == len) {//如果是遇到单星，且单星是最后一个字符，则直接将*转成[^/]*
                    sb.append("[^/]*");
                } else {//否则单星后面还有字符，则不做任何动作，下一把再做动作
                    preX = true;
                }
            } else {//遇到非*字符
                if (preX) {//如果上一把是*，则先把上一把的*对应的[^/]*添进来
                    sb.append("[^/]*");
                    preX = false;
                }
                if (chars[i] == '?') {//接着判断当前字符是不是?，是的话替换成.
                    sb.append('.');
                } else {//不是?的话，则就是普通字符，直接添进来
                    sb.append(chars[i]);
                }
            }
        }
        return sb.toString();
    }

    public boolean isMatch(String str) {
        if (pattern == null) {
            logger.error("请先初始化pattern");
            return false;
        }
        return pattern.matcher(str).matches();
    }

    private static final Logger logger = LoggerFactory.getLogger(RegexUtil.class);

    public static Pattern getPattern(Type type) {
        return Pattern.compile(type.regexString);
    }

    public static Pattern getPattern(String regexString) {
        return Pattern.compile(regexString);
    }


    /**
     * 判断指定字符串是否符合给定的类型
     *
     * @param str  要判断的字符串
     * @param type 类型
     * @return boolean
     * @author Huang Yongxiang
     * @date 2021/12/17 17:52
     */
    public static boolean isMatch(String str, Type type) {
        if (StringUtils.isEmpty(str) || type == null) {
            logger.error("正则表达判断失败，参数为空");
            return false;
        }
        return getPattern(type).matcher(str).matches();
    }

    public static boolean isMatch(String str, String regexString) {
        if (StringUtils.isEmpty(str) || StringUtils.isEmpty(regexString)) {
            logger.error("正则表达判断失败，参数为空");
            return false;
        }
        return getPattern(regexString).matcher(str).matches();
    }

    public static void main(String[] args) {
        String str = "**.job";
        String regexx = wildcardToRegexString(str);
        System.out.println(regexx);

        System.out.println(isMatch("com.example.autojob.job", regexx));
    }

}
