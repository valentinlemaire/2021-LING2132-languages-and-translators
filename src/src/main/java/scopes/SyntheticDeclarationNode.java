package scopes;

import ast.BinaryNode;
import ast.VarAssignmentNode;

import java.util.Locale;

/**
 * In Sigh's implementation, every reference must resolve to a {@link VarAssignmentNode}.
 * A {@code SyntheticDeclarationNode} is such a node for declarations that have not been
 * introduced by the user.
 *
 * <p>At present, all such declarations are unconditionally introduced in the {@link RootScope}.
 */
public final class SyntheticDeclarationNode extends VarAssignmentNode
{
    private final String name;
    private final DeclarationKind kind;

    public SyntheticDeclarationNode(String name, DeclarationKind kind) {
        super(null, null);
        this.name = name;
        this.kind = kind;
    }

    public String name () {
        return name;
    }

    public DeclarationKind kind() {
        return kind;
    }

    public String contents () {
        return name;
    }

    public String declaredThing () {
        return "built-in " + kind.name().toLowerCase(Locale.ROOT);
    }
}
