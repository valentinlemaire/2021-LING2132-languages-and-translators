package ast;

public class DivNode implements ASTNode {
    public ASTNode left;
    public ASTNode right;
    public DivNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(DivNode.class)) return false;
        DivNode n = (DivNode) o;
        return this.left.equals(n.left) && this.right.equals(n.right);
    }
}
