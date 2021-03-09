package ast;

import java.util.Objects;

public class PairNode implements ASTNode{
    public ASTNode element1;
    public ASTNode element2;

    public PairNode(ASTNode e1, ASTNode e2) {
        this.element1 = e1;
        this.element2 = e2;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PairNode pair = (PairNode) o;
        return Objects.equals(this.element1, pair.element1) && Objects.equals(this.element2, pair.element2);
    }
}
