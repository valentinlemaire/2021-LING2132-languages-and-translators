package ast;

import java.util.Objects;

public class OrNode implements ASTNode {
    public ASTNode left;
    public ASTNode right;

    public OrNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OrNode orNode = (OrNode) o;
        return Objects.equals(left, orNode.left) && Objects.equals(right, orNode.right);
    }
}
