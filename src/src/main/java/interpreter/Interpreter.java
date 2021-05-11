package interpreter;

import Types.File;
import Types.PolymorphArray;
import Types.PolymorphMap;
import ast.*;
import norswap.uranium.Reactor;
import norswap.utils.exceptions.Exceptions;
import norswap.utils.exceptions.NoStackException;
import norswap.utils.visitors.ValuedVisitor;
import scopes.DeclarationKind;
import scopes.RootScope;
import scopes.Scope;
import scopes.SyntheticDeclarationNode;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;

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

    private String[] args;

    // ---------------------------------------------------------------------------------------------

    public Interpreter(Reactor reactor, String[] args) {
        this.reactor = reactor;
        this.args = args;

        // SCOPES
        visitor.register(RootNode.class, this::root);
        visitor.register(BlockNode.class, this::block);

        // PRIMITIVE LITERALS
        visitor.register(NoneNode.class, this::none);
        visitor.register(BoolNode.class, this::bool);
        visitor.register(IntegerNode.class, this::integer);
        visitor.register(StringNode.class, this::string);

        // VARIABLES
        visitor.register(IdentifierNode.class, this::identifier);
        visitor.register(VarAssignmentNode.class, this::varAssignment);

        // COLLECTIONS
        visitor.register(MapNode.class, this::map_);
        visitor.register(ArrayNode.class, this::array);
        visitor.register(ListComprehensionNode.class, this::listComprehension);

        // OPERATIONS
        visitor.register(UnaryNode.class, this::unary);
        visitor.register(BinaryNode.class, this::binary);

        // STATEMENTS
        visitor.register(IfNode.class, this::if_);
        visitor.register(ElseNode.class, this::else_);
        visitor.register(ForNode.class, this::for_);
        visitor.register(WhileNode.class, this::while_);

        // FUNCTIONS
        visitor.register(FunctionCallNode.class, this::functionCall);
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

        private Return(Object value) {
            this.value = value;
        }
    }

    private <T> T get(ASTNode node) {
        return cast(run(node));
    }

    private boolean isPrimitive(Object o) {
        return o instanceof String || o instanceof Long || o instanceof Boolean;
    }

    public static String convertToString(Object arg) {
        if (arg instanceof FunctionDefinitionNode)
            return ((FunctionDefinitionNode) arg).name.value;
        else if (arg instanceof Boolean)
            return (boolean) arg ? "True" : "False";
        else
            return arg.toString();
    }

    public static String recConvertToString(Object arg) {
        if (arg instanceof String)
            return "\"" + arg + "\"";
        return convertToString(arg);
    }

    private String type(Object arg) {
        if (arg instanceof Long) return "int";
        if (arg instanceof Boolean) return "bool";
        if (arg instanceof String) return "string";
        if (arg instanceof PolymorphArray) return "array";
        if (arg instanceof PolymorphMap) return "map";
        if (arg instanceof File) return "file";
        if (arg instanceof None) return "None";
        return "unknown type";
    }


    // SCOPES
    private Object root(RootNode n) {
        assert storage == null;

        rootScope = reactor.get(n, "scope");
        storage = rootStorage = new ScopeStorage(rootScope, null);
        storage.initRoot(rootScope, args);

        Object res = get(n.block);
        storage = null;
        return cast(res);
    }

    private Object block(BlockNode n) {
        Scope scope = reactor.get(n, "scope");
        storage = new ScopeStorage(scope, storage);
        Object res = null;
        for (ASTNode statement : n.statements) {
            res = get(statement);
        }
        storage = storage.parent;
        return res;
    }


    // PRIMITIVE LITERALS
    private Object none(NoneNode n) {
        return None.INSTANCE;
    }

    private Boolean bool(BoolNode n) {
        return n.value;
    }

    private Long integer(IntegerNode n) {
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
                || decl instanceof ForNode
                || decl instanceof ListComprehensionNode
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
            } else if (indexable instanceof PolymorphArray) {
                PolymorphArray array = (PolymorphArray) indexable;
                int index = getIndex(arrayAccess.right);
                try {
                    array.set(index, get(n.right));
                    return null;
                } catch (ArrayIndexOutOfBoundsException e) {
                    throw new PassthroughException(e);
                }
            } else if (indexable instanceof PolymorphMap) {
                PolymorphMap map = (PolymorphMap) indexable;
                Object indexer = get(arrayAccess.right);
                if (isPrimitive(indexer)) {
                    map.put(indexer, get(n.right));
                } else {
                    throw new PassthroughException(new RuntimeException("Can only use string, integers and booleans as keys of a map, not " + type(indexer)));
                }
                return null;
            }
        }

        throw new Error("Should not get here");
    }

    private int getIndex(ASTNode node) {
        long index = get(node);
        if (index < 0)
            throw new ArrayIndexOutOfBoundsException("Negative index: " + index);
        if (index >= Integer.MAX_VALUE - 1)
            throw new ArrayIndexOutOfBoundsException("Index exceeds max array index (2ˆ31 - 2): " + index);
        return (int) index;
    }


    // COLLECTIONS
    private Object map_(MapNode n) {
        PolymorphMap dictionary = new PolymorphMap();
        if (n.elements != null) {
            for (BinaryNode pair : n.elements) {
                if (isPrimitive(get(pair.left))) {
                    dictionary.put(get(pair.left), get(pair.right));
                } else {
                    throw new PassthroughException(new RuntimeException("Cannot use " + type(get(pair.left)) + " as key in map"));
                }
            }
        }
        return dictionary;
    }

    private Object array(ArrayNode n) {
        if (n.elements != null) {
            return new PolymorphArray(map(n.elements, new Object[0], visitor));
        } else if (n.size != null) {
            int arg = getIndex(n.size);
            return new PolymorphArray(LongStream.range(0, arg).mapToObj((x) -> None.INSTANCE).toArray());
        }

        throw new Error("Should not get here");
    }

    private Object listComprehension(ListComprehensionNode n) {
        Object list = get(n.iterable);
        if (!(list instanceof PolymorphArray))
            throw new PassthroughException(new RuntimeException("List comprehension must iterate over an array, not "+type(list)));
        List<Object> result = new ArrayList<>();

        ScopeStorage oldStorage = storage;
        Scope scope = reactor.get(n, "scope");
        storage = new ScopeStorage(scope, storage);

        PolymorphArray array = (PolymorphArray) list;
        for (Object elem : array) {
            storage.set(scope, n.variable.value, elem);
            if (n.condition != null) {
                Object condition = get(n.condition);
                if (!(condition instanceof Boolean))
                    throw new PassthroughException(new RuntimeException("Filter condition in list comprehension must be a boolean not "+type(condition)));

                if ((boolean) condition) {
                    result.add(get(n.expression));
                }
            } else {
                result.add(get(n.expression));
            }
        }

        storage = oldStorage;
        return new PolymorphArray(result.toArray());

    }


    // OPERATIONS
    private Object unary(UnaryNode n) {
        Object arg;
        switch (n.code) {

            case UnaryNode.NEGATION:
                arg = get(n.child);
                if (arg instanceof Long) {
                    return - (long) arg;
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
        Object leftObject = get(n.left);
        Object rightObject = get(n.right);

        if (!(leftObject instanceof Long && rightObject instanceof Long)) {
            throw new PassthroughException(new ClassCastException("Cannot do arithmetic operations on "
                    + type(leftObject) + " and "
                    + type(rightObject) + "."));
        }

        Long left = (Long) leftObject;
        Long right = (Long) rightObject;

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
        Object leftObject = get(n.left);
        Object rightObject = get(n.right);

        if (!(leftObject instanceof Boolean && rightObject instanceof Boolean)) {
            throw new PassthroughException(new ClassCastException("Cannot do logic operations on "
                    + type(leftObject) + " and "
                    + type(rightObject) + "."));
        }

        Boolean left = (Boolean) leftObject;
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
        Object leftObject = get(n.left);
        Object rightObject = get(n.right);

        int comparison;

        if (leftObject instanceof Long && rightObject instanceof Long) {
            Long left = (Long) leftObject;
            Long right = (Long) rightObject;

            comparison = left.compareTo(right);

        } else if (leftObject instanceof String && rightObject instanceof String) {
            String left = (String) leftObject;
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
        Object leftObject = get(n.left);
        Object rightObject = get(n.right);

        if (leftObject instanceof PolymorphArray && rightObject instanceof Long) {
            PolymorphArray list = (PolymorphArray) leftObject;
            int idx = getIndex(n.right);

            try {
                return list.get(idx);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw new PassthroughException(e);
            }
        } else if (leftObject instanceof PolymorphMap && isPrimitive(rightObject)) {
            PolymorphMap map = (PolymorphMap) leftObject;
            try {
                return map.get(rightObject);
            } catch (RuntimeException e) {
                throw new PassthroughException(e);
            }
        }
        // Errors in case of invalid types
        if (leftObject instanceof PolymorphMap)
            throw new PassthroughException(new ClassCastException(type(rightObject) + " cannot index a map"));

        else if (leftObject instanceof PolymorphArray)
            throw new PassthroughException(new ClassCastException(type(rightObject) + " cannot index an array"));

        else
            throw new PassthroughException(new ClassCastException("Only array and map can be indexed, not " + type(leftObject)));
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
        if (!(arg instanceof PolymorphArray))
            throw new PassthroughException(new RuntimeException("Cannot iterate over " + type(arg)));

        PolymorphArray array = (PolymorphArray) arg;
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

        while ((boolean) arg) {
            get(n.block);
            arg = get(n.bool);
            if (!(arg instanceof Boolean))
                throw new PassthroughException(new RuntimeException("While loop needs boolean condition, not " + type(arg)));
        }

        storage = oldStorage;
        return null;
    }


    // FUNCTIONS
    private Object functionCall(FunctionCallNode n) {
        Object decl = get(n.functionName);

        n.args.forEach(this::run);
        Object[] args = map(n.args, new Object[0], visitor);

        if (decl == None.INSTANCE)
            throw new PassthroughException(new NullPointerException("calling a null function"));


        //
        if (decl instanceof SyntheticDeclarationNode) {
            SyntheticDeclarationNode declNode = (SyntheticDeclarationNode) decl;
            return builtin(declNode.name(), args);
        }

        ScopeStorage oldStorage = storage;
        Scope scope = reactor.get(decl, "scope");
        storage = new ScopeStorage(scope, storage);

        FunctionDefinitionNode funDecl = (FunctionDefinitionNode) decl;

        coIterate(args, funDecl.args,
                (arg, param) -> storage.set(scope, param.param.value, arg));

        try {
            get(funDecl.block);
        } catch (Return r) {
            return r.value;
        } finally {
            storage = oldStorage;
        }
        return None.INSTANCE;
    }

    private Object builtin (String name, Object[] args) {
        switch (name) {
            case "range":
                return new PolymorphArray(LongStream.range(0, ((Long) args[0]).intValue()).boxed().toArray());

            case "indexer":
                if (args[0] instanceof PolymorphArray) {
                    PolymorphArray array = (PolymorphArray) args[0];
                    return new PolymorphArray(LongStream.range(0, array.size()).boxed().toArray());
                } else if (args[0] instanceof PolymorphMap) {
                    PolymorphMap map = (PolymorphMap) args[0];
                    return new PolymorphArray(map.keys());
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of range function must be a map or an array, not " + type(args[0])));
                }
            case "sort":
                if (args[0] instanceof PolymorphArray) {
                    PolymorphArray original = (PolymorphArray) args[0];
                    PolymorphArray copy = original.clone();
                    try {
                        copy.sort();
                        return copy;
                    } catch (ClassCastException e) {
                        throw new PassthroughException(e);
                    }
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of sort function must be an array, not " + type(args[0])));
                }
            case "int":
                if (args[0] instanceof String) {
                    String s = (String) args[0];
                    return Long.parseLong(s);
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of int function must be a string, not " + type(args[0])));
                }
            case "print":
                String out = convertToString(args[0]);
                System.out.print(out);
                return None.INSTANCE;
            case "println":
                String outln = convertToString(args[0]);
                System.out.println(outln);
                return None.INSTANCE;
            case "len":
                if (args[0] instanceof PolymorphArray) {
                    PolymorphArray array = (PolymorphArray) args[0];
                    return ((Integer) array.size()).longValue();
                } else if (args[0] instanceof PolymorphMap) {
                    PolymorphMap map = (PolymorphMap) args[0];
                    return ((Integer) map.size()).longValue();
                } else {
                    throw new PassthroughException(new RuntimeException("Argument of len function must be an array or a map, not" + type(args[0])));
                }
            case "open":
                assert args.length == 2;
                try {
                    return new File((String) args[0], (String) args[1]);
                } catch (IOException e) {
                    throw new PassthroughException(e);
                }
            case "close":
                assert args.length == 1;
                File fClose = (File) args[0];
                try {
                    fClose.close();
                } catch (IOException e) {
                    throw new PassthroughException(e);
                }
                return None.INSTANCE;
            case "read":
                assert args.length == 1;
                File fRead = (File) args[0];
                try {
                    return fRead.read();
                } catch (IOException e) {
                    throw new PassthroughException(e);
                }
            case "write":
                assert args.length == 2;
                File fWrite = (File) args[0];
                try {
                    fWrite.write(args[1]);
                } catch (IOException e) {
                    throw new PassthroughException(e);
                }
                return None.INSTANCE;
        }
        throw new Error("Should not get here");
    }
}
