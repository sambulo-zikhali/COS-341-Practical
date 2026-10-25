package spl.codegen;

import spl.model.TreeNode;

public interface BodyTranslator {
    void transBody(TreeNode p, RenameEnv env, boolean withMine);
}
