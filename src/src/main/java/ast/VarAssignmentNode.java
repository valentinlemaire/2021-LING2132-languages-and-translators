package ast;

import ast.ASTNode;

import java.util.Objects;

public class VarAssignmentNode extends ASTNode implements DeclarationNode {
    public ASTNode left;
    public ASTNode right;
    public int code;

    public VarAssignmentNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarAssignmentNode that = (VarAssignmentNode) o;
        return code == that.code && Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }
}
