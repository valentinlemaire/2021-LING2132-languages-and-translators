package ast;

import java.util.Objects;

public class BinaryNode implements ASTNode {

    public static final int ADD         = 0;
    public static final int SUB         = 1;
    public static final int MUL         = 2;
    public static final int DIV         = 3;
    public static final int MOD         = 4;

    public static final int OR          = 5;
    public static final int AND         = 6;

    public static final int EQ          = 7;
    public static final int NEQ         = 8;

    public static final int LEQ         = 9;
    public static final int GEQ         = 10;
    public static final int L           = 11;
    public static final int G           = 12;

    public static final int PAIR        = 13;

    public static final int IDX_ACCESS  = 14;

    public ASTNode left;
    public ASTNode right;
    public int code;

    public BinaryNode(ASTNode left, ASTNode right, int code) {
        this.left = left;
        this.right = right;
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BinaryNode that = (BinaryNode) o;
        return code == that.code && Objects.equals(left, that.left) && Objects.equals(right, that.right);
    }

    public boolean isArithmeticOperation() {
        return code >= ADD && code <= MOD;
    }

    public boolean isLogicOperation() {
        return code == OR || code == AND;
    }

    public boolean isEqualityComparison() {
        return code == EQ || code == NEQ;
    }

    public boolean isInequalityComparison() {
        return code >= LEQ && code <= G;
    }

    public boolean isPair() {
        return code == PAIR;
    }

    public boolean isIdxAccess() {
        return code == IDX_ACCESS;
    }
}
