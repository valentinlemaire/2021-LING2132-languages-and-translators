package ast;

import java.util.List;
import java.util.Objects;

public class IfNode extends ASTNode {
    public ASTNode bool;
    public BlockNode block;
    public List<ElseNode> else_blocks;

    public IfNode(ASTNode bool, BlockNode block, List<ElseNode> else_blocks) {
        this.bool = bool;
        this.block = block;
        this.else_blocks = else_blocks;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IfNode ifNode = (IfNode) o;
        return Objects.equals(bool, ifNode.bool) && Objects.equals(block, ifNode.block) && Objects.equals(else_blocks, ifNode.else_blocks);
    }
}
