package spl.output;

import java.util.List;

public class TreeValidationException {
    private final List<String> issues;
 
    public TreeValidationException(List<String> issues) {
        super(buildMessage(issues));
        this.issues = issues;
    }
 
    public List<String> getIssues() {
        return issues;
    }
 
    private static String buildMessage(List<String> issues) {
        StringBuilder sb = new StringBuilder("Tree validation failed with " + issues.size() + " issue(s):");
        for (String issue : issues) {
            sb.append("\n  - ").append(issue);
        }
        return sb.toString();
    }
}
