package com.jingge.autojob.skeleton.model.interpreter;

/**
 * @Description 小数型解释器
 * @Author Huang Yongxiang
 * @Date 2022/07/06 15:25
 */
public class DecimalInterpreter extends AbstractInterpreter {


    public DecimalInterpreter(IExpression type, String valueString, int pos) {
        super(type, valueString, pos);
    }

    @Override
    public Object interpreter() {
        if (SupportAttributeType.convert(type.interpreter()) == SupportAttributeType.DECIMAL) {
            return new Attribute((Class<?>) type.interpreter(), Double.valueOf(valueString), pos);
        }
        return null;
    }
}
