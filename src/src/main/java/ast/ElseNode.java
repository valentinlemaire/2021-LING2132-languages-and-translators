package ast;

import java.util.List;
import java.util.Objects;

public class ElseNode implements ASTNode {
    public ASTNode bool;
    public List<ASTNode> statements;

    public ElseNode(ASTNode bool, List<ASTNode> statements) {
        this.bool = bool;
        this.statements = statements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElseNode elseNode = (ElseNode) o;
        return Objects.equals(bool, elseNode.bool) && Objects.equals(statements, elseNode.statements);
    }
}
