package ast;

import java.util.Objects;

public class ArgAccessNode implements ASTNode {
    public ASTNode index;

    public ArgAccessNode(ASTNode index) {
        this.index = index;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArgAccessNode that = (ArgAccessNode) o;
        return Objects.equals(index, that.index);
    }

}
