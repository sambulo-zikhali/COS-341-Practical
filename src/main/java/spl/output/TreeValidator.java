package spl.output;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Validates a tree.xml file against the four structural rules from the
 * SPL spec:
 *   - every node has a unique id
 *   - the root has contents + children, and NO parent
 *   - every inner node has contents + children + parent
 *   - every leaf node has contents + parent, and NO children
 *   - every id referenced in a <children> list actually exists
 *   - every id referenced as <parent> actually exists
 *
 * Usage: TreeValidator.validate(Path.of("tree.xml")) — throws
 * IllegalStateException with a descriptive message on the first
 * violation found, or returns silently if the file is valid.
 */
public class TreeValidator {

    public static void validate(File xmlFile) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document doc = builder.parse(xmlFile);
        doc.getDocumentElement().normalize();

        NodeList nodeElements = doc.getElementsByTagName("NODE");
        if (nodeElements.getLength() == 0) {
            throw new IllegalStateException("No <NODE> elements found in " + xmlFile);
        }

        Set<Integer> seenIds = new HashSet<>();
        Map<Integer, ParsedNode> nodesById = new HashMap<>();
        int rootCount = 0;
        Integer rootId = null;

        // First pass: parse every node and check per-node structural rules
        for (int i = 0; i < nodeElements.getLength(); i++) {
            Element el = (Element) nodeElements.item(i);

            int id = requireIntField(el, "id");
            if (!seenIds.add(id)) {
                throw new IllegalStateException("Duplicate node id " + id + " found in " + xmlFile);
            }

            String contents = requireTextField(el, "contents");

            boolean hasChildren = el.getElementsByTagName("children").getLength() > 0;
            boolean hasParent = el.getElementsByTagName("parent").getLength() > 0;

            Set<Integer> childIds = new HashSet<>();
            if (hasChildren) {
                NodeList childTags = ((Element) el.getElementsByTagName("children").item(0))
                        .getElementsByTagName("child");
                for (int c = 0; c < childTags.getLength(); c++) {
                    childIds.add(Integer.parseInt(childTags.item(c).getTextContent().trim()));
                }
            }

            Integer parentId = hasParent
                    ? Integer.parseInt(el.getElementsByTagName("parent").item(0).getTextContent().trim())
                    : null;

            if (!hasParent) {
                rootCount++;
                rootId = id;
                if (!hasChildren) {
                    throw new IllegalStateException(
                        "Root node " + id + " must have <children> per spec, but none found.");
                }
            } else {
                // Non-root: either inner (has children) or leaf (no children)
                // Both are valid shapes as long as parent is present, which it is.
            }

            nodesById.put(id, new ParsedNode(id, contents, childIds, parentId, hasChildren));
        }

        // Structural rule: exactly one root
        if (rootCount != 1) {
            throw new IllegalStateException(
                "Expected exactly 1 root node (no <parent>), found " + rootCount + " in " + xmlFile);
        }

        // Second pass: cross-reference children/parent ids actually exist
        for (ParsedNode node : nodesById.values()) {
            for (int childId : node.childIds) {
                if (!nodesById.containsKey(childId)) {
                    throw new IllegalStateException(
                        "Node " + node.id + " references child id " + childId + " which does not exist.");
                }
                ParsedNode child = nodesById.get(childId);
                if (child.parentId == null || child.parentId != node.id) {
                    throw new IllegalStateException(
                        "Node " + childId + " is listed as a child of " + node.id
                        + " but its own <parent> is " + child.parentId + ".");
                }
            }
            if (node.parentId != null && !nodesById.containsKey(node.parentId)) {
                throw new IllegalStateException(
                    "Node " + node.id + " references parent id " + node.parentId + " which does not exist.");
            }
        }

        // Sanity: every non-root node must be reachable from the root
        Set<Integer> visited = new HashSet<>();
        java.util.Deque<Integer> stack = new java.util.ArrayDeque<>();
        stack.push(rootId);
        while (!stack.isEmpty()) {
            int current = stack.pop();
            if (!visited.add(current)) continue;
            for (int childId : nodesById.get(current).childIds) {
                stack.push(childId);
            }
        }
        if (visited.size() != nodesById.size()) {
            throw new IllegalStateException(
                "Tree has " + nodesById.size() + " total nodes but only "
                + visited.size() + " are reachable from root " + rootId + " — disconnected node(s) present.");
        }

        System.out.println("tree.xml is structurally valid: " + nodesById.size()
            + " nodes, root id=" + rootId + ".");
    }

    private static int requireIntField(Element parent, String tag) {
        return Integer.parseInt(requireTextField(parent, tag));
    }

    private static String requireTextField(Element parent, String tag) {
        NodeList list = parent.getElementsByTagName(tag);
        if (list.getLength() == 0) {
            throw new IllegalStateException("Node missing required <" + tag + "> element.");
        }
        return list.item(0).getTextContent();
    }

    private static class ParsedNode {
        final int id;
        final String contents;
        final Set<Integer> childIds;
        final Integer parentId;
        final boolean hasChildren;

        ParsedNode(int id, String contents, Set<Integer> childIds, Integer parentId, boolean hasChildren) {
            this.id = id;
            this.contents = contents;
            this.childIds = childIds;
            this.parentId = parentId;
            this.hasChildren = hasChildren;
        }
    }
}