package ast;

import norswap.uranium.Attribute;

public class ASTNode {

    public final Attribute attr (String name) {
        return new Attribute(this, name);
    }

}





















