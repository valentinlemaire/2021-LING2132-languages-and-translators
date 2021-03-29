package ast;

import java.util.Objects;

public class ParameterNode extends ASTNode implements DeclarationNode {
    public IdentifierNode param;

    public ParameterNode(IdentifierNode param) {
        this.param = param;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParameterNode that = (ParameterNode) o;
        return Objects.equals(param, that.param);
    }

    @Override
    public int hashCode() {
        return Objects.hash(param);
    }
}
