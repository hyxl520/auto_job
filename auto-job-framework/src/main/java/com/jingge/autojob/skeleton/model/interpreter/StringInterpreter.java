package com.jingge.autojob.skeleton.model.interpreter;

/**
 * @Description 字符串参数解释器
 * @Author Huang Yongxiang
 * @Date 2022/07/06 15:47
 */
public class StringInterpreter extends AbstractInterpreter{


    public StringInterpreter(IExpression type, String valueString, int pos) {
        super(type, valueString, pos);
    }

    @Override
    public Object interpreter() {
        if (SupportAttributeType.convert(type.interpreter()) == SupportAttributeType.STRING) {
            return new Attribute((Class<?>) type.interpreter(), valueString, pos);
        }
        return null;
    }
}
