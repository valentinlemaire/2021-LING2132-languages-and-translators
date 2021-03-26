package ast;

import java.util.List;
import java.util.Objects;

public class RootNode extends ASTNode {
    public BlockNode block;

    public RootNode(BlockNode block) {
        this.block = block;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RootNode rootNode = (RootNode) o;
        return Objects.equals(block, rootNode.block);
    }
}
