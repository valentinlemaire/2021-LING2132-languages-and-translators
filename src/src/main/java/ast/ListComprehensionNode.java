package ast;

import java.util.Objects;

public class ListComprehensionNode extends ASTNode implements DeclarationNode {
    public ASTNode expression;
    public IdentifierNode variable;
    public ASTNode iterable;
    public ASTNode condition;

    public ListComprehensionNode(ASTNode expression, IdentifierNode variable, ASTNode iterable, ASTNode condition) {
        this.expression = expression;
        this.variable = variable;
        this.iterable = iterable;
        this.condition = condition;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListComprehensionNode that = (ListComprehensionNode) o;
        return Objects.equals(expression, that.expression) && Objects.equals(variable, that.variable) && Objects.equals(iterable, that.iterable) && Objects.equals(condition, that.condition);
    }
}
