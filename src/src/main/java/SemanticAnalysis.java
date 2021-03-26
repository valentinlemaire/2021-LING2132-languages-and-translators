/*
 * This code is heavily inspired by the example given by the teacher : Nicolas Laurent.
 * His code can be found on GitHub
 * [https://github.com/norswap/sigh/blob/master/src/norswap/sigh/SemanticAnalysis.java]
 *
 */
import ast.*;
import norswap.uranium.Attribute;
import scopes.*;

import norswap.uranium.Reactor;
import norswap.uranium.Rule;
import norswap.utils.visitors.ReflectiveFieldWalker;
import norswap.utils.visitors.Walker;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

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
        walker.register(IntegerNode.class,              PRE_VISIT,  analysis::integer);
        walker.register(StringNode.class,               PRE_VISIT,  analysis::string);
        walker.register(IdentifierNode.class,           PRE_VISIT,  analysis::identifier);
        walker.register(ArrayNode.class,                PRE_VISIT,  analysis::array);
        walker.register(FunctionCallNode.class,         PRE_VISIT,  analysis::functionCall);
        walker.register(UnaryNode.class,                PRE_VISIT,  analysis::unaryExpression);
        walker.register(BinaryNode.class,               PRE_VISIT,  analysis::binaryExpression);

        // declarations & scopes
        walker.register(RootNode.class,                 PRE_VISIT,  analysis::root);
        walker.register(BlockNode.class,                PRE_VISIT,  analysis::block);
        walker.register(FunctionDefinitionNode.class,   PRE_VISIT,  analysis::functionDeclaration);
        walker.register(ForNode.class,                  PRE_VISIT,  analysis::for_);
        walker.register(IfNode.class,                   PRE_VISIT,  analysis::if_);
        walker.register(WhileNode.class,                PRE_VISIT,  analysis::while_);

        walker.register(RootNode.class,                 POST_VISIT, analysis::popScope);
        walker.register(BlockNode.class,                POST_VISIT, analysis::popScope);
        walker.register(FunctionDefinitionNode.class,   POST_VISIT, analysis::popScope);
        walker.register(ForNode.class,                  POST_VISIT, analysis::popScope);


        // statements
        walker.register(VarAssignmentNode.class,        PRE_VISIT,  analysis::varAssignment);

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

    public void popScope(ASTNode node) {
        scope = scope.parent;
    }

    public void root(RootNode node) {
        assert scope == null;
        scope = new RootScope(node, R);
        R.set(node, "scope", scope);
    }

    public void block(BlockNode node) {
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);
    }


    public void functionDeclaration(FunctionDefinitionNode node) {
        scope.declare(node.name.value, node);
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);
        R.set(node, "type", Type.UNKOWN_TYPE);


        // declaring function args in scope
        R.rule()
                .by(r -> {
                    if (node.args != null) {
                        for (ASTNode arg : node.args) {
                            if (!(arg instanceof IdentifierNode)) {
                                r.error("Function declared with invalid variable name as argument", node);
                            } else {
                                scope.declare(((IdentifierNode) arg).value, node);
                            }
                        }
                    }

                });

    }

    public void if_(IfNode node) {
        R.rule()
                .using(node.bool, "type")
                .by(r -> {
                    Type conditionType = r.get(0);
                    if (!(conditionType == Type.BOOLEAN || conditionType == Type.UNKOWN_TYPE)) {
                        r.error("If statement must have a boolean as condition", node);
                    }
                });


    }

    public void while_(WhileNode node) {
        R.rule()
                .using(node.bool, "type")
                .by(r -> {
                    Type conditionType = r.get(0);
                    if (!(conditionType == Type.BOOLEAN || conditionType == Type.UNKOWN_TYPE)) {
                        r.error("If statement must have a boolean as condition", node);
                    }
                });
    }

    public void for_(ForNode node) {
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);

        R.rule()
                .using(node.list, "type")
                .by(r -> {
                    Type iterableType = r.get(0);
                    if (!(iterableType == Type.ARRAY || iterableType == Type.UNKOWN_TYPE)) {
                        r.error("If statement must have a boolean as condition", node);
                    }
                });

        scope.declare(node.variable.value, node);
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
        return right == Type.NONE || right == Type.UNKOWN_TYPE || right == left;
    }


    // endregion
    // =============================================================================================
    // region [Other Statements]
    // =============================================================================================
    // endregion
    // =============================================================================================
}