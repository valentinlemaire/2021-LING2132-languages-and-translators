package ast;

import java.util.Objects;

public class NegationNode implements ASTNode {
    public ASTNode child;

    public NegationNode(ASTNode child) {
        this.child = child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NegationNode orNode = (NegationNode) o;
        return Objects.equals(child, orNode.child);
    }
}
