package ast;

public class MultNode implements ASTNode {
    public ASTNode left;
    public ASTNode right;
    public MultNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(MultNode.class)) return false;
        MultNode n = (MultNode) o;
        return this.left.equals(n.left) && this.right.equals(n.right);
    }
}
