package ast;

public class SubNode implements ASTNode {
    public ASTNode left;
    public ASTNode right;
    public SubNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(SubNode.class)) return false;
        SubNode n = (SubNode) o;
        return this.left.equals(n.left) && this.right.equals(n.right);
    }
}
