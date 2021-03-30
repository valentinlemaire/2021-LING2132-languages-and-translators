/*
 * This code is heavily inspired by the example given by the teacher : Nicolas Laurent.
 * His code can be found on GitHub
 * [https://github.com/norswap/sigh/blob/master/src/norswap/sigh/SemanticAnalysis.java]
 *
 */
import Types.Type;
import ast.*;
import norswap.uranium.Attribute;
import scopes.*;

import norswap.uranium.Reactor;
import norswap.uranium.Rule;
import norswap.utils.visitors.ReflectiveFieldWalker;
import norswap.utils.visitors.Walker;

import java.util.stream.Stream;

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
        walker.register(BoolNode.class,                 PRE_VISIT,  analysis::bool);
        walker.register(StringNode.class,               PRE_VISIT,  analysis::string);
        walker.register(NoneNode.class,                 PRE_VISIT,  analysis::none);
        walker.register(IdentifierNode.class,           PRE_VISIT,  analysis::identifier);
        walker.register(ArrayNode.class,                PRE_VISIT,  analysis::array);
        walker.register(MapNode.class,                  PRE_VISIT,  analysis::map);
        walker.register(FunctionCallNode.class,         PRE_VISIT,  analysis::functionCall);
        walker.register(UnaryNode.class,                PRE_VISIT,  analysis::unaryExpression);
        walker.register(BinaryNode.class,               PRE_VISIT,  analysis::binaryExpression);

        // declarations & scopes
        walker.register(RootNode.class,                 PRE_VISIT,  analysis::root);
        walker.register(BlockNode.class,                PRE_VISIT,  analysis::block);
        walker.register(ParameterNode.class,            PRE_VISIT,  analysis::parameter);
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
        R.set(node, "type", Type.INTEGER);
    }

    private void bool(BoolNode node) {
        R.set(node, "type", Type.BOOLEAN);
    }

    private void string(StringNode node) {
        R.set(node, "type", Type.STRING);
    }

    private void none(NoneNode node) {
        R.set(node, "type", Type.NONE);
    }

    private void identifier(IdentifierNode node) {
        final Scope scope = this.scope;

        // Try to lookup immediately. This must succeed for variables, but not necessarily for
        // functions or types. By looking up now, we can report looked up variables later
        // as being used before being defined.
        DeclarationContext maybeCtx = scope.lookup(node.value);

        if (maybeCtx != null) {
            R.set(node, "scope", maybeCtx.scope);

            R.rule(node, "type")
                    .using(maybeCtx.declaration, "type")
                    .by(Rule::copyFirst);
        }
    }

    private void array(ArrayNode node) {
        if (node.elements != null) {
            Attribute[] deps = node.elements.stream().map(it -> it.attr("type")).toArray(Attribute[]::new);
            R.rule(node.attr("type"))
                    .using(deps)
                    .by(r -> {
                        Type superType = Type.UNKNOWN_TYPE;
                        for (int i = 0; i < deps.length; i++) {
                            superType = getSuperType(superType, r.get(i));
                            if (superType == null) {
                                r.errorFor("All array elements must have the same type", node, node.attr("type"));
                                return;
                            }
                        }
                        r.set(node, "type", Type.ARRAY);
                    });
        } else if (node.size != null) {
            R.rule(node.attr("type"))
                    .using(node.size.attr("type"))
                    .by(r -> {
                        Type sizeType = r.get(0);
                        if (!(sizeType == Type.INTEGER || sizeType == Type.UNKNOWN_TYPE)) {
                            r.errorFor("Array must be initialised with integer size", node, node.attr("type"));
                            return;
                        }
                        r.set(node, "type", Type.ARRAY);
                    });
        }
    }

    private void map(MapNode node) {
        if (node.elements != null) {
            Attribute[] deps  = Stream.concat(
                    node.elements.stream().map(it -> it.left .attr("type")),
                    node.elements.stream().map(it -> it.right.attr("type")))
                .toArray(Attribute[]::new);

            R.rule(node.attr("type"))
                    .using(deps)
                    .by(r -> {
                        Type superType = Type.UNKNOWN_TYPE;
                        for (int i = 0; i < deps.length/2; i++) {
                            superType = getSuperType(superType, r.get(i));
                            if (superType == null) {
                                r.errorFor("All keys of a map must have the same type", node, node.attr("type"));
                                return;
                            }
                        }
                        superType = Type.UNKNOWN_TYPE;
                        for (int i = deps.length/2; i < deps.length; i++) {
                            superType = getSuperType(superType, r.get(i));
                            if (superType == null) {
                                r.errorFor("All values of a map must have the same type", node, node.attr("type"));
                                return;
                            }
                        }
                        r.set(node, "type", Type.MAP);
                    });
        }
    }

    private void functionCall(FunctionCallNode node) {

        DeclarationContext maybeCtx = scope.lookup(node.functionName.value);

        if (maybeCtx != null) {
            R.set(node, "scope", maybeCtx.scope);

            R.rule(node, "type")
                    .using(maybeCtx.declaration, "type")
                    .by(Rule::copyFirst);
        }
    }

    private void unaryExpression(UnaryNode node) {
        switch (node.code) {
            case UnaryNode.RANGE:
                R.rule(node.attr("type"))
                        .using(node.child.attr("type"))
                        .by(r -> {
                           Type argType = r.get(0);
                           if (argType == Type.INTEGER || argType == Type.UNKNOWN_TYPE) {
                               r.set(node, "type", Type.ARRAY);
                           } else {
                               r.errorFor("Argument of range function must be an integer", node, node.attr("type"));
                           }
                        });
                break;
            case UnaryNode.INDEXER:
                R.rule(node.attr("type"))
                        .using(node.child.attr("type"))
                        .by(r -> {
                           Type nodeType = r.get(0);
                           if (nodeType == Type.ARRAY || nodeType == Type.MAP || nodeType == Type.UNKNOWN_TYPE) {
                               r.set(node, "type", Type.ARRAY);
                           } else {
                               r.errorFor("Argument of indexer function must be an array or a map", node, node.attr("type"));
                           }
                        });
                break;
            case UnaryNode.SORT:
                R.rule(node.attr("type"))
                    .using(node.child.attr("type"))
                    .by(r -> {
                        Type nodeType = r.get(0);
                        if (nodeType == Type.ARRAY || nodeType == Type.UNKNOWN_TYPE) {
                            r.set(node, "type", Type.ARRAY);
                        } else {
                            r.errorFor("Argument of sort function must be an array", node, node.attr("type"));
                        }
                    });
                break;
            case UnaryNode.PARSE_INT:
                R.rule(node.attr("type"))
                        .using(node.child.attr("type"))
                        .by(r -> {
                            Type childType = r.get(0);
                            if (childType == Type.STRING || childType == Type.UNKNOWN_TYPE) {
                                r.set(node, "type", Type.INTEGER);
                            } else {
                                r.errorFor("Argument of int function must be a string", node, node.attr("type"));
                            }
                        });
                break;
            case UnaryNode.PRINT:
            case UnaryNode.PRINTLN:
                R.set(node, "type", Type.NONE);
                break;
            case UnaryNode.NEGATION:
                R.rule(node.attr("type"))
                        .using(node.child.attr("type"))
                        .by(r -> {
                            Type childType = r.get(0);
                            if (childType == Type.INTEGER || childType == Type.UNKNOWN_TYPE) {
                                r.set(node, "type", Type.INTEGER);
                            } else {
                                r.errorFor("Cannot negate a non-int type", node, node.attr("type"));
                            }
                        });
                break;
            case UnaryNode.NOT:
                R.rule(node.attr("type"))
                        .using(node.child.attr("type"))
                        .by(r -> {
                            Type childType = r.get(0);
                            if (childType == Type.BOOLEAN || childType == Type.UNKNOWN_TYPE) {
                                r.set(node, "type", Type.BOOLEAN);
                            } else {
                                r.errorFor("Cannot negate a non-int type", node, node.attr("type"));
                            }
                        });
                break;
            case UnaryNode.RETURN:
                FunctionDefinitionNode funDef = currentFunction();
                R.rule()
                        .using()
                        .by(r -> {
                            if (funDef == null) {
                                r.error("Cannot return when not in a function", node);
                            }
                        });

                break;
            case UnaryNode.LEN:
                R.rule(node.attr("type"))
                        .using(node.child.attr("type"))
                        .by(r -> {
                            Type childType = r.get(0);
                            if (childType == Type.ARRAY || childType == Type.MAP || childType == Type.UNKNOWN_TYPE) {
                                r.set(node, "type", Type.INTEGER);
                            } else {
                                r.errorFor("Argument of len function must be an array or a map", node, node.attr("type"));
                            }
                        });
                break;
            default:
                break;
        }

    }

    private FunctionDefinitionNode currentFunction() {
        Scope scope = this.scope;
        while (scope != null) {
            ASTNode node = scope.node;
            if (node instanceof FunctionDefinitionNode)
                return (FunctionDefinitionNode) node;
            scope = scope.parent;
        }
        return null;
    }

    private void binaryExpression(BinaryNode node) {
        if (node.isArithmeticOperation()) {
            R.rule(node, "type")
            .using(node.left.attr("type"), node.right.attr("type"))
            .by(r -> {
                Type left = r.get(0);
                Type right = r.get(1);

                if (left == Type.INTEGER && right == Type.INTEGER
                ||  left == Type.UNKNOWN_TYPE || right == Type.UNKNOWN_TYPE)
                    r.set(0, Type.INTEGER);
                else
                    r.error("Arithmetic operation needs 2 integers.", node);
            });
        }
        else if (node.isEqualityComparison()) {
            R.rule(node, "type")
            .using()
            .by(r -> {
                // Comparison will return False if ≠ types are compared, but will not output an error
                r.set(0, Type.BOOLEAN);
            });
        } else if (node.isInequalityComparison()) {
            R.rule(node, "type")
            .using(node.left.attr("type"), node.right.attr("type"))
            .by(r -> {
                Type left = r.get(0);
                Type right = r.get(1);

                if (left == Type.INTEGER && right == Type.INTEGER
                ||  left == Type.STRING && right == Type.STRING
                ||  left == Type.UNKNOWN_TYPE || right == Type.UNKNOWN_TYPE)
                    r.set(0, Type.BOOLEAN);
                else
                    r.error("Inequality comparison can only be used with integers and strings", node);
            });
        } else if (node.isLogicOperation()) {
            R.rule(node, "type")
            .using(node.left.attr("type"), node.right.attr("type"))
            .by(r -> {
                Type left = r.get(0);
                Type right = r.get(1);

                if (left == Type.BOOLEAN && right == Type.BOOLEAN
                ||  left == Type.UNKNOWN_TYPE || right == Type.UNKNOWN_TYPE)
                    r.set(0, Type.BOOLEAN);
                else
                    r.error("Inequality comparison can only be used with integers and strings", node);
             });
        } else if (node.isIdxAccess()) {
            R.rule(node, "type")
            .using(node.left.attr("type"), node.right.attr("type"))
            .by(r -> {
                Type left = r.get(0);
                Type right = r.get(1);

            if (left == Type.ARRAY && right == Type.INTEGER
            ||  left == Type.MAP
            ||  left == Type.UNKNOWN_TYPE || right == Type.UNKNOWN_TYPE)
                r.set(0, Type.UNKNOWN_TYPE);
            else if (left == Type.ARRAY)
                r.error("Arrays can only be indexed with integers", node);
            else
                r.error("Only Arrays and Map objects can be indexed", node);
            });
        }
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

    public void parameter(ParameterNode node) {
        scope.declare(node.param.value, node);
        R.set(node, "type", Type.UNKNOWN_TYPE);
        R.set(node, "scope", scope);
    }

    public void functionDeclaration(FunctionDefinitionNode node) {
        scope.declare(node.name.value, node);
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);
        R.set(node, "type", Type.UNKNOWN_TYPE);
    }

    public void if_(IfNode node) {
        R.rule()
                .using(node.bool, "type")
                .by(r -> {
                    Type conditionType = r.get(0);
                    if (!(conditionType == Type.BOOLEAN || conditionType == Type.UNKNOWN_TYPE)) {
                        r.error("If statement must have a boolean as condition", node);
                    }
                });
    }

    public void while_(WhileNode node) {
        R.rule()
                .using(node.bool, "type")
                .by(r -> {
                    Type conditionType = r.get(0);
                    if (!(conditionType == Type.BOOLEAN || conditionType == Type.UNKNOWN_TYPE)) {
                        r.error("While statement must have a boolean as condition", node);
                    }
                });
    }

    public void for_(ForNode node) {
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);
        R.set(node, "type", Type.UNKNOWN_TYPE);

        R.rule()
                .using(node.list.attr("type"))
                .by(r -> {
                    Type iterableType = r.get(0);
                    if (iterableType != Type.ARRAY && iterableType != Type.UNKNOWN_TYPE) {
                        r.error("For statement must have an array to iterate over", node);
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

                    if ((node.left instanceof BinaryNode && ((BinaryNode) node.left).code == BinaryNode.IDX_ACCESS)) { // variables can be assigned to a new type but not array elements
                        if (!isAssignableTo(right, left))
                            r.errorFor("Trying to assign a value to a non-compatible lvalue.", node);
                    }
                });
    }

    public boolean isAssignableTo(Type right, Type left) {
        return right == Type.NONE || right == Type.UNKNOWN_TYPE || right == left;
    }

    public Type getSuperType(Type a, Type b) {
        if (a == Type.UNKNOWN_TYPE || a == Type.NONE)
            return (b == Type.NONE) ? Type.UNKNOWN_TYPE : b;
        if (b == Type.UNKNOWN_TYPE || b == Type.NONE)
            return a;
        if (a == b) return a;
        return null;
    }


    // endregion
    // =============================================================================================
    // region [Other Statements]
    // =============================================================================================
    // endregion
    // =============================================================================================
}