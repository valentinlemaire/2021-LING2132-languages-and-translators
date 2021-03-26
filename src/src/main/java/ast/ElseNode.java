package ast;

import java.util.List;
import java.util.Objects;

public class ElseNode extends ASTNode {
    public ASTNode bool;
    public BlockNode block;

    public ElseNode(ASTNode bool, BlockNode block) {
        this.bool = bool;
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ElseNode elseNode = (ElseNode) o;
        return Objects.equals(bool, elseNode.bool) && Objects.equals(block, elseNode.block);
    }
}
