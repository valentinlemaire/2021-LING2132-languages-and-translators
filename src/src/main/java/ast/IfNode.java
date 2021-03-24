package ast;

import java.util.List;
import java.util.Objects;

public class IfNode extends ASTNode {
    public ASTNode bool;
    public List<ASTNode> statements;
    public List<ASTNode> else_blocks;

    public IfNode(ASTNode bool, List<ASTNode> statements, List<ASTNode> else_blocks) {
        this.bool = bool;
        this.statements = statements;
        this.else_blocks = else_blocks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IfNode ifNode = (IfNode) o;
        return Objects.equals(bool, ifNode.bool) && Objects.equals(statements, ifNode.statements) && Objects.equals(else_blocks, ifNode.else_blocks);
    }
}
