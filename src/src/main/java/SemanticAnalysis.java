
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
    private SighNode inferenceContext;

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
    public static Walker<SighNode> createWalker (Reactor reactor)
    {
        ReflectiveFieldWalker<SighNode> walker = new ReflectiveFieldWalker<>(
                SighNode.class, PRE_VISIT, POST_VISIT);

        SemanticAnalysis analysis = new SemanticAnalysis(reactor);

        // expressions
        walker.register(IntLiteralNode.class,           PRE_VISIT,  analysis::intLiteral);
        walker.register(FloatLiteralNode.class,         PRE_VISIT,  analysis::floatLiteral);
        walker.register(StringLiteralNode.class,        PRE_VISIT,  analysis::stringLiteral);
        walker.register(ReferenceNode.class,            PRE_VISIT,  analysis::reference);
        walker.register(ConstructorNode.class,          PRE_VISIT,  analysis::constructor);
        walker.register(ArrayLiteralNode.class,         PRE_VISIT,  analysis::arrayLiteral);
        walker.register(ParenthesizedNode.class,        PRE_VISIT,  analysis::parenthesized);
        walker.register(FieldAccessNode.class,          PRE_VISIT,  analysis::fieldAccess);
        walker.register(ArrayAccessNode.class,          PRE_VISIT,  analysis::arrayAccess);
        walker.register(FunCallNode.class,              PRE_VISIT,  analysis::funCall);
        walker.register(UnaryExpressionNode.class,      PRE_VISIT,  analysis::unaryExpression);
        walker.register(BinaryExpressionNode.class,     PRE_VISIT,  analysis::binaryExpression);
        walker.register(AssignmentNode.class,           PRE_VISIT,  analysis::assignment);

        // types
        walker.register(SimpleTypeNode.class,           PRE_VISIT,  analysis::simpleType);
        walker.register(ArrayTypeNode.class,            PRE_VISIT,  analysis::arrayType);

        // declarations & scopes
        walker.register(RootNode.class,                 PRE_VISIT,  analysis::root);
        walker.register(BlockNode.class,                PRE_VISIT,  analysis::block);
        walker.register(VarDeclarationNode.class,       PRE_VISIT,  analysis::varDecl);
        walker.register(FieldDeclarationNode.class,     PRE_VISIT,  analysis::fieldDecl);
        walker.register(ParameterNode.class,            PRE_VISIT,  analysis::parameter);
        walker.register(FunDeclarationNode.class,       PRE_VISIT,  analysis::funDecl);
        walker.register(StructDeclarationNode.class,    PRE_VISIT,  analysis::structDecl);

        walker.register(RootNode.class,                 POST_VISIT, analysis::popScope);
        walker.register(BlockNode.class,                POST_VISIT, analysis::popScope);
        walker.register(FunDeclarationNode.class,       POST_VISIT, analysis::popScope);

        // statements
        walker.register(ExpressionStatementNode.class,  PRE_VISIT,  node -> {});
        walker.register(IfNode.class,                   PRE_VISIT,  analysis::ifStmt);
        walker.register(WhileNode.class,                PRE_VISIT,  analysis::whileStmt);
        walker.register(ReturnNode.class,               PRE_VISIT,  analysis::returnStmt);

        walker.registerFallback(POST_VISIT, node -> {});

        return walker;
    }

    // endregion
    // =============================================================================================
    // region [Expressions]
    // =============================================================================================
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