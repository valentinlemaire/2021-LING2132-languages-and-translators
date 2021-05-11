package ast;

import ast.ASTNode;

import java.util.Objects;

public class VarAssignmentNode extends ASTNode implements DeclarationNode {
    public ASTNode left;
    public ASTNode right;
    public boolean final_;

    public VarAssignmentNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
        this.final_ = false;
    }

    public VarAssignmentNode(ASTNode left, ASTNode right, boolean final_) {
        this.left = left;
        this.right = right;
        this.final_ = final_;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VarAssignmentNode that = (VarAssignmentNode) o;
        return final_ == that.final_ && Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }
}
