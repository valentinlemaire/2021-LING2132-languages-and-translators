package scopes;

import ast.RootNode;
import norswap.uranium.Reactor;

import static scopes.DeclarationKind.*;

/**
 * The lexical scope of a file in Sigh. It is notably responsible for introducing the default
 * declarations made by the language.
 */
public final class RootScope extends Scope
{
    // ---------------------------------------------------------------------------------------------

    private SyntheticDeclarationNode decl (String name, DeclarationKind kind) {
        SyntheticDeclarationNode decl = new SyntheticDeclarationNode(name, kind);
        declare(name,  decl);
        return decl;
    }

    // root scope variables
    public final SyntheticDeclarationNode _true  = decl("True",  VARIABLE);
    public final SyntheticDeclarationNode _false = decl("False", VARIABLE);
    public final SyntheticDeclarationNode _none  = decl("None",  VARIABLE);

    public final SyntheticDeclarationNode _args = decl("args", VARIABLE);

    // root scope functions
    public final SyntheticDeclarationNode print = decl("print", FUNCTION);
    public final SyntheticDeclarationNode println = decl("println", FUNCTION);
    public final SyntheticDeclarationNode sort = decl("sort", FUNCTION);
    public final SyntheticDeclarationNode range = decl("range", FUNCTION);
    public final SyntheticDeclarationNode indexer = decl("indexer", FUNCTION);

    // ---------------------------------------------------------------------------------------------

    public RootScope (RootNode node, Reactor reactor) {
        super(node, null);

    }

    // ---------------------------------------------------------------------------------------------
}
