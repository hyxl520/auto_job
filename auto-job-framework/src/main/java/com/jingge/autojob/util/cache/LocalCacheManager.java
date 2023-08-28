package com.jingge.autojob.util.cache;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * 本地缓存管理器
 *
 * @Auther Huang Yongxiang
 * @Date 2022/02/16 10:47
 */
public class LocalCacheManager<K, V> {

    private ExpiringMap<K, V> localCache;


    private LocalCacheManager() {
    }

    /**
     * 按照默认策略创建一个构建工厂对象，过期时间：30min，最大长度：100，过期策略：CREATED，是否允许设置单数据过期时间：true
     *
     * @return com.example.ammetermodel.cache.Builder
     * @author Huang Yongxiang
     * @date 2022/2/16 11:09
     */
    public static Builder<Object, Object> builder() {
        Builder<Object, Object> builder = new Builder<>();
        builder.setExpiringTime(5 * 60 * 1000);
        builder.setMaxLength(100);
        builder.setPolicy(ExpirationPolicy.CREATED);
        return builder;
    }

    /**
     * 添加一个同步的过期监听器
     *
     * @param listener 监听器
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/9/23 9:40
     */
    public boolean addSyncExpirationListener(ExpirationListener<K, V> listener) {
        try {
            localCache.addExpirationListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 添加一个异步过期监听器，该方法不会影响条目过期，监听器的处理将放在一个异步线程里执行
     *
     * @param listener 监听器
     * @return boolean
     * @author Huang Yongxiang
     * @date 2022/9/23 9:41
     */
    public boolean addAsyncExpirationListener(ExpirationListener<K, V> listener) {
        try {
            localCache.addAsyncExpirationListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public boolean set(K key, V value) {
        try {
            localCache.put(key, value);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public long getExpiringTime(K key) {
        return localCache.getExpiration(key);
    }

    public boolean set(K key, V value, long expiringTime, TimeUnit unit) {
        try {
            localCache.put(key, value, expiringTime, unit);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean refreshExpiringTime(K key, long expiringTime, TimeUnit unit) {
        try {
            localCache.setExpiration(key, expiringTime, unit);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public boolean refreshExpiringTime(long expiringTime, TimeUnit unit) {
        try {
            localCache.setExpiration(expiringTime, unit);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    /**
     * 获取值为字符串
     *
     * @param key 键
     * @return java.lang.String 如果不存在返回null
     * @author Huang Yongxiang
     * @date 2022/1/21 12:23
     */
    public String getAsString(K key) {
        String value = String.valueOf(localCache.get(key));
        return "null".equals(value) ? null : value;
    }

    public V get(K key) {
        return localCache.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getAsClassType(K key, Class<T> clazz) {
        try {
            return (T) localCache.get(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean exist(K key) {
        return localCache.containsKey(key);
    }


    public Object getAsObject(K key) {
        try {
            return localCache.get(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public boolean remove(K key) {
        try {
            localCache.remove(key);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public int size() {
        return localCache.size();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return localCache.entrySet();
    }

    public List<V> values() {
        return new ArrayList<>(localCache.values());
    }

    @Setter
    @Accessors(chain = true)
    public static class Builder<K, V> {
        /**
         * 最大长度
         */
        private int maxLength;
        /**
         * 过期时间，毫秒
         */
        private long expiringTime;
        /**
         * 过期策略
         */
        private ExpirationPolicy policy;
        /**
         * 是否允许设置单条数据的过期时间
         */
        private boolean isEntriesExpiration;
        /**
         * 同步过期监听器
         */
        private List<ExpirationListener<? super K, ? super V>> syncListeners = new ArrayList<>();
        /**
         * 异步过期监听器
         */
        private List<ExpirationListener<? super K, ? super V>> asyncListeners = new ArrayList<>();

        public Builder<K, V> setExpiringTime(long expiringTime, TimeUnit unit) {
            this.expiringTime = unit.toMillis(expiringTime);
            return this;
        }

        public Builder<K, V> addSyncExpirationListener(ExpirationListener<K, V> listener) {
            syncListeners.add(listener);
            return this;
        }

        public Builder<K, V> addAsyncExpirationListener(ExpirationListener<K, V> listener) {
            asyncListeners.add(listener);
            return this;
        }

        public <K1 extends K, V1 extends V> LocalCacheManager<K1, V1> build() {
            LocalCacheManager<K1, V1> cacheManager = new LocalCacheManager<>();
            cacheManager.localCache = ExpiringMap
                    .builder()
                    .expiration(expiringTime, TimeUnit.MILLISECONDS)
                    .maxSize(maxLength)
                    .variableExpiration()
                    .expirationPolicy(policy)
                    .asyncExpirationListeners(asyncListeners)
                    .expirationListeners(syncListeners)
                    .build();
            return cacheManager;
        }

    }
}
