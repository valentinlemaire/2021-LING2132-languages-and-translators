import ast.*;
import norswap.autumn.AutumnTestFixture;
import norswap.autumn.positions.LineMapString;
import norswap.uranium.Reactor;
import norswap.uranium.UraniumTestFixture;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;

import java.lang.reflect.Array;
import java.util.Arrays;

public class SemanticUnitTests extends UraniumTestFixture {

    private final NSParser grammar = new NSParser();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.rule = grammar.root();
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    private String input;

    @Override
    protected Object parse (String input) {
        this.input = input;
        return autumnFixture.success(input).topValue();
    }

    @Override
    protected String astNodeToString (Object ast) {
        return ast.toString();
    }

    // ---------------------------------------------------------------------------------------------

    @Override
    protected void configureSemanticAnalysis (Reactor reactor, Object ast) {
        Walker<ASTNode> walker = SemanticAnalysis.createWalker(reactor);
        walker.walk(((ASTNode) ast));
    }

    // ---------------------------------------------------------------------------------------------

    @Test
    public void testInteger() {
        successInput("28");
        successInput("5");
    }

    @Test
    public void testBool() {
        successInput("True");
        successInput("False");
    }

    @Test
    public void testString() {
        successInput("\"Hello World\"");
        successInput("\"PO-TA-TOES!\n\"");
    }

    @Test
    public void testNone() {
        successInput("None");
    }

    @Test
    public void testIdentifier() {
        successInput("a");
        successInput("identifier");
        successInput("Second_Identifier");
    }

    @Test
    public void testArray() {
        successInput("[1, 2, 3]");
        successInput("[\"Hello\", \" \", \"World\"]");
        successInput("a = [:3]");
        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new ArrayNode(Arrays.asList(
                                new IntegerNode(1),
                                new IntegerNode(2),
                                new StringNode("3")
                        ))
                ))),
                new ArrayNode(Arrays.asList(
                        new IntegerNode(1),
                        new IntegerNode(2),
                        new StringNode("3")
                )));
        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new ArrayNode(new StringNode("Hello"))
                ))),
                new ArrayNode(new StringNode("Hello")));
    }

    @Test
    public void testMap() {
        successInput("a = {1 : \"a\", 2 : \"b\", 3 : \"c\"}");
        successInput("x = 1\n"     +
                     "y = \"b\"\n" +
                     "myMap = {x : \"a\", 2 : y    , 3 : \"c\"}");
        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new MapNode(Arrays.asList(
                                new BinaryNode(new IntegerNode(1), new StringNode("Hello"), BinaryNode.PAIR),
                                new BinaryNode(new IntegerNode(2), new IntegerNode(3),      BinaryNode.PAIR)
                        ))
                ))),
                new MapNode(Arrays.asList(
                        new BinaryNode(new IntegerNode(1), new StringNode("Hello"), BinaryNode.PAIR),
                        new BinaryNode(new IntegerNode(2), new IntegerNode(3),      BinaryNode.PAIR)
                )));
    }

    @Test
    public void testFunctionCall() {
        /* TODO shoud fail ? failureInput("testFunction(\"Hello World\", 3)");*/
        successInput("def testFunction(arg1, arg2) :\n" +
                     "   a = 3 + 4\n" +
                     "end\n" +
                     "testFunction(\"Hello World\", 3)");
    }

    @Test
    public void testUnaryNode() {
        // range
        successInput("a = range(3)");
        failureInput("a = range(b)");
        successInput("b = 4\n" +
                     "a = range(b)");
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(new StringNode("Hello"), UnaryNode.RANGE)
                ))),
                new UnaryNode(new StringNode("Hello"), UnaryNode.RANGE)
        );
        success(new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(new IntegerNode(3), UnaryNode.RANGE)
                )))
        );

        // indexer
        // TODO should return [0, 1, 2, 3] if indexer(array) and [key1, key2, key3] if indexer(map)

        // sort
        successInput("a = sort([3, 2, 5, 4])");
        successInput("a = sort([\"PO\", \"TA\", \"TOES\"])");
        successInput("b = [1, 3, 2, 7]\n" +
                     "a = sort(b)");
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(new StringNode("Hello"), UnaryNode.SORT)
                ))),
                new UnaryNode(new StringNode("Hello"), UnaryNode.SORT)
        );
        success(new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(new ArrayNode(Arrays.asList(
                                new IntegerNode(4),
                                new IntegerNode(2),
                                new IntegerNode(7),
                                new IntegerNode(1)
                        )), UnaryNode.SORT)
        ))));

        // parseInt
        successInput("a = int(\"3\")");
        successInput("b = \"hello\"\n" +
                     "a = int(b)");
        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(new IntegerNode(3), UnaryNode.PARSE_INT)
                ))),
                new UnaryNode(new IntegerNode(3), UnaryNode.PARSE_INT)
        );
        success(new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(new StringNode("Hello World"), UnaryNode.PARSE_INT)
        ))));

        // print
        successInput("print(\"3\")");
        successInput("print(3)");
        successInput("print(True)");
        successInput("b = \"hello\"\n" +
                     "print(b)");
        /* TODO should fail ? failureInput("print(a)");*/

        // println
        successInput("println(\"3\")");
        successInput("println(3)");
        successInput("println(True)");
        successInput("b = \"hello\"\n" +
                     "println(b)");

        // negation
        successInput("a = -2");
        successInput("a = -4");
        successInput("a = -int(\"3\")");
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new StringNode("Hello"), UnaryNode.NEGATION)
                ))),
                new UnaryNode(new StringNode("Hello"), UnaryNode.NEGATION)
        );
        success(new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(
                                new UnaryNode(new StringNode("Hello World"), UnaryNode.PARSE_INT),
                                UnaryNode.NEGATION)
        ))));

        // not
        successInput("not True");
        successInput("not False");
        successInput("b = True\n" +
                     "not b");
        success(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new BoolNode(true), UnaryNode.NOT)
        ))));
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new StringNode("Hello"), UnaryNode.NOT)))),
                new UnaryNode(new StringNode("Hello"), UnaryNode.NOT)
        );
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new IntegerNode(7), UnaryNode.NOT)))),
                new UnaryNode(new IntegerNode(7), UnaryNode.NOT)
        );

        // return
        successInput("def testFunction(arg1, arg2) :\n" +
                     "   return 3\n" +
                     "end\n" +
                     "testFunction(\"Hello World\", 3)");
        failureInput("return 3");
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new StringNode("Hello"), UnaryNode.RETURN)))),
                new UnaryNode(new StringNode("Hello"), UnaryNode.RETURN)
        );
        success(new RootNode(new BlockNode(Arrays.asList(
                new FunctionDefinitionNode(
                        new IdentifierNode("myFunction"),
                        Arrays.asList(
                                new ParameterNode(new IdentifierNode("arg1")),
                                new ParameterNode(new IdentifierNode("arg2"))
                        ),
                        new BlockNode(Arrays.asList(
                                new VarAssignmentNode(new IdentifierNode("var"), new IntegerNode(4)),
                                new UnaryNode(new IdentifierNode("var"), UnaryNode.RETURN)
                        )))
        ))));

        // len
        successInput("a = len([1, 2, 3])");
        successInput("a = len([:4])");
        successInput("a = [:4]\n" +
                     "len(a)");
        /* TODO should this not fail ?*/
        /*failureInput("a = True\n" +
                     "len(a)");*/
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new StringNode("Hello World"), UnaryNode.LEN)))),
                new UnaryNode(new StringNode("Hello World"), UnaryNode.LEN)
        );
        success(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new ArrayNode(Arrays.asList(
                        new IntegerNode(4),
                        new IntegerNode(3),
                        new IntegerNode(7)
                )), UnaryNode.LEN)
        ))));
    }

    @Test
    public void testBinaryNode() {
        // TODO
    }

    @Test
    public void testRange() {
        successInput(   "a = range(12)");
        successInput(   "a = range(len(args))");
    }

    @Test
    public void testFunctionDefinition() {
        successInput(   "def fun(a, b):\n" +
                        "   c = b\n" +
                        "end");

        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new FunctionDefinitionNode(new IdentifierNode("fun"), Arrays.asList(new ParameterNode(new IdentifierNode("a")), new ParameterNode(new IdentifierNode("b"))), new BlockNode(Arrays.asList(
                        new VarAssignmentNode(new IdentifierNode("c"), new IdentifierNode("d")))))))),
                new IdentifierNode("d"));

        successInput(   "d = 1\n" +
                        "def fun(a, b):\n" +
                        "   c = d\n" +
                        "end");

        failureAt(new RootNode(new BlockNode(Arrays.asList(
                        new FunctionDefinitionNode(new IdentifierNode("fun"), Arrays.asList(new ParameterNode(new IdentifierNode("a")), new ParameterNode(new IdentifierNode("b"))),
                                new BlockNode(Arrays.asList(
                                        new VarAssignmentNode(new IdentifierNode("c"), new IdentifierNode("d"))))),
                        new VarAssignmentNode(new IdentifierNode("d"), new IntegerNode(1))))),
                  new IdentifierNode("d"));

        successInput(   "def fun(a, b):\n" +
                        "   a = b\n" +
                        "end");
    }

    @Test
    public void testReturn() {
        successInput(   "d = 1\n" +
                        "def fun(a, b):\n" +
                        "   c = d\n" +
                        "   return c\n" +
                        "end");
    }

    @Test
    public void testIfCondition() {
        failureAt(  new RootNode(new BlockNode(Arrays.asList(
                        new IfNode(new IntegerNode(2), new BlockNode(Arrays.asList(
                                new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2)))), null)))),
                    new IfNode(new IntegerNode(2), new BlockNode(Arrays.asList(
                        new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2)))), null
                ));

        successInput(   "if True:\n" +
                        "   a = 1\n" +
                        "else:\n" +
                        "   a = 2\n" +
                        "end");
    }

    @Test
    public void testWhileLoop() {
        failureAt(  new RootNode(new BlockNode(Arrays.asList(
                new WhileNode(new IntegerNode(2), new BlockNode(Arrays.asList(
                        new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2)))))))),
                new WhileNode(new IntegerNode(2), new BlockNode(Arrays.asList(
                        new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2))))
                ));

        successInput(   "while True:\n" +
                        "   a = 1\n" +
                        "end");

        successInput(   "a = False\n" +
                        "while a:\n" +
                        "   a = 1\n" +
                        "end");
    }

    @Test
    public void testForLoop() {
        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new ForNode(
                                new IdentifierNode("i"),
                                new IntegerNode(2),
                                new BlockNode(Arrays.asList(
                                        new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2)))
                                )
                        )
                ))),
                new ForNode(new IdentifierNode("i"), new IntegerNode(2), new BlockNode(Arrays.asList(
                        new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2)))
                )));

        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new ForNode(
                                new IdentifierNode("i"),
                                new MapNode(Arrays.asList(
                                        new BinaryNode(new IntegerNode(3), new StringNode("hello"), BinaryNode.PAIR)
                                )),
                                new BlockNode(Arrays.asList(
                                        new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2)))
                                )
                        )
                ))),
                new ForNode(
                        new IdentifierNode("i"),
                        new MapNode(Arrays.asList(
                                new BinaryNode(new IntegerNode(3), new StringNode("hello"), BinaryNode.PAIR)
                        )),
                        new BlockNode(Arrays.asList(
                                new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(2)))
                        )
                ));

        successInput(   "for i in range(2):\n" +
                        "   a = 1\n" +
                        "end");
        successInput(   "for i in range(len(args)):\n" +
                        "   a = 1\n" +
                        "end");
    }

    @Test
    public void testVarAssignment() {
        // TODO
    }
}
