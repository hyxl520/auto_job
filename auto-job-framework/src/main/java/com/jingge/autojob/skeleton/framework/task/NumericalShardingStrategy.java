package com.jingge.autojob.skeleton.framework.task;

import com.jingge.autojob.skeleton.cluster.model.ClusterNode;
import com.jingge.autojob.skeleton.framework.config.AutoJobShardingConfig;

import java.util.*;

/**
 * 基于整形数值的分片策略
 *
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-07-28 16:26
 * @email 1158055613@qq.com
 */
public class NumericalShardingStrategy implements ShardingStrategy {
    @Override
    public boolean isAvailable(AutoJobShardingConfig config) {
        return config != null && config.isEnable() && config.getTotal() != null && config.getTotal() instanceof Number;
    }

    @Override
    public Map<ClusterNode, Object> executionSharding(AutoJobShardingConfig config, List<ClusterNode> clusterNodes) {
        int total = ((Number) config.getTotal()).intValue();
        Map<ClusterNode, Object> res = new HashMap<>();
        for (int i = 1; i <= clusterNodes.size(); i++) {
            List<Integer> per = new ArrayList<>();
            if (i <= total) {
                per.add(i);
            }
            int num = i;
            do {
                num += clusterNodes.size();
                if (num > total) {
                    break;
                }
                per.add(num);
            } while (true);
            res.put(clusterNodes.get(i - 1), per);
        }
        return res;
    }


}
