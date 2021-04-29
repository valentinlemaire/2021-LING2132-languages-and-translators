package interpreter;

import ast.*;
import norswap.uranium.Reactor;
import norswap.utils.Util;
import norswap.utils.exceptions.Exceptions;
import norswap.utils.exceptions.NoStackException;
import norswap.utils.visitors.ValuedVisitor;
import scopes.RootScope;

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
        visitor.register(MapNode.class,                 this::map);
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


    // SCOPES
    private Object root(RootNode n) {
        /* TODO */
        return null;
    }

    private Object block(BlockNode n) {
        /* TODO */
        return null;
    }


    // PRIMITIVE LITERALS
    private Object none(NoneNode n) {
        /* TODO singleton 'None' */
        return null;
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
        /* TODO */
        return null;
    }

    private Object varAssignment(VarAssignmentNode n) {
        /* TODO */
        return null;
    }


    // COLLECTIONS
    private Object map(MapNode n) {
        /* TODO */
        return null;
    }

    private Object array(ArrayNode n) {
        /* TODO */
        return null;
    }


    // OPERATIONS
    private Object unary(UnaryNode n) {
        /* TODO */
        return null;
    }

    private Object binary(BinaryNode n) {
        /* TODO */
        return null;
    }


    // STATEMENTS
    private Object if_(IfNode n) {
        /* TODO */
        return null;
    }

    private Object else_(ElseNode n) {
        /* TODO */
        return null;
    }

    private Object for_(ForNode n) {
        /* TODO */
        return null;
    }

    private Object while_(WhileNode n) {
        /* TODO */
        return null;
    }


    // FUNCTIONS
    private Object functionDefinition(FunctionDefinitionNode n) {
        /* TODO */
        return null;
    }

    private Object functionCall(FunctionCallNode n) {
        /* TODO */
        return null;
    }

    private Object parameter(ParameterNode n) {
        /* TODO */
        return null;
    }
}
