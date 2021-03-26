package ast;

public class StringNode extends ASTNode {
    public String value;

    public StringNode(String v) {
        this.value = v;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(StringNode.class)) return false;
        StringNode n = (StringNode) o;
        return value.equals(n.value);
    }
}