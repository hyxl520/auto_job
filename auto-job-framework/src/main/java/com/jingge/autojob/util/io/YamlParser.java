package com.jingge.autojob.util.io;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.InputStream;
import java.util.*;

/**
 * Yaml文件解析类
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/17 14:12
 */
public class YamlParser {
    private static final Logger logger = LoggerFactory.getLogger(YamlParser.class);

    /**
     * yml文件流转成单层map，用于转properties
     *
     * @param yamlContent yaml文件内容
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Huang Yongxiang
     * @date 2022/8/17 14:19
     */
    public static Map<String, Object> yaml2FlattenedMap(String yamlContent) {
        Yaml yaml = createYaml();
        Map<String, Object> map = new HashMap<>();
        for (Object object : yaml.loadAll(yamlContent)) {
            if (object != null) {
                map = asMap(object);
                map = getFlattenedMap(map);
            }
        }
        return map;
    }

    public static Map<String, Object> classPathYaml2FlattenedMap(String yamlClassPath) {
        return classPathYaml2FlattenedMap(getClassPathResourceInputStream(yamlClassPath));
    }

    public static Map<String, Object> classPathYaml2FlattenedMap(InputStream inputStream) {
        Yaml yaml = createYaml();
        Map<String, Object> map = new HashMap<>();
        for (Object object : yaml.loadAll(inputStream)) {
            if (object != null) {
                map = asMap(object);
                map = getFlattenedMap(map);
            }
        }
        return map;
    }

    private static InputStream getClassPathResourceInputStream(String fileName) {
        return YamlParser.class
                .getClassLoader()
                .getResourceAsStream(fileName);
    }


    /**
     * yml文件流转成多次嵌套map
     *
     * @param yamlContent yaml文件内容
     * @return java.util.Map<java.lang.String, java.lang.Object>
     * @author Huang Yongxiang
     * @date 2022/8/17 14:19
     */
    public static Map<String, Object> yaml2MultilayerMap(String yamlContent) {
        Yaml yaml = createYaml();
        Map<String, Object> result = new LinkedHashMap<>();
        for (Object object : yaml.loadAll(yamlContent)) {
            if (object != null) {
                result.putAll(asMap(object));
            }
        }
        return result;
    }


    private static Yaml createYaml() {
        return new Yaml(new Constructor());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object object) {
        Map<String, Object> result = new LinkedHashMap<>();
        if (!(object instanceof Map)) {
            result.put("document", object);
            return result;
        }
        Map<Object, Object> map = (Map<Object, Object>) object;
        for (Map.Entry<Object, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                value = asMap(value);
            }
            Object key = entry.getKey();
            if (key instanceof CharSequence) {
                result.put(key.toString(), value);
            } else {
                result.put("[" + key.toString() + "]", value);
            }
        }
        return result;
    }

    private static Map<String, Object> getFlattenedMap(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        buildFlattenedMap(result, source, null);
        return result;
    }

    private static void buildFlattenedMap(Map<String, Object> result, Map<String, Object> source, String path) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            String key = entry.getKey();
            if (!StringUtils.isBlank(path)) {
                if (key.startsWith("[")) {
                    key = path + key;
                } else {
                    key = path + '.' + key;
                }
            }
            Object value = entry.getValue();
            if (value instanceof String) {
                result.put(key, value);
            } else if (value instanceof Map) {
                @SuppressWarnings("unchecked") Map<String, Object> map = (Map<String, Object>) value;
                buildFlattenedMap(result, map, key);
            } else if (value instanceof Collection) {
                @SuppressWarnings("unchecked") Collection<Object> collection = (Collection<Object>) value;
                int count = 0;
                for (Object object : collection) {
                    buildFlattenedMap(result, Collections.singletonMap("[" + (count++) + "]", object), key);
                }
            } else {
                result.put(key, (value != null ? value.toString() : ""));
            }
        }
    }

    public static void main(String[] args) {
        Map<String, Object> result = classPathYaml2FlattenedMap("auto-job.yml");
        System.out.println(result);
    }


}
