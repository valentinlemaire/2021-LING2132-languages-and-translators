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

import java.util.ArrayList;
import java.util.List;
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
        walker.register(ListComprehensionNode.class,    PRE_VISIT,  analysis::listComprehension);
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
        walker.register(ElseNode.class,                 PRE_VISIT,  analysis::else_);
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

    private void identifier(final IdentifierNode node) {
        final Scope scope = this.scope;

        DeclarationContext maybeCtx = scope.lookup(node.value);

        if (maybeCtx != null) {
            R.set(node, "decl",  maybeCtx.declaration);
            R.set(node, "scope", maybeCtx.scope);

            if (maybeCtx.declaration instanceof ForNode || maybeCtx.declaration instanceof ListComprehensionNode) {
                R.set(node, "type", Type.UNKNOWN_TYPE);
            } else {
                R.rule(node, "type")
                        .using(maybeCtx.declaration, "type")
                        .by(Rule::copyFirst);
            }
            
            return;
        }

        // Re-lookup after the scopes have been built.
        R.rule(node.attr("decl"), node.attr("scope"))
                .by(r -> {
                    DeclarationContext ctx = scope.lookup(node.value);
                    DeclarationNode decl = ctx == null ? null : ctx.declaration;

                    if (ctx == null) {
                        r.errorFor("Could not resolve: " + node.value,
                                node, node.attr("decl"), node.attr("scope"), node.attr("type"));
                    } else {
                        r.set(node, "scope", ctx.scope);
                        r.set(node, "decl", decl);

                        r.errorFor("Variable used before declaration: " + node.value,
                                node, node.attr("type"));

                    }
                });
    }

    private void array(ArrayNode node) {
        if (node.elements != null) {
            R.set(node, "type", Type.ARRAY);

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

    private void listComprehension(ListComprehensionNode node) {
        scope = new Scope(node, scope);
        R.set(node, "scope", scope);

        if (node.condition != null) {
            R.rule(node.attr("type"))
                    .using(node.iterable.attr("type"), node.condition.attr("type"))
                    .by(r -> {
                        Type iterableType = r.get(0);
                        if (!(iterableType == Type.ARRAY || iterableType == Type.UNKNOWN_TYPE)) {
                            r.errorFor("List comprehension must iterate through an array", node, node.attr("type"));
                            return;
                        }
                        Type conditionType = r.get(1);
                        if (!(conditionType == Type.BOOLEAN || conditionType == Type.UNKNOWN_TYPE)) {
                            r.errorFor("List comprehension must have boolean as filter condition", node, node.attr("type"));
                            return;
                        }
                        r.set(node, "type", Type.ARRAY);
                    });
        } else {
            R.rule(node.attr("type"))
                    .using(node.iterable.attr("type"))
                    .by(r -> {
                        Type iterableType = r.get(0);
                        if (!(iterableType == Type.ARRAY || iterableType == Type.UNKNOWN_TYPE)) {
                            r.errorFor("List comprehension must iterate through an array", node, node.attr("type"));
                            return;
                        }
                        r.set(node, "type", Type.ARRAY);
                    });
        }
        scope.declare(node.variable.value, node);


    }

    private void map(MapNode node) {
        if (node.elements != null) {
            Attribute[] deps  =
                    node.elements.stream().map(it -> it.left .attr("type")).toArray(Attribute[]::new);

            R.rule(node.attr("type"))
                    .using(deps)
                    .by(r -> {
                        r.set(node, "type", Type.MAP);

                        for (int i = 0; i < deps.length; i++) {
                            Type type = r.get(i);
                            if (!(type == Type.BOOLEAN || type == Type.STRING || type == Type.INTEGER || type == Type.UNKNOWN_TYPE)) {
                                r.errorFor("keys of a map must be strings, integers or booleans", node, node.attr("type"));
                                return;
                            }
                        }
                    });
        } else {
            R.set(node, "type", Type.MAP);
        }
    }



    private void functionCall(FunctionCallNode node) {

        final Scope scope = this.scope;

        R.rule(node, "type")
                .using()
                .by(r -> {


                    DeclarationContext maybeCtx = scope.lookup(node.functionName.value);

                    if (maybeCtx != null) {
                        List<ParameterNode> params = null;
                        if (maybeCtx.declaration instanceof FunctionDefinitionNode) {
                            params = ((FunctionDefinitionNode) maybeCtx.declaration).args;
                            r.set(0, Type.UNKNOWN_TYPE);
                        } else if (maybeCtx.declaration instanceof SyntheticDeclarationNode) {
                            params = ((SyntheticDeclarationNode) maybeCtx.declaration).getParameters();
                            builtin(node, r);
                        }
                        List<ASTNode> args = node.args;

                        if ((params == null && args != null) || (args == null && params != null) || params.size() != args.size())
                            r.errorFor("Wrong number of arguments, expected "+params.size()+" but got "+args.size(),
                                    node);
                    } else {
                        r.errorFor("Calling function that was not declared", node, node.attr("type"));
                    }

                });
    }

    private void builtin(FunctionCallNode node, Rule rule) {
        switch (node.functionName.value) {
            case "range":
                rule.set(node, "type", Type.ARRAY);
                R.rule()
                        .using(node.args.get(0).attr("type"))
                        .by(r -> {
                            Type argType = r.get(0);
                            if (!(argType == Type.INTEGER || argType == Type.UNKNOWN_TYPE)) {
                                r.errorFor("Argument of range function must be an integer", node);
                            }
                        });
                break;
            case "indexer":
                rule.set(node, "type", Type.ARRAY);
                R.rule()
                        .using(node.args.get(0).attr("type"))
                        .by(r -> {
                            Type nodeType = r.get(0);
                            if (!(nodeType == Type.ARRAY || nodeType == Type.MAP || nodeType == Type.UNKNOWN_TYPE)) {
                                r.errorFor("Argument of indexer function must be an array or a map", node);
                            }
                        });
                break;
            case "sort":
                rule.set(node, "type", Type.ARRAY);
                R.rule()
                        .using(node.args.get(0).attr("type"))
                        .by(r -> {
                            Type nodeType = r.get(0);
                            if (!(nodeType == Type.ARRAY || nodeType == Type.UNKNOWN_TYPE)) {
                                r.errorFor("Argument of sort function must be an array", node);
                            }
                        });
                break;
            case "int":
                rule.set(node, "type", Type.INTEGER);
                R.rule()
                        .using(node.args.get(0).attr("type"))
                        .by(r -> {
                            Type childType = r.get(0);
                            if (!(childType == Type.STRING || childType == Type.UNKNOWN_TYPE)) {
                                r.errorFor("Argument of int function must be a string", node);
                            }
                        });
                break;
            case "print":
            case "println":
                rule.set(node, "type", Type.NONE);
                break;
            case "len":
                rule.set(node, "type", Type.INTEGER);
                R.rule()
                        .using(node.args.get(0).attr("type"))
                        .by(r -> {
                            Type childType = r.get(0);
                            if (!(childType == Type.ARRAY || childType == Type.MAP || childType == Type.UNKNOWN_TYPE)) {
                                r.errorFor("Argument of len function must be an array or a map", node);
                            }
                        });
                break;
            case "open":
            case "write":
            case "read":
            case "close":
                rule.set(node, "type", Type.UNKNOWN_TYPE);
                break;
            default:
                break;

        }
    }

    private void unaryExpression(final UnaryNode node) {
        switch (node.code) {
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
                    r.error("Logic operations can only be done on booleans", node);
             });
        } else if (node.isIdxAccess()) {
            R.rule(node, "type")
            .using(node.left.attr("type"), node.right.attr("type"))
            .by(r -> {
                Type left = r.get(0);
                Type right = r.get(1);

                if (left == Type.ARRAY && right == Type.INTEGER
                ||  (left == Type.MAP && right != Type.NONE)
                ||  left == Type.UNKNOWN_TYPE && right != Type.NONE || right == Type.UNKNOWN_TYPE)
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
        DeclarationContext declCtx = scope.lookup(node.name.value);
        Scope s = new Scope(node, scope);
        if (declCtx == null) {
            scope.declare(node.name.value, node);
            scope = s;
        }

        R.rule(node.attr("scope"), node.attr("type"))
                .by(r -> {
                    if (declCtx == null) {
                        r.set(node, "scope", s);
                        r.set(node, "type", Type.UNKNOWN_TYPE);
                    } else {
                        r.errorFor("Function name already declared in this scope", node, node.attr("scope"), node.attr("type"));
                    }
                });
    }

    public void if_(IfNode node) {
        if (node.else_blocks != null) {
            R.rule()
                    .by(r -> {
                        for (int i = 0; i < node.else_blocks.size() - 1; i++) {
                            if (node.else_blocks.get(i).bool == null) {
                                r.errorFor("Cannot have elsif or else blocks after an else block", node);
                            }
                        }
                    });
        }


        R.rule()
                .using(node.bool, "type")
                .by(r -> {
                    Type conditionType = r.get(0);
                    if (!(conditionType == Type.BOOLEAN || conditionType == Type.UNKNOWN_TYPE)) {
                        r.error("If statement must have a boolean as condition", node);
                    }
                });
    }

    public void else_(ElseNode node) {
        if (node.bool != null) {
            R.rule()
                    .using(node.bool, "type")
                    .by(r -> {
                        Type conditionType = r.get(0);
                        if (!(conditionType == Type.BOOLEAN || conditionType == Type.UNKNOWN_TYPE)) {
                            r.error("elsif statement must have a boolean as condition", node);
                        }
                    });
        }
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
        DeclarationContext maybeCtx;
        if (node.left instanceof IdentifierNode) {
            maybeCtx = scope.lookup(((IdentifierNode) node.left).value);

            if (maybeCtx == null || maybeCtx.declaration instanceof FunctionDefinitionNode) {
                scope.declare(((IdentifierNode) node.left).value, node);
                R.set(node, "scope", scope);

                R.rule(node, "type")
                        .using(node.right.attr("type"))
                        .by(Rule::copyFirst);
                return;
            }
        } else if (node.left instanceof BinaryNode && ((BinaryNode) node.left).code == BinaryNode.IDX_ACCESS) {
            maybeCtx = scope.lookup(((IdentifierNode) ((BinaryNode) node.left).left).value);
        } else {
            maybeCtx = null;
        }

        R.rule(node, "type")
                .using(node.right.attr("type"))
                .by(r -> {
                    if (maybeCtx != null && maybeCtx.declaration instanceof VarAssignmentNode) {
                        if (((VarAssignmentNode) maybeCtx.declaration).final_) {
                            r.errorFor("Cannot assign to final variable", node, node.attr("type"));
                            return;
                        }
                    }
                    Type right = r.get(0);

                    r.set(node, "type", right); // the type of the assignment is the right-side type
                });
    }


    // endregion
    // =============================================================================================
    // region [Other Statements]
    // =============================================================================================
    // endregion
    // =============================================================================================
}