package com.livingframe.common.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class TreeUtilsTest {

    static class Node {
        Long id;
        Long parentId;
        String name;
        List<Node> children = new ArrayList<>();

        Node(Long id, Long parentId, String name) {
            this.id = id;
            this.parentId = parentId;
            this.name = name;
        }
    }

    @Test
    void shouldBuildTreeAndSkipMissingParents() {
        List<Node> nodes = new ArrayList<>();
        nodes.add(new Node(1L, 0L, "root"));
        nodes.add(new Node(2L, 1L, "child-a"));
        nodes.add(new Node(3L, 1L, "child-b"));
        nodes.add(new Node(4L, 99L, "orphan"));

        List<Node> roots = TreeUtils.build(nodes, n -> n.id, n -> n.parentId, (n, c) -> n.children = c, 0L);

        assertThat(roots).extracting(n -> n.name).containsExactly("root", "orphan");
        assertThat(roots.get(0).children).extracting(n -> n.name).containsExactly("child-a", "child-b");
    }

    @Test
    void shouldReturnEmptyForEmptyInput() {
        assertThat(TreeUtils.build(List.<Node>of(), n -> n.id, n -> n.parentId, (n, c) -> n.children = c, 0L))
                .isEmpty();
    }
}
