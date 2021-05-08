import ast.*;
import norswap.autumn.AutumnTestFixture;
import norswap.uranium.Reactor;
import norswap.uranium.UraniumTestFixture;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;

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
        successInput(   "a = 1\n" +
                        "a");
        successInput(   "identifier = None\n" +
                        "identifier = \"this\"");

        failureAt(  new RootNode(new BlockNode(Arrays.asList(
                        new IdentifierNode("a")
                    ))), new IdentifierNode("a"));
    }

    @Test
    public void testArray() {
        successInput("[1, 2, 3]");
        successInput("[\"Hello\", \" \", \"World\"]");
        successInput("a = [:3]");
        success(
                new RootNode(new BlockNode(Arrays.asList(
                        new ArrayNode(Arrays.asList(
                                new IntegerNode(1),
                                new IntegerNode(2),
                                new StringNode("3")
                        ))
                ))));
        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new ArrayNode(new StringNode("Hello"))
                ))),
                new ArrayNode(new StringNode("Hello")));
    }

    @Test
    public void testListComprehension() {

        successInput("[x for x in [1, 2]]");

        successInput("[f(x) for x in [1, 2]]");

        successInput("[f(x) for x in range(12)]");

        successInput("[x+5 for x in [None, 2] if x != None");

        failureAt(new RootNode(new BlockNode(Arrays.asList(new ASTNode[]{new ListComprehensionNode(
                new IdentifierNode("a"),
                new IdentifierNode("x"),
                new ArrayNode(Arrays.asList(new ASTNode[]{new IntegerNode(1), new IntegerNode(2)})),
                null)}))), new ListComprehensionNode(
                new IdentifierNode("a"),
                new IdentifierNode("x"),
                new ArrayNode(Arrays.asList(new ASTNode[]{new IntegerNode(1), new IntegerNode(2)})),
                null));

        failureAt(new RootNode(new BlockNode(Arrays.asList(new ASTNode[]{new ListComprehensionNode(
                new FunctionCallNode(new IdentifierNode("f"), Arrays.asList(new ASTNode[]{new IdentifierNode("x")})),
                new IdentifierNode("x"), new ArrayNode(Arrays.asList(new ASTNode[]{new IntegerNode(1), new IntegerNode(2)})),
                new IdentifierNode("x"))}))), new ListComprehensionNode(
                new FunctionCallNode(new IdentifierNode("f"), Arrays.asList(new ASTNode[]{new IdentifierNode("x")})),
                new IdentifierNode("x"), new ArrayNode(Arrays.asList(new ASTNode[]{new IntegerNode(1), new IntegerNode(2)})),
                new IdentifierNode("x")));


    }

    @Test
    public void testMap() {
        successInput("a = {1 : \"a\", 2 : \"b\", 3 : \"c\"}");
        successInput("x = 1\n"     +
                     "y = \"b\"\n" +
                     "myMap = {x : \"a\", 2 : y    , 3 : \"c\"}");
        success(
                new RootNode(new BlockNode(Arrays.asList(
                        new MapNode(Arrays.asList(
                                new BinaryNode(new IntegerNode(1), new StringNode("Hello"), BinaryNode.PAIR),
                                new BinaryNode(new IntegerNode(2), new IntegerNode(3),      BinaryNode.PAIR)
                        ))
                ))));

    }

    @Test
    public void testFunctionCall() {
        failureInput("testFunction(\"Hello World\", 3)");
        successInput("def testFunction(arg1, arg2) :\n" +
                     "   a = 3 + 4\n" +
                     "end\n" +
                     "testFunction(\"Hello World\", 3)");
    }

    @Test
    public void testRange() {
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
    }

    @Test
    public void testIndexer() {
        successInput("a = [1, 3, 2, 4]\n" +
                     "indexer(a)");
        successInput("a = {\"a\" : 1, \"b\" : 2, \"d\" : 4, \"c\" : 3}\n" +
                     "indexer(a)");
        failureInput("a = 3\n" +
                     "indexer(a)");
        failureInput("a = \"PO-TA-TOES\"\n" +
                     "indexer(a)");
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new StringNode("Hello"), UnaryNode.INDEXER)
                ))),
                new UnaryNode(new StringNode("Hello"), UnaryNode.INDEXER)
        );
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new VarAssignmentNode(new IdentifierNode("a"), new StringNode("Test")),
                new UnaryNode(new IdentifierNode("a"), UnaryNode.INDEXER)
                ))),
                new UnaryNode(new IdentifierNode("a"), UnaryNode.INDEXER)
        );

    }

    @Test
    public void testSort() {
        successInput("a = sort([3, 2, 5, 4])");
        successInput("a = sort([\"PO\", \"TA\", \"TOES\"])");
        successInput(   "b = [1, 3, 2, 7]\n" +
                        "a = sort(b)");

        failureInput(   "b = False\n" +
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
    }

    @Test
    public void testParseInt() {
        successInput("a = int(\"3\")");
        successInput("b = \"hello\"\n" +
                     "a = int(b)");
        successInput("b = \"hello\"\n" +
                     "a = int(b)\n" +
                     "a = a + 1");
        failureAt(
                new RootNode(new BlockNode(Arrays.asList(
                        new UnaryNode(new IntegerNode(3), UnaryNode.PARSE_INT)
                ))),
                new UnaryNode(new IntegerNode(3), UnaryNode.PARSE_INT)
        );
        success(new RootNode(new BlockNode(Arrays.asList(
                new UnaryNode(new StringNode("Hello World"), UnaryNode.PARSE_INT)
        ))));
    }

    @Test
    public void testPrint() {
        successInput("print(\"3\")");
        successInput("print(3)");
        successInput("print(True)");
        successInput("b = \"hello\"\n" +
                     "print(b)");
        failureInput("print(a)");
    }

    @Test
    public void testPrintln() {
        successInput("println(\"3\")");
        successInput("println(3)");
        successInput("println(True)");
        successInput("b = \"hello\"\n" +
                     "println(b)");
        failureInput("print(a)");
    }

    @Test
    public void testNegation() {
        successInput("a = -2");
        successInput("a = -4");
        successInput("a = -int(\"3\")");
        successInput("a = 3\n" +
                     "-a");
        failureInput("a = \"hello\"\n" +
                     "-a");
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
    }

    @Test
    public void testNot() {
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
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new VarAssignmentNode(new IdentifierNode("var"), new IntegerNode(3)),
                new UnaryNode(new IdentifierNode("var"), UnaryNode.NOT)))),
                new UnaryNode(new IdentifierNode("var"), UnaryNode.NOT)
        );
    }

    @Test
    public void testReturn() {
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
    }

    @Test
    public void testLen() {
        failureInput("a = 23\n" +
                     "len(a)");
        successInput("a = len([1, 2, 3])");
        successInput("a = len([:4])");
        successInput(   "a = [:4]\n" +
                        "len(a)");

        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new VarAssignmentNode(new IdentifierNode("a"), new BoolNode(false)),
                new UnaryNode(new IdentifierNode("a"), UnaryNode.LEN)
        ))), new UnaryNode(new IdentifierNode("a"), UnaryNode.LEN));
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
    public void testArithmeticOperation() {
        successInput("1 + 2");
        successInput("1 - 2");
        successInput("1 * 2");
        successInput("1 / 2");
        successInput("1 % 2");
        successInput("a = 3\n" +
                     "a + 2");
        failureInput("a + 2");
        failureInput("a = \"3\"\n" +
                     "a + 2");

        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new VarAssignmentNode(new IdentifierNode("a"), new StringNode("3")),
                new BinaryNode(new IdentifierNode("a"), new IntegerNode(2), BinaryNode.ADD)))),
                new BinaryNode(new IdentifierNode("a"), new IntegerNode(2), BinaryNode.ADD)
        );
        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new VarAssignmentNode(new IdentifierNode("a"), new StringNode("3")),
                new BinaryNode(new IntegerNode(2), new IdentifierNode("a"), BinaryNode.ADD)))),
                new BinaryNode(new IntegerNode(2), new IdentifierNode("a"), BinaryNode.ADD)
        );
    }

    @Test
    public void testEqualityComparison() {
        successInput("1 == 2");
        successInput("1 != 2");
        successInput("\"1\" == \"2\"");
        successInput("[1] == [1, 2]");
        successInput("a = 1\n" +
                     "a == 2");
        successInput("a = \"Hello World\"\n" +
                     "a == \"PO-TA-TOES\"");
    }

    @Test
    public void testInequalityComparison() {
        successInput("1 < 2");
        successInput("1 <= 2");
        successInput("1 > 2");
        successInput("1 >= 2");
        successInput("1 < 2");
        successInput("\"Hello\" < \"Hello\"");
    }

    @Test
    public void testLogicOperation() {
        successInput("True and False");
        successInput("False and False");
        successInput("True and True");
        successInput("a = True\n" +
                     "a or True");

        failureAt(new RootNode(new BlockNode(Arrays.asList(
                    new VarAssignmentNode(new IdentifierNode("a"), new StringNode("str")),
                    new BinaryNode(new IdentifierNode("a"), new BoolNode(true), BinaryNode.OR)))),
                new BinaryNode(new IdentifierNode("a"), new BoolNode(true), BinaryNode.OR));
    }

    @Test
    public void testIndexAccess() {
        failureAt(new RootNode(new BlockNode(Arrays.asList(new BinaryNode(new StringNode("Hello"), new IntegerNode(2), BinaryNode.IDX_ACCESS)))), new BinaryNode(new StringNode("Hello"), new IntegerNode(2), BinaryNode.IDX_ACCESS));

        successInput("a = [1, 2, 3]\n" +
                     "a[1]");
        successInput("[:3][2]");
        failureInput("a = [1, 2, 3]\n" +
                     "a[\"hello\"]");
        failureInput("a = True\n" +
                     "a[1]");
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

        successInput(   "map = {1:5, 3:2, 5:4}\n" +
                        "for key in indexer(map):\n" +
                        "   map[key] = \"yo\"\n" +
                        "end");
    }

    @Test
    public void testVarAssignment() {
        successInput("a = 1");

        failureAt(new RootNode(new BlockNode(Arrays.asList(
                new VarAssignmentNode(new BinaryNode(new IdentifierNode("a"), new IntegerNode(2), BinaryNode.IDX_ACCESS), new IntegerNode(5))))),
                new IdentifierNode("a"));

        successInput(   "a = True\n" +
                        "b = a");

        successInput(   "a = range(len([:7]))");

        successInput(   "b = True\n" +
                        "b = 1");


    }
}
