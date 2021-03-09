package ast;

import java.util.List;
import java.util.Objects;

public class PrintNode implements ASTNode {
    public ASTNode to_print;
    public boolean newline;

    public PrintNode(ASTNode to_print) {
        this.to_print = to_print;
        this.newline = false;
    }

    public PrintNode(ASTNode to_print, boolean newline) {
        this.to_print = to_print;
        this.newline = newline;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PrintNode printNode = (PrintNode) o;
        return Objects.equals(this.to_print, printNode.to_print) && this.newline == printNode.newline;
    }
}
