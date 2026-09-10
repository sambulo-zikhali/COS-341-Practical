package spl.output;

import spl.model.TreeNode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class XmlTreeWriter {
    public void write(TreeNode root, Map<Integer, TreeNode> nodesById, String outputPath) throws IOException {
        new TreeValidator().validate(root, nodesById);
        writeUnchecked(root, nodesById, outputPath);
    }
 
    public void writeUnchecked(TreeNode root, Map<Integer, TreeNode> nodesById, String outputPath) throws IOException {
        if (root == null) {
            throw new IllegalArgumentException("root node cannot be null");
        }
        Path path = Path.of(outputPath);
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
            writer.write("<tree>\n");
            writeNodesInOrder(root, nodesById, writer);
            writer.write("</tree>\n");
        }
    }
 
    private void writeNodesInOrder(TreeNode root, Map<Integer, TreeNode> nodesById, Writer writer) throws IOException {
        Set<Integer> visited = new HashSet<>();
        Deque<Integer> queue = new ArrayDeque<>();
        queue.add(root.id);
        visited.add(root.id);
 
        while (!queue.isEmpty()) {
            int id = queue.poll();
            TreeNode node = nodesById.get(id);
            if (node == null) {
                throw new IllegalStateException("Node id " + id + " referenced but missing from registry");
            }
            writeNode(node, writer);
            for (int childId : node.children) {
                if (visited.add(childId)) {
                    queue.add(childId);
                }
            }
        }
    }
 
    private void writeNode(TreeNode node, Writer writer) throws IOException {
        boolean isLeaf = node.children.isEmpty();
 
        writer.write("  <node id=\"" + node.id + "\"");
        if (node.parent != null) {
            writer.write(" parent=\"" + node.parent + "\"");
        }
        writer.write(">\n");
 
        writer.write("    <contents>" + escape(node.contents) + "</contents>\n");
 
        if (!isLeaf) {
            writer.write("    <children>\n");
            for (int childId : node.children) {
                writer.write("      <child>" + childId + "</child>\n");
            }
            writer.write("    </children>\n");
        }
 
        writer.write("  </node>\n");
    }
 
    private String escape(String text) {
        if (text == null) {
            return "";
        }
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;");
    }
}
