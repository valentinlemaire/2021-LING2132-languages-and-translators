package ast;

public class IdentifierNode implements ASTNode {
    public boolean isNegative;
    public String value;

    public IdentifierNode(String v) {
        this.value = v;
        this.isNegative = false;
    }

    public IdentifierNode(String v, boolean isNegative) {
        this.value = v;
        this.isNegative = isNegative;
    }

    public IdentifierNode(String v, String sign) {
        this.value = v;
        this.isNegative = sign.equals("-");
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(IdentifierNode.class)) return false;
        IdentifierNode n = (IdentifierNode) o;
        return value.equals(n.value);
    }
}
