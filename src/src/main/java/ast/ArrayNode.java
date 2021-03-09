package ast;

import java.util.Objects;

public class ArrayNode implements ASTNode{
    public ASTNode[] elements;

    public ArrayNode(ASTNode[] elements) {
        this.elements = elements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayNode arrayNode = (ArrayNode) o;
        return Objects.equals(this.elements, arrayNode.elements);
    }
}
