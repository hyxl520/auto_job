package com.jingge.autojob.skeleton.db;

import com.jingge.autojob.skeleton.db.mapper.AutoJobMapperHolder;
import com.jingge.autojob.skeleton.db.mapper.TransactionEntry;

import java.sql.Connection;
import java.util.LinkedList;
import java.util.List;

/**
 * 储存链
 *
 * @author Huang Yongxiang
 * @date 2023-01-04 17:33
 * @email 1158055613@qq.com
 */
public class StorageChain<T> {
    private final List<Object> chain = new LinkedList<>();

    private StorageChain() {
    }

    public boolean store(List<T> entities) {
        return store(entities, Connection.TRANSACTION_REPEATABLE_READ);
    }

    @SuppressWarnings("unchecked")
    public boolean store(List<T> entities, int level) {
        return AutoJobMapperHolder.METHOD_TASK_ENTITY_MAPPER.doTransaction(chain
                .stream()
                .map(obj -> {
                    StorageNode<T> node = (StorageNode<T>) obj;
                    return (TransactionEntry) connection -> node.store(entities);
                })
                .toArray(TransactionEntry[]::new), level);
    }

    public static class Builder<T> {
        List<Object> chain = new LinkedList<>();

        public <T1 extends T> Builder<T> addNode(StorageNode<T1> node) {
            chain.add(node);
            return this;
        }

        public <T1 extends T> StorageChain<T1> build() {
            StorageChain<T1> storageChain = new StorageChain<>();
            storageChain.chain.addAll(this.chain);
            return storageChain;
        }
    }
}
