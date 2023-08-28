package com.jingge.autojob.skeleton.model.interpreter;

/**
 * @Description
 * @Author Huang Yongxiang
 * @Date 2022/07/06 15:47
 */
public class BooleanInterpreter extends AbstractInterpreter {


    public BooleanInterpreter(IExpression type, String valueString, int pos) {
        super(type, valueString, pos);
    }

    @Override
    public Object interpreter() {
        if (SupportAttributeType.convert(type.interpreter()) == SupportAttributeType.BOOLEAN) {
            return new Attribute((Class<?>) type.interpreter(), Boolean.valueOf(valueString), pos);
        }
        return null;
    }
}
