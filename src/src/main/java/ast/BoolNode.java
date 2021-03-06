package ast;

import norswap.autumn.Grammar;

public class BoolNode implements ASTNode {

    public static final int EQ  = 0; // ==
    public static final int LEQ = 1; // <=
    public static final int GEQ = 2; // >=
    public static final int NEQ = 3; // !=
    public static final int L   = 4; // <
    public static final int G   = 5; // >

    public boolean isComparison;

    public int comparator;
    public ASTNode left;
    public ASTNode right;

    public boolean value;

    public BoolNode(int comparator, ASTNode left, ASTNode right) {
        this.isComparison = true;
        this.comparator = comparator;
        this.left = left;
        this.right = right;
    }


    public BoolNode(boolean value) {
        this.isComparison = false;
        this.value = value;
    }


    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(BoolNode.class)) return false;
        BoolNode n = (BoolNode) o;
        if (!this.isComparison && !n.isComparison) return this.value == n.value;
        else if (this.isComparison && n.isComparison) return (this.comparator == n.comparator) && this.left.equals(n.left) && this.right.equals(n.right);
        else return false;
    }
}
