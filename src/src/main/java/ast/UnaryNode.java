package ast;

import java.util.Objects;

public class UnaryNode extends ASTNode {
    public static final int RANGE       = 0;
    public static final int INDEXER     = 1;
    public static final int SORT        = 2;
    public static final int PARSE_INT   = 3;
    public static final int PRINT       = 4;
    public static final int PRINTLN     = 5;
    public static final int NEGATION    = 6;
    public static final int NOT         = 7;
    public static final int RETURN      = 8;
    public static final int LEN         = 9;

    public ASTNode child;
    public int code;

    public UnaryNode(ASTNode child, int code) {
        this.child = child;
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryNode node = (UnaryNode) o;
        return Objects.equals(this.child, node.child) && Objects.equals(this.code, node.code);
    }

    public boolean isArray() {
        return code == RANGE || code == INDEXER || code == SORT;
    }

    public boolean isInteger() {
        return code == PARSE_INT || code == NEGATION || code == LEN;
    }

    public boolean isVoid() {
        return code == PRINT || code == PRINTLN;
    }

    public boolean isReturnStatement() {
        return code == RETURN;
    }

    public boolean isBool() {
        return code == NOT;
    }
}
