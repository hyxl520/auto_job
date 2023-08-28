package com.jingge.autojob.skeleton.model.interpreter;

/**
 * @Description 终结符表达式
 * @Author Huang Yongxiang
 * @Date 2022/07/06 15:14
 */
public abstract class AbstractInterpreter implements IExpression{
    protected IExpression type;
    protected String valueString;
    protected int pos;

    public AbstractInterpreter(IExpression type, String valueString, int pos) {
        this.type = type;
        this.valueString = valueString;
        this.pos = pos;
    }
}
