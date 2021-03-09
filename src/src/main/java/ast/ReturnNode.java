package ast;

import java.util.Objects;

public class ReturnNode implements ASTNode {
    public ASTNode return_value;

    public ReturnNode(ASTNode return_value) {
        this.return_value = return_value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReturnNode that = (ReturnNode) o;
        return Objects.equals(return_value, that.return_value);
    }

}
