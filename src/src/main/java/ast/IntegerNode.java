package ast;

public class IntegerNode implements ASTNode {
    public int value;
    public IntegerNode(int v) {
        this.value = v;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(IntegerNode.class)) return false;
        IntegerNode n = (IntegerNode) o;
        return value == n.value;
    }
}
