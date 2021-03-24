package scopes;

import ast.BinaryNode;

/**
 * A pair of a {@link Scope} and a {@link BinaryNode} declaring an entry in that scope.
 */
public final class DeclarationContext
{
    public final Scope scope;
    public final BinaryNode declaration;

    public DeclarationContext(Scope scope, BinaryNode declaration) {
        this.scope = scope;
        this.declaration = declaration;
    }
}
