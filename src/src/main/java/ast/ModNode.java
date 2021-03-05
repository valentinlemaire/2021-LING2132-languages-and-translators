package ast;

public class ModNode implements ASTNode {
    public ASTNode left;
    public ASTNode right;
    public ModNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(ModNode.class)) return false;
        ModNode n = (ModNode) o;
        return this.left.equals(n.left) && this.right.equals(n.right);
    }
}
