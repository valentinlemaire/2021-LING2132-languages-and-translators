package ast;

import java.util.Objects;

public class NotNode implements ASTNode {
    public ASTNode child;

    public NotNode(ASTNode child) {
        this.child = child;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        NotNode n = (NotNode) o;
        return Objects.equals(child, n.child);
    }
}
