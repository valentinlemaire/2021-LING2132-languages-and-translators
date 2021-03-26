package ast;

import java.util.List;
import java.util.Objects;

public class BlockNode extends ASTNode {
    public List<ASTNode> statements;

    public BlockNode(List<ASTNode> statements) {
        this.statements = statements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BlockNode blockNode = (BlockNode) o;
        return Objects.equals(statements, blockNode.statements);
    }

}
