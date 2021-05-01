package interpreter;

import ast.*;
import norswap.uranium.Reactor;
import norswap.utils.Util;
import norswap.utils.exceptions.Exceptions;
import norswap.utils.exceptions.NoStackException;
import norswap.utils.visitors.ValuedVisitor;
import scopes.RootScope;
import scopes.Scope;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static norswap.utils.Util.cast;
import static norswap.utils.Vanilla.coIterate;
import static norswap.utils.Vanilla.map;


public final class Interpreter {
    // ---------------------------------------------------------------------------------------------

    private final ValuedVisitor<ASTNode, Object> visitor = new ValuedVisitor<>();
    private final Reactor reactor;
    private ScopeStorage storage = null;
    private RootScope rootScope;
    private ScopeStorage rootStorage;

    // ---------------------------------------------------------------------------------------------

    public Interpreter(Reactor reactor) {
        this.reactor = reactor;

        // SCOPES
        visitor.register(RootNode.class,                this::root);
        visitor.register(BlockNode.class,               this::block);

        // PRIMITIVE LITERALS
        visitor.register(NoneNode.class,                this::none);
        visitor.register(BoolNode.class,                this::bool);
        visitor.register(IntegerNode.class,             this::integer);
        visitor.register(StringNode.class,              this::string);

        // VARIABLES
        visitor.register(IdentifierNode.class,          this::identifier);
        visitor.register(VarAssignmentNode.class,       this::varAssignment);

        // COLLECTIONS
        visitor.register(MapNode.class,                 this::map_);
        visitor.register(ArrayNode.class,               this::array);

        // OPERATIONS
        visitor.register(UnaryNode.class,               this::unary);
        visitor.register(BinaryNode.class,              this::binary);

        // STATEMENTS
        visitor.register(IfNode.class,                  this::if_);
        visitor.register(ElseNode.class,                this::else_);
        visitor.register(ForNode.class,                 this::for_);
        visitor.register(WhileNode.class,               this::while_);

        // FUNCTIONS
        visitor.register(FunctionDefinitionNode.class,  this::functionDefinition);
        visitor.register(FunctionCallNode.class,        this::functionCall);
        visitor.register(ParameterNode.class,           this::parameter);

        visitor.registerFallback(node -> null);
    }

    public Object interpret(ASTNode root) {
        try {
            return run(root);
        } catch (PassthroughException e) {
            throw Exceptions.runtime(e.getCause());
        }
    }

    public Object run(ASTNode node) {
        try {
            return visitor.apply(node);
        } catch (InterpreterException | PassthroughException | Return e) {
            throw e;
        } catch (RuntimeException e) {
            throw new InterpreterException("exception while executing " + node, e);
        }
    }

    private static class Return extends NoStackException {
        final Object value;
        private Return (Object value) {
            this.value = value;
        }
    }

    private <T> T get(ASTNode node) {
        return cast(run(node));
    }

    private boolean isPrimitive(Object o) {
        return o instanceof String || o instanceof Integer || o instanceof Boolean;
    }


    // SCOPES
    private Object root(RootNode n) {
        assert storage == null;

        rootScope = reactor.get(n, "scope");
        storage = rootStorage = new ScopeStorage(rootScope, null);
        storage.initRoot(rootScope);

        try {
            n.block.statements.forEach(this::run);
        } catch (Return r) {
            return r.value;
            // allow returning from the main script
        } finally {
            storage = null;
        }
        return null;
    }

    private Object block(BlockNode n) {
        Scope scope = reactor.get(n, "scope");
        storage = new ScopeStorage(scope, storage);
        n.statements.forEach(this::run);
        storage = storage.parent;
        return null;
    }


    // PRIMITIVE LITERALS
    private Object none(NoneNode n) {
        return None.INSTANCE;
    }

    private Boolean bool(BoolNode n) {
        return n.value;
    }

    private Integer integer(IntegerNode n) {
        return n.value;
    }

    private String string(StringNode n) {
        return n.value;
    }


    // VARIABLES
    private Object identifier(IdentifierNode n) {
        /* TODO Val */
        return null;
    }

    private Object varAssignment(VarAssignmentNode n) {
        /* TODO Val */
        return null;
    }


    // COLLECTIONS
    private Object map_(MapNode n) {
        HashMap<Object, Object> dictionary = new HashMap<>();
        if (n.elements != null) {
            for (BinaryNode pair : n.elements) {
                if (isPrimitive(get(pair.left))) {
                    dictionary.put(get(pair.left), get(pair.right));
                } else {
                    throw new PassthroughException(new RuntimeException("Cannot use "+get(pair.left)+" as key in map"));
                }
            }
        }
        return dictionary;
    }

    private Object array(ArrayNode n) {
        if (n.elements != null) {
            return map(n.elements, new Object[0], visitor);
        } else if (n.size != null) {
            return new Object[(int) get(n.size)];
        }

        // Should not happen
        throw new PassthroughException(new RuntimeException("Illegal declaration of array "+n));
    }


    // OPERATIONS
    private Object unary(UnaryNode n) {
        /* TODO Val */
        return null;
    }

    private Object binary(BinaryNode n) {
        /* TODO Gus */
        return null;
    }


    // STATEMENTS
    private Object if_(IfNode n) {
        /* TODO Val */
        return null;
    }

    private Object else_(ElseNode n) {
        /* TODO Val */
        return null;
    }

    private Object for_(ForNode n) {
        /* TODO Val */
        return null;
    }

    private Object while_(WhileNode n) {
        /* TODO Gus */
        return null;
    }


    // FUNCTIONS
    private Object functionDefinition(FunctionDefinitionNode n) {
        /* TODO Gus  */
        return null;
    }

    private Object functionCall(FunctionCallNode n) {
        /* TODO  Gus */
        return null;
    }

    private Object parameter(ParameterNode n) {
        /* TODO Gus */
        return null;
    }
}
