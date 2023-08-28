package com.jingge.autojob.util.io;


import com.jingge.autojob.util.convert.DateUtils;
import com.jingge.autojob.util.json.JsonUtil;
import com.jingge.autojob.util.convert.StringUtils;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Slf4j
public class PropertiesHolder {
    private Properties properties;
    /**
     * 配置文件路径，仅支持classpath下的资源文件加载
     */
    private final List<String> propertiesPath = new ArrayList<>();

    private final List<InputStream> others = new ArrayList<>();

    private String[] args;

    private boolean ignoreSystemProperties;


    @Setter
    @Accessors(chain = true)
    public static class Builder {
        /**
         * 配置文件路径，仅支持classpath下的资源文件加载
         */
        private List<String> propertiesPath = new ArrayList<>();

        private List<InputStream> others = new ArrayList<>();

        private String[] args;

        private boolean ignoreSystemProperties;

        public Builder addPropertiesFile(String pattern) {
            propertiesPath.add(pattern);
            return this;
        }

        public Builder addAllPropertiesFile(List<String> patterns) {
            propertiesPath.addAll(patterns);
            return this;
        }

        public Builder addOtherInputStream(InputStream inputStream) {
            if (inputStream != null) {
                others.add(inputStream);
            }
            return this;
        }

        public Builder addOthers(List<InputStream> others) {
            if (others != null) {
                this.others.addAll(others);
            }
            return this;
        }

        public Builder setArgs(String[] args) {
            this.args = args;
            return this;
        }

        public Builder setIgnoreSystemProperties(boolean ignoreSystemProperties) {
            this.ignoreSystemProperties = ignoreSystemProperties;
            return this;
        }

        public PropertiesHolder build() {
            PropertiesHolder propertiesHolder = new PropertiesHolder(ignoreSystemProperties, args, others, propertiesPath.toArray(new String[]{}));
            propertiesHolder.propertiesPath.addAll(propertiesPath);
            propertiesHolder.args = args;
            propertiesHolder.others.addAll(others);
            propertiesHolder.ignoreSystemProperties = ignoreSystemProperties;
            return propertiesHolder;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public void reload() {
        properties = load(ignoreSystemProperties, args, others, propertiesPath.toArray(new String[]{}));
    }


    private PropertiesHolder(boolean ignoreSystemProperties, String[] args, List<InputStream> others, String... configFiles) {
        this.properties = load(ignoreSystemProperties, args, others, configFiles);
    }

    private static Properties load(boolean ignoreSystemProperties, String[] args, List<InputStream> others, String... configFiles) {
        Properties properties = new Properties();

        //从配置文件load
        for (String location : configFiles) {
            try {
                InputStream is = getClassPathResourceInputStream(location);
                if (is == null) {
                    //log.warn("没有找到资源文件：{}", location);
                    continue;
                }
                try {
                    //log.info("加载资源文件：{}", location);
                    if (location.endsWith(".properties")) {
                        properties.load(is);
                    } else if (location.endsWith(".yml")) {
                        Map<String, Object> content = YamlParser.classPathYaml2FlattenedMap(location);
                        for (Map.Entry<String, Object> entry : content.entrySet()) {
                            properties.setProperty(entry.getKey(), entry.getValue() + "");
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    IOUtils.closeQuietly(is);
                }
            } catch (Exception e) {
                log.error("Load " + location + " failure. ", e);
            }
            // 存储当前加载的配置文件路径和名称
            properties.setProperty("configFiles", StringUtils.join(configFiles, ","));

        }

        //从其他配置文件load
        for (InputStream in : others) {
            if (in == null) {
                continue;
            }
            try {
                try {
                    properties.load(in);
                } catch (IllegalArgumentException ex) {
                    Map<String, Object> content = YamlParser.classPathYaml2FlattenedMap(in);
                    for (Map.Entry<String, Object> entry : content.entrySet()) {
                        properties.setProperty(entry.getKey(), entry.getValue() + "");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                IOUtils.closeQuietly(in);
            }
        }

        if (!ignoreSystemProperties) {
            //从系统变量load
            Properties system = System.getProperties();
            Enumeration<?> enumeration = system.propertyNames();
            while (enumeration.hasMoreElements()) {
                String key = (String) enumeration.nextElement();
                properties.put(key, system.getProperty(key));
            }
        }

        //从启动参数获取
        if (args != null && args.length > 0) {
            for (String arg : args) {
                String param = arg;
                if (StringUtils.isEmpty(param)) {
                    continue;
                }
                param = param.replace("-", "");
                String[] params = param.split("=");
                if (params.length == 2) {
                    properties.put(params[0], params[1]);
                } else {
                    properties.put(params[0], "");
                }
            }
        }

        //load完成替换嵌套取值
        Enumeration<?> enumeration2 = properties.propertyNames();
        while (enumeration2.hasMoreElements()) {
            String key = (String) enumeration2.nextElement();
            String value = properties.getProperty(key);
            if (StringUtils.isEmpty(value)) {
                continue;
            }
            properties.put(key, replace(properties, value));

        }
        return properties;
    }

    private static InputStream getClassPathResourceInputStream(String fileName) {
        return PropertiesHolder.class
                .getClassLoader()
                .getResourceAsStream(fileName);
    }

    public Properties getProperties() {
        return properties;
    }

    private static final Pattern p1 = Pattern.compile("\\$\\{.*?\\}");
    private static final Pattern p2 = Pattern.compile(".*\\$\\{.*\\}.*");

    public String getNestingKey(String key) {
        //处理嵌套key
        Matcher matcher = p2.matcher(key);
        if (matcher.find()) {
            return replace(properties, key);
        } else {
            return key;
        }
    }

    public String getProperty(String key) {
        key = getNestingKey(key);
        String value = properties.getProperty(key);
        if (value != null) {
            // 支持嵌套取值的问题 key=${xx}/yy
            Matcher m = p1.matcher(value);
            while (m.find()) {
                String g = m.group();
                String keyChild = g
                        .replaceAll("\\$\\{", "")
                        .replaceAll("\\}", "");
                value = value.replace(g, getProperty(keyChild));
            }
            return value;
        } else {
            return System.getProperty(key);
        }
    }

    private static String replace(Properties properties, String value) {
        if (StringUtils.isEmpty(value)) {
            return "";
        }
        Matcher m = p1.matcher(value);
        while (m.find()) {
            String g = m.group();
            String keyChild = g
                    .replaceAll("\\$\\{", "")
                    .replaceAll("\\}", "");
            value = value.replace(g, replace(properties, properties.getProperty(keyChild)));
        }
        return value;
    }


    public String getProperty(String key, String defaultValue) {
        key = getNestingKey(key);
        String value = getProperty(key);
        return value != null ? value : defaultValue;
    }

    public <T> T getProperty(String key, Class<T> clazz, String defaultValue) {
        key = getNestingKey(key);
        T value = getProperty(key, clazz);
        return value == null ? parseStringValue(defaultValue, clazz) : value;
    }

    @SuppressWarnings("unchecked")
    public <T> T getProperty(String key, Class<T> clazz) {
        key = getNestingKey(key);
        if (clazz == Map.class) {
            Map<String, String> res = new HashMap<>();
            for (int i = 0; ; i++) {
                String value = getProperty(String.format("%s[%d]", key, i));
                if (StringUtils.isEmpty(value)) {
                    break;
                }
                String[] entry = value.split(",");
                if (entry.length > 0 && !StringUtils.isEmpty(entry[0])) {
                    res.put(entry[0], entry[1]);
                }
            }
            return (T) res;
        }
        return parseStringValue(properties.getProperty(key, ""), clazz);
    }

    @SuppressWarnings("unchecked")
    private static <T> T parseStringValue(String value, Class<T> type) {
        try {
            if (type == Boolean.class) {
                if (StringUtils.isEmpty(value)) {
                    return null;
                }
                return (T) Boolean.valueOf(value);
            } else if (type == Integer.class) {
                return (T) Integer.valueOf(value);
            } else if (type == Double.class) {
                return (T) Double.valueOf(value);
            } else if (type == Long.class) {
                return (T) Long.valueOf(value);
            } else if (type == List.class) {
                return (T) Arrays.asList(value.split(","));
            } else if (type == String.class) {
                return (T) value;
            } else if (type == BigDecimal.class) {
                return (T) new BigDecimal(value);
            } else if (type == Date.class) {
                return (T) DateUtils.parseDate(value, "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyyMMdd");
            }
        } catch (Exception ignored) {
            return null;
        }
        return JsonUtil.jsonStringToPojo(value, type);
    }


}