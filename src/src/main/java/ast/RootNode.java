package ast;

import java.util.List;
import java.util.Objects;

public class RootNode extends ASTNode {
    public List<ASTNode> statements;

    public RootNode(List<ASTNode> statements) {
        this.statements = statements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RootNode rootNode = (RootNode) o;
        return Objects.equals(statements, rootNode.statements);
    }
}
