package ast;

import java.util.Objects;

public class ParseIntNode implements ASTNode {
    public ASTNode to_parse;

    public ParseIntNode(ASTNode to_parse) {
        this.to_parse = to_parse;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ParseIntNode parseIntNode = (ParseIntNode) o;
        return Objects.equals(this.to_parse, parseIntNode.to_parse);
    }
}
