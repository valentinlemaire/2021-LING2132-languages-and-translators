package ast;

public class ComparisonNode implements ASTNode {
    public static final int EQ  = 0; // ==
    public static final int LEQ = 1; // <=
    public static final int GEQ = 2; // >=
    public static final int NEQ = 3; // !=
    public static final int L   = 4; // <
    public static final int G   = 5; // >

    public int comparator;
    public ASTNode left;
    public ASTNode right;


    public ComparisonNode(int comparator, ASTNode left, ASTNode right) {
        this.comparator = comparator;
        this.left = left;
        this.right = right;
    }


    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(this.getClass())) return false;
        ComparisonNode n = (ComparisonNode) o;
        return (this.comparator == n.comparator) && this.left.equals(n.left) && this.right.equals(n.right);
    }
}
