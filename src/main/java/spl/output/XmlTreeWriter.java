package spl.output;

import spl.grammar.Grammar;
import spl.model.TreeNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Serializes a parsed syntax tree to tree.xml, per spec:
 *
 * Root node:   unique id, <contents> = start symbol, <children>, NO <parent>
 * Inner node:  unique id, <contents> = non-terminal name, <children>, <parent>
 * Leaf node:   unique id, <contents> = terminal token text, <parent>, NO <children>
 *
 * A node is classified as a leaf if its contents does NOT match a grammar
 * non-terminal (i.e. it's literal token text from a SHIFT); otherwise it's
 * an inner node (from a REDUCE) or the root (parent == null).
 */
public class XmlTreeWriter {

    private final Set<String> nonTerminals;

    public XmlTreeWriter(Grammar grammar) {
        this.nonTerminals = grammar.getNonTerminals();
    }

    /**
     * Writes the full syntax tree rooted at {@code root} to {@code outputPath}.
     * {@code nodesById} must contain every node reachable from root (as returned
     * by Parser.getNodesById()).
     */
    public void write(TreeNode root, Map<Integer, TreeNode> nodesById, Path outputPath) throws IOException {
        StringBuilder xml = new StringBuilder();
        xml.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        xml.append("<SYNTREE>\n");
        writeNode(root, nodesById, xml, true);
        xml.append("</SYNTREE>\n");

        Files.writeString(outputPath, xml.toString());
    }

    private void writeNode(TreeNode node, Map<Integer, TreeNode> nodesById, StringBuilder xml, boolean isRoot) {
        boolean isLeaf = !nonTerminals.contains(node.contents);

        xml.append("  <NODE>\n");
        xml.append("    <id>").append(node.id).append("</id>\n");
        xml.append("    <contents>").append(escape(node.contents)).append("</contents>\n");

        if (!isLeaf) {
            xml.append("    <children>\n");
            for (int childId : node.children) {
                xml.append("      <child>").append(childId).append("</child>\n");
            }
            xml.append("    </children>\n");
        }

        if (!isRoot) {
            xml.append("    <parent>").append(node.parent).append("</parent>\n");
        }

        xml.append("  </NODE>\n");

        // Recurse into children (order doesn't matter for correctness, just readability)
        if (!isLeaf) {
            for (int childId : node.children) {
                TreeNode child = nodesById.get(childId);
                if (child == null) {
                    throw new IllegalStateException("XmlTreeWriter: missing node for child id " + childId);
                }
                writeNode(child, nodesById, xml, false);
            }
        }
    }

    private String escape(String text) {
        return text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;");
    }
}