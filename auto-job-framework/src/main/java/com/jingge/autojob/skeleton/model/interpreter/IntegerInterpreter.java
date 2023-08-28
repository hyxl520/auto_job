package com.jingge.autojob.skeleton.model.interpreter;

/**
 * @Description 整数参数解释器
 * @Author Huang Yongxiang
 * @Date 2022/07/06 15:22
 */
public class IntegerInterpreter extends AbstractInterpreter {

    public IntegerInterpreter(IExpression type, String valueString, int pos) {
        super(type, valueString, pos);
    }

    @Override
    public Object interpreter() {
        SupportAttributeType attributeType = SupportAttributeType.convert(type.interpreter());
        if (attributeType != SupportAttributeType.INTEGER && attributeType != SupportAttributeType.LONG) {
            return null;
        }
        Object valueObject;
        if (attributeType == SupportAttributeType.LONG) {
            valueObject = Long.valueOf(valueString);
        } else {
            valueObject = Integer.valueOf(valueString);
        }
        return new Attribute((Class<?>) type.interpreter(), valueObject, pos);
    }
}
