/*
 * This code is heavily inspired by the example given by the teacher : Nicolas Laurent.
 * His code can be found on GitHub
 * [https://github.com/norswap/sigh/blob/master/src/norswap/sigh/SemanticAnalysis.java]
 *
 */
import ast.*;
import scopes.*;

import norswap.uranium.Attribute;
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
        walker.register(IntegerNode.class,      PRE_VISIT,  analysis::integer);
        walker.register(StringNode.class,       PRE_VISIT,  analysis::string);
        walker.register(IdentifierNode.class,   PRE_VISIT,  analysis::identifier);
        walker.register(ArrayNode.class,        PRE_VISIT,  analysis::array);
        walker.register(FunctionCallNode.class, PRE_VISIT,  analysis::functionCall);
        walker.register(UnaryNode.class,        PRE_VISIT,  analysis::unaryExpression);
        walker.register(BinaryNode.class,       PRE_VISIT,  analysis::binaryExpression);

        // declarations & scopes

        // statements


        walker.registerFallback(PRE_VISIT,  node -> {});
        walker.registerFallback(POST_VISIT, node -> {});

        return walker;
    }

    // endregion
    // =============================================================================================
    // region [Expressions]
    // =============================================================================================
    private void integer(IntegerNode node) {
        R.set(node, "type", Types.INTEGER);
    }

    private void string(StringNode node) {
        R.set(node, "type", Types.STRING);
    }

    private void identifier(IdentifierNode node) {
        // TODO
    }

    private void array(ArrayNode node) {
        // TODO
    }

    private void functionCall(FunctionCallNode node) {
        // TODO
    }

    private void unaryExpression(UnaryNode node) {
        // TODO implement node.isSomething() methods
    }

    private void binaryExpression(BinaryNode node) {
        /* array access, arithmetic operations, ... */
        // TODO compare using node.isArithmeticOperation() and other methods
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
    // endregion
    // =============================================================================================
    // region [Other Statements]
    // =============================================================================================
    // endregion
    // =============================================================================================
}