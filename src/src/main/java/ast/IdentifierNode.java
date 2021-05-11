package ast;

import java.util.Objects;

public class IdentifierNode extends ASTNode {
    public String value;

    public IdentifierNode(String v) {
        this.value = v;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IdentifierNode that = (IdentifierNode) o;
        return Objects.equals(value, that.value);
    }

}
