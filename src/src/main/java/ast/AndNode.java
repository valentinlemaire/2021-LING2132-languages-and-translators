package ast;

import java.util.Objects;

public class AndNode implements ASTNode {
    public ASTNode left;
    public ASTNode right;

    public AndNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AndNode orNode = (AndNode) o;
        return Objects.equals(left, orNode.left) && Objects.equals(right, orNode.right);
    }
}
