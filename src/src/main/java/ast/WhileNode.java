package ast;

import java.util.List;
import java.util.Objects;

public class WhileNode implements ASTNode {
    public ASTNode bool;
    public List<ASTNode> statements;

    public WhileNode(ASTNode bool, List<ASTNode> statements) {
        this.bool = bool;
        this.statements = statements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhileNode whileNode = (WhileNode) o;
        return Objects.equals(bool, whileNode.bool) && Objects.equals(statements, whileNode.statements);
    }
}
