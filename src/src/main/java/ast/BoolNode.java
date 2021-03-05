package ast;

public class BoolNode implements ASTNode {

    public boolean isComparison;

    public String comparator;
    public ASTNode left;
    public ASTNode right;

    public boolean value;

    public BoolNode(String comparator, ASTNode left, ASTNode right) {
        this.isComparison = false;
        this.comparator = comparator;
        this.left = left;
        this.right = right;
    }


    public BoolNode(boolean value) {
        this.isComparison = true;
        this.value = value;
    }


    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(BoolNode.class)) return false;
        BoolNode n = (BoolNode) o;
        if (this.isComparison && n.isComparison) return this.value == n.value;
        else if (!this.isComparison && !n.isComparison) return this.comparator.equals(n.comparator) && this.left.equals(n.left) && this.right.equals(n.right);
        else return false;
    }
}
