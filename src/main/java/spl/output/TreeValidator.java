package spl.output;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import spl.model.TreeNode;

public class TreeValidator {
    public void validate(TreeNode root, Map<Integer, TreeNode> nodesById) {
        List<String> issues = new ArrayList<>();
 
        if (root == null) {
            issues.add("root node is null");
            throw new TreeValidationException(issues);
        }
        if (nodesById == null) {
            issues.add("nodesById registry is null");
            throw new TreeValidationException(issues);
        }
 
        checkRoot(root, issues);
        checkRegistryConsistency(nodesById, issues);
        checkParentChildLinks(nodesById, issues);
        checkReachabilityAndCycles(root, nodesById, issues);
        checkContents(nodesById, issues);
 
        if (!issues.isEmpty()) {
            throw new TreeValidationException(issues);
        }
    }
 
    private void checkRoot(TreeNode root, List<String> issues) {
        if (root.parent != null) {
            issues.add("root node (id=" + root.id + ") has a non-null parent (" + root.parent + ")");
        }
    }
 
    private void checkRegistryConsistency(Map<Integer, TreeNode> nodesById, List<String> issues) {
        for (Map.Entry<Integer, TreeNode> entry : nodesById.entrySet()) {
            TreeNode node = entry.getValue();
            if (node == null) {
                issues.add("registry contains a null node at id " + entry.getKey());
                continue;
            }
            if (node.id != entry.getKey()) {
                issues.add("registry key " + entry.getKey() + " does not match node.id " + node.id);
            }
        }
    }
 
    private void checkParentChildLinks(Map<Integer, TreeNode> nodesById, List<String> issues) {
        for (TreeNode node : nodesById.values()) {
            if (node == null) continue;
 
            for (int childId : node.children) {
                TreeNode child = nodesById.get(childId);
                if (child == null) {
                    issues.add("node " + node.id + " references missing child id " + childId);
                    continue;
                }
                if (child.parent == null || child.parent != node.id) {
                    issues.add("node " + childId + " is listed as a child of " + node.id
                            + " but its own parent field says " + child.parent);
                }
            }
 
            if (node.parent != null && !nodesById.containsKey(node.parent)) {
                issues.add("node " + node.id + " has parent " + node.parent + " which does not exist in the registry");
            }
        }
    }
 
    private void checkReachabilityAndCycles(TreeNode root, Map<Integer, TreeNode> nodesById, List<String> issues) {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> stack = new ArrayDeque<>();
        stack.push(root.id);
 
        while (!stack.isEmpty()) {
            int id = stack.pop();
            if (!visited.add(id)) {
                issues.add("cycle detected: node " + id + " is reachable more than once from root");
                continue;
            }
            TreeNode node = nodesById.get(id);
            if (node == null) {
                issues.add("node " + id + " is referenced as a child but missing from registry");
                continue;
            }
            for (int childId : node.children) {
                stack.push(childId);
            }
        }
 
        for (Integer id : nodesById.keySet()) {
            if (!visited.contains(id)) {
                issues.add("node " + id + " exists in the registry but is unreachable from root (orphan)");
            }
        }
    }
 
    private void checkContents(Map<Integer, TreeNode> nodesById, List<String> issues) {
        for (TreeNode node : nodesById.values()) {
            if (node == null) continue;
            if (node.contents == null || node.contents.isEmpty()) {
                issues.add("node " + node.id + " has null/empty contents");
            }
        }
    }
}
