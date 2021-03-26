package ast;

import java.util.List;
import java.util.Objects;

public class ForNode extends ASTNode implements DeclarationNode {
    public IdentifierNode variable;
    public ASTNode list;
    public BlockNode block;

    public ForNode(IdentifierNode variable, ASTNode list, BlockNode block) {
        this.variable = variable;
        this.list = list;
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ForNode forNode = (ForNode) o;
        return Objects.equals(variable, forNode.variable) && Objects.equals(list, forNode.list) && Objects.equals(block, forNode.block);
    }

}
