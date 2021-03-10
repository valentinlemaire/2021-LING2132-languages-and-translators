package ast;

import java.util.Objects;

public class SortNode implements ASTNode {
    public ASTNode to_sort;

    public SortNode(ASTNode to_sort) {
        this.to_sort = to_sort;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SortNode sortNode = (SortNode) o;
        return Objects.equals(this.to_sort, sortNode.to_sort);
    }
}
