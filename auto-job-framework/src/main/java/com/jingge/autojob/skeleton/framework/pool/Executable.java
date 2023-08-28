package com.jingge.autojob.skeleton.framework.pool;

/**
 * 声明该接口的类是一个能被执行器池执行的类
 *
 * @Author Huang Yongxiang
 * @Date 2022/08/02 11:01
 */
public interface Executable {

    /**
     * 执行的主方法
     *
     * @param params 参数
     * @return java.lang.Object
     * @author Huang Yongxiang
     * @date 2022/8/2 17:16
     */
    Object execute(Object... params) throws Exception;

    /**
     * 执行的参数
     *
     * @return java.lang.Object[]
     * @author Huang Yongxiang
     * @date 2022/8/5 11:19
     */
    Object[] getExecuteParams();

}
