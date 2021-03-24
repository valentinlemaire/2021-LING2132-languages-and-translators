package scopes;

import ast.VarAssignmentNode;

/**
 * A pair of a {@link Scope} and a {@link BinaryNode} declaring an entry in that scope.
 */
public final class DeclarationContext
{
    public final Scope scope;
    public final VarAssignmentNode declaration;

    public DeclarationContext(Scope scope, VarAssignmentNode declaration) {
        this.scope = scope;
        this.declaration = declaration;
    }
}
