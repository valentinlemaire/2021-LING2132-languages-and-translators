package ast;

import java.util.List;
import java.util.Objects;

public class ForNode extends ASTNode {
    public IdentifierNode variable;
    public ASTNode list;
    public List<ASTNode> statements;

    public ForNode(IdentifierNode variable, ASTNode list, List<ASTNode> statements) {
        this.variable = variable;
        this.list = list;
        this.statements = statements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForNode forNode = (ForNode) o;
        return Objects.equals(variable, forNode.variable) && Objects.equals(list, forNode.list) && Objects.equals(statements, forNode.statements);
    }

}
