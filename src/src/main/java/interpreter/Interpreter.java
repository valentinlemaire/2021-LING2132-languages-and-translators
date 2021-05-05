package interpreter;

import ast.*;
import norswap.uranium.Reactor;
import norswap.utils.exceptions.Exceptions;
import norswap.utils.exceptions.NoStackException;
import norswap.utils.visitors.ValuedVisitor;
import scopes.DeclarationKind;
import scopes.RootScope;
import scopes.Scope;
import scopes.SyntheticDeclarationNode;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private String convertToString (Object arg) {
        if (arg == None.INSTANCE)
            return "None";
        else if (arg instanceof Object[])
            return Arrays.deepToString((Object[]) arg);
        else if (arg instanceof FunctionDefinitionNode)
            return ((FunctionDefinitionNode) arg).name.value;
        else if (arg instanceof HashMap)
            return "{"+((HashMap<?, ?>) arg).entrySet().stream().map((e) -> convertToString(e.getKey()) + ":" + convertToString(e.getValue())).collect(Collectors.joining(", ")) + "}";
        else
            return arg.toString();
    }

    private String type(Object arg) {
        if (arg instanceof Integer)  return "int";
        if (arg instanceof Boolean)  return "bool";
        if (arg instanceof String)   return "string";
        if (arg instanceof Object[]) return "array";
        if (arg instanceof HashMap)  return "map";
        return "unknown type";
    }


    // SCOPES
    private Object root(RootNode n) {
        assert storage == null;

        rootScope = reactor.get(n, "scope");
        storage = rootStorage = new ScopeStorage(rootScope, null);
        storage.initRoot(rootScope);

        try {
            Object res = null;
            for (ASTNode statement : n.block.statements) {
                res = get(statement);
            }
            return res;
        } catch (Return r) {
            // TODO: claqué
            return r.value;
            // allow returning from the main script
        } finally {
            storage = null;
        }
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
        Scope scope = reactor.get(n, "scope");
        DeclarationNode decl = reactor.get(n, "decl");

        if (decl instanceof VarAssignmentNode
                || decl instanceof ParameterNode
                || decl instanceof SyntheticDeclarationNode
                && ((SyntheticDeclarationNode) decl).kind() == DeclarationKind.VARIABLE)
            return scope == rootScope
                    ? rootStorage.get(scope, n.value)
                    : storage.get(scope, n.value);

        return decl;
    }

    private Object varAssignment(VarAssignmentNode n) {
        if (n.left instanceof IdentifierNode) {
            Scope scope = reactor.get(n.left, "scope");
            String name = ((IdentifierNode) n.left).value;
            Object rvalue = get(n.right);
            storage.set(scope, name, rvalue);
            return rvalue;
        }

        if (n.left instanceof BinaryNode && ((BinaryNode) n.left).code == BinaryNode.IDX_ACCESS) {
            BinaryNode arrayAccess = (BinaryNode) n.left;
            Object indexable = get(arrayAccess.left);
            if (indexable == None.INSTANCE) {
                throw new PassthroughException(new NullPointerException("Indexing null array " + n.left));
            } else if (indexable instanceof Object[]) {
                Object[] array = (Object[]) indexable;
                int index = getIndex(arrayAccess.right);
                try {
                    array[index] = get(n.right);
                    return null;
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new PassthroughException(e);
                }
            } else if (indexable instanceof HashMap) {
                HashMap<Object, Object> map = (HashMap<Object, Object>) indexable;
                Object indexer = get(arrayAccess.right);
                if (isPrimitive(indexer)) {
                    map.put(indexer, get(n.right));
                } else {
                    throw new PassthroughException(new RuntimeException("Can only use string, integers and booleans as keys of a map, not "+type(indexer)));
                }
                return null;
            }
        }

        throw new Error("should not reach here");
    }

    private int getIndex (ASTNode node) {
        long index = get(node);
        if (index < 0)
            throw new ArrayIndexOutOfBoundsException("Negative index: " + index);
        if (index >= Integer.MAX_VALUE - 1)
            throw new ArrayIndexOutOfBoundsException("Index exceeds max array index (2ˆ31 - 2): " + index);
        return (int) index;
    }




    // COLLECTIONS
    private Object map_(MapNode n) {
        HashMap<Object, Object> dictionary = new HashMap<>();
        if (n.elements != null) {
            for (BinaryNode pair : n.elements) {
                if (isPrimitive(get(pair.left))) {
                    dictionary.put(get(pair.left), get(pair.right));
                } else {
                    throw new PassthroughException(new RuntimeException("Cannot use "+type(get(pair.left))+" as key in map"));
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

        throw new Error("should not reach here");
    }


    // OPERATIONS
    private Object unary(UnaryNode n) {
        Object arg;
        switch (n.code) {
            case UnaryNode.RANGE:
                arg = get(n.child);
                if (arg instanceof Integer) {
                    return IntStream.range(0, (int) arg - 1).toArray();
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of range function must be an integer, not " + type(arg)));
                }


            case UnaryNode.INDEXER:
                arg = get(n.child);
                if (arg instanceof Object[]) {
                    Object[] array = (Object[]) arg;
                    return IntStream.range(0, array.length - 1).toArray();
                } else if (arg instanceof HashMap) {
                    HashMap<Object, Object> map = (HashMap<Object, Object>) arg;
                    return map.keySet().toArray();
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of range function must be a map or an array, not " + type(arg)));
                }
            case UnaryNode.SORT:
                arg = get(n.child);
                if (arg instanceof Object[]) {
                    Object[] original = (Object[]) arg;
                    Object[] copy = original.clone();
                    try {
                        Arrays.sort(copy);
                        return copy;
                    } catch (ClassCastException e) {
                        throw new PassthroughException(e);
                    }
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of sort function must be an array, not "+type(arg)));
                }
            case UnaryNode.PARSE_INT:
                arg = get(n.child);
                if (arg instanceof String) {
                    String s = (String) arg;
                    return Integer.parseInt(s);
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of int function must be a string, not " + type(arg)));
                }
            case UnaryNode.PRINT:
                arg = get(n.child);
                String out = convertToString(arg);
                System.out.print(out);
                return None.INSTANCE;
            case UnaryNode.PRINTLN:
                arg = get(n.child);
                String outln = convertToString(arg);
                System.out.println(outln);
                return None.INSTANCE;
            case UnaryNode.NEGATION:
                arg = get(n.child);
                if (arg instanceof Integer) {
                    return - (int) arg;
                } else {
                    throw new PassthroughException(new RuntimeException("Cannot negate a non-int value " + type(arg)));
                }
            case UnaryNode.NOT:
                arg = get(n.child);
                if (arg instanceof Boolean) {
                    return !((boolean) arg);
                } else {
                    throw new PassthroughException(new RuntimeException("Cannot apply not operator on a non-boolean value " + type(arg)));
                }
            case UnaryNode.RETURN:
                throw new Return(n.child == null ? None.INSTANCE : get(n.child));
            case UnaryNode.LEN:
                arg = get(n.child);
                if (arg instanceof Object[]) {
                    Object[] array = (Object[]) arg;
                    return array.length;
                } else if (arg instanceof HashMap) {
                    HashMap<Object, Object> map = (HashMap<Object, Object>) arg;
                    return map.size();
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of len function must be an array or a map, not" +type(arg)));
                }
            default:
                return null;
        }
    }

    private Object binary(BinaryNode n) {
        Object arg;
        if (n.isArithmeticOperation()) {
            return arithmeticOperation(n);
        } else if (n.isLogicOperation()) {
            return logicOperation(n);
        } else if (n.isEqualityComparison()) {
            return equalityComparison(n);
        } else if (n.isInequalityComparison()) {
            return inequalityComparison(n);
        } else if (n.isPair()) {
            return pair(n);
        } else if (n.isIdxAccess()) {
            return idxAccess(n);
        }
        throw new Error("Should not get here");
    }

    private Object arithmeticOperation(BinaryNode n) {
        Object leftObject  = get(n.left);
        Object rightObject = get(n.left);

        if (!(leftObject instanceof Integer && rightObject instanceof Integer)) {
            throw new PassthroughException(new ClassCastException("Cannot do arithmetic operations on "
                                                                 + type(leftObject) + " and "
                                                                 + type(rightObject) + "."));
        }

        Integer left  = (Integer) leftObject;
        Integer right = (Integer) rightObject;

        switch (n.code) {
            case BinaryNode.ADD:
                return left + right;
            case BinaryNode.SUB:
                return left - right;
            case BinaryNode.MUL:
                return left * right;
            case BinaryNode.DIV:
                return left / right;
            case BinaryNode.MOD:
                return left % right;
        }

        throw new Error("Should not get here");
    }

    private Object logicOperation(BinaryNode n) {
        Object leftObject  = get(n.left);
        Object rightObject = get(n.left);

        if (!(leftObject instanceof Boolean && rightObject instanceof Boolean)) {
            throw new PassthroughException(new ClassCastException("Cannot do logic operations on "
                                                                 + type(leftObject) + " and "
                                                                 + type(rightObject) + "."));
        }

        Boolean left  = (Boolean) leftObject;
        Boolean right = (Boolean) rightObject;

        switch (n.code) {
            case BinaryNode.OR:
                return left || right;
            case BinaryNode.AND:
                return left && right;
        }

        throw new Error("Should not get here");
    }

    private Object equalityComparison(BinaryNode n) {
        switch (n.code) {
            case BinaryNode.EQ:
                return get(n.left).equals(get(n.right));
            case BinaryNode.NEQ:
                return !get(n.left).equals(get(n.right));
        }

        throw new Error("Should not get here");
    }

    private Object inequalityComparison(BinaryNode n) {
        Object leftObject  = get(n.left);
        Object rightObject = get(n.left);

        int comparison;

        if (leftObject instanceof Integer && rightObject instanceof Integer) {
            Integer left  = (Integer) leftObject;
            Integer right = (Integer) rightObject;

            comparison = left.compareTo(right);

        } else if (leftObject instanceof String && rightObject instanceof String) {
            String left  = (String) leftObject;
            String right = (String) rightObject;

            comparison = left.compareTo(right);

        } else {
            throw new PassthroughException(new ClassCastException("Cannot do inequality comparisons on "
                    + type(leftObject) + " and "
                    + type(rightObject) + "."));
        }

        switch (n.code) {
            case BinaryNode.LEQ:
                return comparison <= 0;
            case BinaryNode.GEQ:
                return comparison >= 0;
            case BinaryNode.L:
                return comparison < 0;
            case BinaryNode.G:
                return comparison > 0;
        }

        throw new Error("Should not get here");
    }

    private Object pair(BinaryNode n) {
        throw new Error("Should not get here");
    }

    private Object idxAccess(BinaryNode n) {
        Object leftObject  = get(n.left);
        Object rightObject = get(n.right);

        if (leftObject instanceof Object[] && rightObject instanceof Integer) {
            Object[] list = (Object[]) leftObject;
            Integer idx = (Integer) rightObject;

            return list[idx];
        } else if (leftObject instanceof HashMap) {
            HashMap<Object, Object> map = (HashMap<Object, Object>) leftObject;
            /* todo use isprimitive() to check type of keys */
            /* todo raise passthrough exception if invalid key */
            /* TODO ?? indexing with strings won't work since Object.compareTo will compare references */
        }
        return null;
        /*TODO GUS*/
    }


    // STATEMENTS
    private Object if_(IfNode n) {
        Object arg = get(n.bool);

        if (!(arg instanceof Boolean))
            throw new PassthroughException(new RuntimeException("If statement needs boolean condition, not " + type(arg)));

        if (get(n.bool))
            get(n.block);

        else if (n.else_blocks != null) {
            for (ElseNode elseNode : n.else_blocks) {
                if (elseNode.bool == null) {
                    get(elseNode.block);
                    break;
                }

                arg = get(n.bool);

                if (!(arg instanceof Boolean))
                    throw new PassthroughException(new RuntimeException("Elsif statement needs boolean condition, not " + type(arg)));

                if (get(elseNode.bool)) {
                    get(elseNode.block);
                    break;
                }
            }
        }
        return null;
    }

    private Object else_(ElseNode n) {
        throw new Error("Should not get here");
    }

    private Object for_(ForNode n) {
        ScopeStorage oldStorage = storage;
        Scope scope = reactor.get(n, "scope");
        storage = new ScopeStorage(scope, storage);

        Object arg = get(n.list);
        if (!(arg instanceof Object[])) throw new PassthroughException(new RuntimeException("Cannot iterate over "+type(arg)));

        Object[] array = (Object[]) arg;
        for (Object elem : array) {
            storage.set(scope, n.variable.value, elem);
            get(n.block);
        }

        storage = oldStorage;
        return null;
    }

    private Object while_(WhileNode n) {
        ScopeStorage oldStorage = storage;
        Scope scope = reactor.get(n, "scope");
        storage = new ScopeStorage(scope, storage);

        Object arg = get(n.bool);
        if (!(arg instanceof Boolean))
            throw new PassthroughException(new RuntimeException("While loop needs boolean condition, not " + type(arg)));

        while (get(n.bool)) {
            get(n.block);
        }

        storage = oldStorage;
        return null;
    }


    // FUNCTIONS
    private Object functionDefinition(FunctionDefinitionNode n) {
        /* TODO Gus  */
        return null;
    }

    private Object functionCall(FunctionCallNode n) {
        /* TODO  Gus
        *   une fonction sans return doit retourner None*/
        return null;
    }

    private Object parameter(ParameterNode n) {
        /* TODO Gus */
        return null;
    }
}
