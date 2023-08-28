package com.jingge.autojob.skeleton.db;

import java.util.List;

/**
 * 存储节点
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 17:30
 * @email 1158055613@qq.com
 */
public interface StorageNode<T> {
    int store(List<T> entities);
}
