package ast;

public class IntegerNode extends ASTNode {
    public long value;
    public IntegerNode(long v) {
        this.value = v;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(IntegerNode.class)) return false;
        IntegerNode n = (IntegerNode) o;
        return value == n.value;
    }
}
