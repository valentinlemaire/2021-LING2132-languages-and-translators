import ast.*;
import scopes.*;

import norswap.uranium.Reactor;
import norswap.uranium.Rule;
import norswap.utils.visitors.ReflectiveFieldWalker;
import norswap.utils.visitors.Walker;

import static norswap.utils.visitors.WalkVisitType.POST_VISIT;
import static norswap.utils.visitors.WalkVisitType.PRE_VISIT;

public final class SemanticAnalysis {
    // =============================================================================================
    // region [Initialization]
    // =============================================================================================

    private final Reactor R;

    /** Current scope. */
    private Scope scope;

    /** Current context for type inference (currently only to infer the type of empty arrays). */
    private ASTNode inferenceContext;

    /** Index of the current function argument. */
    private int argumentIndex;

    // ---------------------------------------------------------------------------------------------

    private SemanticAnalysis(Reactor reactor) {
        this.R = reactor;
    }

    // ---------------------------------------------------------------------------------------------

    /**
     * Call this method to create a tree walker that will instantiate the typing rules defined
     * in this class when used on an AST, using the given {@code reactor}.
     */
    public static Walker<ASTNode> createWalker (Reactor reactor)
    {
        ReflectiveFieldWalker<ASTNode> walker = new ReflectiveFieldWalker<>(
                ASTNode.class, PRE_VISIT, POST_VISIT);

        SemanticAnalysis analysis = new SemanticAnalysis(reactor);

        // expressions
        walker.register(IntegerNode.class,          PRE_VISIT,  analysis::integer);

        // types

        // declarations & scopes
        walker.register(RootNode.class,            PRE_VISIT,  analysis::root);
        walker.register(VarAssignmentNode.class,   PRE_VISIT,  analysis::varAssignment);

        // statements

        walker.registerFallback(PRE_VISIT,  node -> {});
        walker.registerFallback(POST_VISIT, node -> {});

        return walker;
    }

    // endregion
    // =============================================================================================
    // region [Expressions]
    // =============================================================================================
    private void integer (IntegerNode node) {
        R.set(node, "type", Type.INTEGER);
    }

    // endregion
    // =============================================================================================
    // region [Binary Expressions]
    // =============================================================================================
    // endregion
    // =============================================================================================
    // region [Types & Typing Utilities]
    // =============================================================================================
    // endregion
    // =============================================================================================
    // region [Scopes & Declarations]
    // =============================================================================================

    public void root(RootNode node) {
        assert scope == null;
        scope = new RootScope(node, R);
        R.set(node, "scope", scope);
    }

    public void varAssignment(VarAssignmentNode node) {
        if (node.left instanceof IdentifierNode) {
            DeclarationContext maybeCtx = scope.lookup(((IdentifierNode) node.left).value);

            if (maybeCtx == null) {
                this.inferenceContext = node;
                scope.declare(((IdentifierNode) node.left).value, node);
                R.set(node, "scope", scope);

            }
        }

        R.rule(node, "type")
                .using(node.left.attr("type"), node.right.attr("type"))
                .by(r -> {
                    Type left  = r.get(0);
                    Type right = r.get(1);

                    r.set(node, "type", right); // the type of the assignment is the right-side type

                    if (node.left instanceof IdentifierNode
                            ||  (node.left instanceof BinaryNode && ((BinaryNode) node.left).code == BinaryNode.IDX_ACCESS)) {
                        if (!isAssignableTo(right, left))
                            r.errorFor("Trying to assign a value to a non-compatible lvalue.", node);
                    }
                    else
                        r.errorFor("Trying to assign to an non-lvalue expression.", node.left);
                });
    }

    public boolean isAssignableTo(Type right, Type left) {
        return right == Type.NONE || right == left;
    }


    // endregion
    // =============================================================================================
    // region [Other Statements]
    // =============================================================================================
    // endregion
    // =============================================================================================
}