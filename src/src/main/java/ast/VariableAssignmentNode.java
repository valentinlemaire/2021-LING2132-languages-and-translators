package ast;

public class VariableAssignmentNode implements ASTNode {
    public VariableNode var;
    public ASTNode value;

    public VariableAssignmentNode(VariableNode var, ASTNode value) {
        this.var = var;
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (!o.getClass().equals(VariableAssignmentNode.class)) return false;
        VariableAssignmentNode n = (VariableAssignmentNode) o;
        return var.equals(n.var) && value.equals(n.value);
    }
}
