package com.jingge.autojob.skeleton.model.interpreter;

import com.jingge.autojob.skeleton.model.builder.AttributesBuilder;
import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.model.task.method.MethodTask;
import com.jingge.autojob.util.bean.ObjectUtil;
import com.jingge.autojob.util.convert.RegexUtil;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 参数构建环上下文
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/06 17:02
 */
@Slf4j
public class AutoJobAttributeContext {
    private final String attributeString;
    private Method targetMethod;
    private static final Pattern pattern = Pattern.compile("\\{(((\\d+)|((-?\\d+)(\\.\\d+))|(true|false)|('.*'))(,)?)" + "*}");

    public AutoJobAttributeContext(String attributeString) {
        this.attributeString = attributeString;
    }

    public AutoJobAttributeContext(AutoJobTask task) {
        this.attributeString = task.getParamsString();
        if (task instanceof MethodTask) {
            this.targetMethod = ObjectUtil.findMethod(((MethodTask) task).getMethodName(), ((MethodTask) task).getMethodClass());
        }
        if (isSimpleAttribute()) {
            task.setParamsString(convertSimple());
        }
    }

    public boolean isSimpleAttribute() {
        return pattern
                .matcher(attributeString)
                .matches();
    }

    private String convertSimple() {
        if (isSimpleAttribute()) {
            //去掉首尾花括号，分割参数
            String[] attributes = attributeString
                    .trim()
                    .substring(1, attributeString.length() - 1)
                    .split(",");
            AttributesBuilder attributesBuilder = AttributesBuilder.getInstance();
            attributesBuilder.clear();
            for (String params : attributes) {
                String trimParams = params.trim();
                if (RegexUtil.isMatch(trimParams, "^\\d+$")) {
                    long value = Long.parseLong(trimParams);
                    if (value < Integer.MAX_VALUE) {
                        attributesBuilder.addParams(AttributesBuilder.AttributesType.INTEGER, (int) value);
                    } else {
                        attributesBuilder.addParams(AttributesBuilder.AttributesType.LONG, value);
                    }
                } else if (RegexUtil.isMatch(trimParams, "(-?\\d+)(\\.\\d+)")) {
                    BigDecimal value = new BigDecimal(trimParams);
                    attributesBuilder.addParams(AttributesBuilder.AttributesType.DECIMAL, value.doubleValue());
                } else if (RegexUtil.isMatch(trimParams, "(^true$|^false$)")) {
                    attributesBuilder.addParams(AttributesBuilder.AttributesType.BOOLEAN, Boolean.valueOf(trimParams));
                } else if (RegexUtil.isMatch(trimParams, "^\\'.*\\'$")) {
                    attributesBuilder.addParams(AttributesBuilder.AttributesType.STRING, trimParams.substring(1, trimParams.length() - 1));
                }
            }
            return attributesBuilder.getAttributesString();
        }
        return attributeString;
    }

    public List<Attribute> convert() {
        try {
            if (targetMethod != null) {
                return InterpreterDelegate.convertAttributeString(targetMethod, isSimpleAttribute() ? convertSimple() : attributeString);
            } else {
                return InterpreterDelegate.convertAttributeString(isSimpleAttribute() ? convertSimple() : attributeString);
            }
        } catch (Exception e) {
            e.printStackTrace();
            log.error("参数转化失败：{}", e.getMessage());
        }
        return Collections.emptyList();
    }

    /**
     * 获取参数对象实体
     *
     * @return java.lang.Object[]
     * @author Huang Yongxiang
     * @date 2022/8/19 12:49
     */
    public Object[] getAttributeEntity() {
        List<Attribute> attributeList = convert();
        return attributeList
                .stream()
                .map(Attribute::getValue)
                .collect(Collectors.toList())
                .toArray(new Object[]{});
    }


}
