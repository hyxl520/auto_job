package com.jingge.autojob.skeleton.db.entity;

import com.jingge.autojob.skeleton.framework.task.AutoJobTask;
import com.jingge.autojob.skeleton.lang.Convertor;

/**
 * @author JingGe(* ^ ▽ ^ *)
 * @date 2023-03-15 16:02
 * @email 1158055613@qq.com
 */
public interface Entity2TaskConvertor<T extends AutoJobTaskBaseEntity> extends Convertor<AutoJobTask,T> {
}
