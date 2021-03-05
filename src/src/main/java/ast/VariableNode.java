package ast;

public class VariableNode implements ASTNode {
    public boolean isNegative;
    public String value;

    public VariableNode(String v) {
        this.value = v;
        this.isNegative = false;
    }

    public VariableNode(String v, boolean isNegative) {
        this.value = v;
        this.isNegative = isNegative;
    }

    public VariableNode(String v, String sign) {
        this.value = v;
        this.isNegative = sign.equals("-");
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(VariableNode.class)) return false;
        VariableNode n = (VariableNode) o;
        return value.equals(n.value);
    }
}
