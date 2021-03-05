package ast;

public class AddNode implements ASTNode {
    public ASTNode left;
    public ASTNode right;
    public AddNode(ASTNode left, ASTNode right) {
        this.left = left;
        this.right = right;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(AddNode.class)) return false;
        AddNode n = (AddNode) o;
        return this.left.equals(n.left) && this.right.equals(n.right);
    }
}
