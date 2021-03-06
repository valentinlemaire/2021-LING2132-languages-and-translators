package ast;

import java.util.Objects;

public class IndexerAccessNode implements ASTNode {
    public IdentifierNode var;
    public ASTNode indexer;


    public IndexerAccessNode(IdentifierNode var, ASTNode indexer) {
        this.var = var;
        this.indexer = indexer;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        IndexerAccessNode n = (IndexerAccessNode) o;
        return Objects.equals(var, n.var) && Objects.equals(indexer, n.indexer);
    }
}
