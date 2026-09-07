package spl.model;

import java.util.ArrayList;
import java.util.List;

// one node of the syntax tree
public class TreeNode {
    public final int id;
    public String contents;
    public final List<Integer> children = new ArrayList<>();
    public Integer parent;

    public TreeNode(int id, String contents) {
        this.id = id;
        this.contents = contents;
    }
}
