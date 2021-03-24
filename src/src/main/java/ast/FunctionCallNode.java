package ast;

import java.util.List;
import java.util.Objects;

public class FunctionCallNode extends ASTNode {

    public IdentifierNode functionName;
    public List<ASTNode> args;

    public FunctionCallNode(IdentifierNode functionName, List<ASTNode> args) {
        this.functionName = functionName;
        this.args = args;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FunctionCallNode that = (FunctionCallNode) o;
        return Objects.equals(functionName, that.functionName) && Objects.equals(args, that.args);
    }
}
