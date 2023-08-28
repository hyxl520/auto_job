package com.jingge.autojob.skeleton.model.interpreter;

import com.jingge.autojob.util.bean.ObjectUtil;

/**
 * @Description 类型解释器
 * @Author Huang Yongxiang
 * @Date 2022/07/06 16:17
 */
public class TypeInterpreter implements IExpression {
    private final String type;

    public TypeInterpreter(String type) {
        this.type = type;
    }

    @Override
    public Object interpreter() {
        switch (type) {
            case "string": {
                return String.class;
            }
            case "decimal": {
                return Double.class;
            }
            case "integer": {
                return Integer.class;
            }
            case "long": {
                return Long.class;
            }
            case "boolean": {
                return Boolean.class;
            }
            default: {
                return ObjectUtil.classPath2Class(type);
            }
        }
    }
}
