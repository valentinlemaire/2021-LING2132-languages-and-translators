package ast;

import java.util.List;
import java.util.Objects;

public class WhileNode extends ASTNode {
    public ASTNode bool;
    public BlockNode block;

    public WhileNode(ASTNode bool, BlockNode block) {
        this.bool = bool;
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        WhileNode whileNode = (WhileNode) o;
        return Objects.equals(bool, whileNode.bool) && Objects.equals(block, whileNode.block);
    }
}
