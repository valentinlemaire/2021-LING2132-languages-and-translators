package ast;

import java.util.List;
import java.util.Objects;

public class FunctionDefinitionNode extends ASTNode implements DeclarationNode {
    public IdentifierNode name;
    public List<ParameterNode> args;
    public BlockNode block;

    public FunctionDefinitionNode(IdentifierNode name, List<ParameterNode> args, BlockNode block) {
        this.name = name;
        this.args = args;
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionDefinitionNode that = (FunctionDefinitionNode) o;
        return Objects.equals(name, that.name) && Objects.equals(args, that.args) && Objects.equals(block, that.block);
    }

}
