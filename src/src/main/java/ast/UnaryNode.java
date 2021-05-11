package ast;

import java.util.Objects;

public class UnaryNode extends ASTNode {
    public static final int NEGATION    = 0;
    public static final int NOT         = 1;
    public static final int RETURN      = 2;


    public ASTNode child;
    public int code;

    public UnaryNode(ASTNode child, int code) {
        this.child = child;
        this.code = code;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UnaryNode node = (UnaryNode) o;
        return Objects.equals(this.child, node.child) && Objects.equals(this.code, node.code);
    }


}
