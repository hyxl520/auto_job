package com.jingge.autojob.skeleton.model.interpreter;

import com.jingge.autojob.util.json.JsonUtil;

/**
 * 对象参数解释器
 *
 * @Author Huang Yongxiang
 * @Date 2022/07/06 16:12
 */
public class ObjectInterpreter extends AbstractInterpreter {

    public ObjectInterpreter(IExpression type, String valueString, int pos) {
        super(type, valueString, pos);
    }

    @Override
    public Object interpreter() {
        if (SupportAttributeType.convert(type.interpreter()) == SupportAttributeType.OBJECT) {
            return new Attribute((Class<?>) type.interpreter(), JsonUtil.jsonStringToPojo(valueString, (Class<?>) type.interpreter()), pos);
        }
        return null;
    }
}
