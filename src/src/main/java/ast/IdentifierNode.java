package ast;

public class IdentifierNode extends ASTNode {
    public String value;

    public IdentifierNode(String v) {
        this.value = v;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(IdentifierNode.class)) return false;
        IdentifierNode n = (IdentifierNode) o;
        return value.equals(n.value);
    }
}
