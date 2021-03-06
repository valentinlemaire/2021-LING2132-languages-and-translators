package ast;


public class BoolNode implements ASTNode {

    public boolean value;

    public BoolNode(boolean value) {
        this.value = value;
    }


    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(BoolNode.class)) return false;
        BoolNode n = (BoolNode) o;
        return this.value == n.value;
    }
}
