package com.livingframe.common.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * 通用树构建工具。
 */
public final class TreeUtils {

    private TreeUtils() {
    }

    /**
     * 将扁平列表组装为树。
     *
     * @param nodes          已按排序字段排好序的节点
     * @param idGetter       节点 ID
     * @param parentGetter   父节点 ID
     * @param childrenSetter 子节点设置器
     * @param rootParent     根节点的父 ID（通常是 0L）
     */
    public static <T> List<T> build(List<T> nodes,
                                    Function<T, Long> idGetter,
                                    Function<T, Long> parentGetter,
                                    BiConsumer<T, List<T>> childrenSetter,
                                    Long rootParent) {
        List<T> roots = new ArrayList<>();
        if (nodes == null || nodes.isEmpty()) {
            return roots;
        }
        Map<Long, T> nodeMap = new HashMap<>(nodes.size());
        Map<Long, List<T>> childrenMap = new HashMap<>(nodes.size());
        for (T node : nodes) {
            Long id = idGetter.apply(node);
            nodeMap.put(id, node);
            childrenMap.put(id, new ArrayList<>());
        }
        for (T node : nodes) {
            Long parentId = parentGetter.apply(node);
            if (parentId == null || Objects.equals(parentId, rootParent) || !nodeMap.containsKey(parentId)) {
                roots.add(node);
            } else {
                childrenMap.get(parentId).add(node);
            }
        }
        for (T node : nodes) {
            childrenSetter.accept(node, childrenMap.get(idGetter.apply(node)));
        }
        return roots;
    }
}
