package ast;

import java.util.List;
import java.util.Objects;

public class ArrayNode implements ASTNode{
    public List<ASTNode> elements;
    public ASTNode size;

    public ArrayNode(List<ASTNode> elements) {
        this.elements = elements;
    }

    public ArrayNode(ASTNode size){
        this.size = size;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ArrayNode arrayNode = (ArrayNode) o;
        return Objects.equals(this.elements, arrayNode.elements);
    }
}
