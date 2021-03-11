package ast;

import java.util.List;
import java.util.Objects;

public class MapNode implements ASTNode{
    public List<BinaryNode> elements;

    public MapNode(List<BinaryNode> elements) {
        this.elements = elements;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MapNode mapNode = (MapNode) o;
        return Objects.equals(this.elements, mapNode.elements);
    }
}