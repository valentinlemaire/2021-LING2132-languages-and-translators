import ast.*;

import org.testng.annotations.Test;
import norswap.autumn.AutumnTestFixture;

import java.util.Arrays;
import java.util.List;


public class ParserUnitTests extends AutumnTestFixture {

    NSParser parser = new NSParser();

    @Test
    public void testNumericalValue() {
        this.rule = parser.numerical_operation;
        success("1");
        success("a_b_c");
    }

    @Test
    public void testString() {
        this.rule = parser.string;
        successExpect("\"Coucou petite perruche\"", new StringNode("Coucou petite perruche"));
        failure("\" failing string");
        successExpect("\" this is'\t a test\n string\"", new StringNode(" this is'\t a test\n string"));

    }

    @Test
    public void testIndexing() {
        this.rule = parser.multiple_indexer_access;
        successExpect("a[1]", new BinaryNode(new IdentifierNode("a"), new IntegerNode(1), BinaryNode.IDX_ACCESS));
        successExpect("a[b]", new BinaryNode(new IdentifierNode("a"), new IdentifierNode("b"), BinaryNode.IDX_ACCESS));
        successExpect("a[b[c[1]]]", new BinaryNode(new IdentifierNode("a"), new BinaryNode(new IdentifierNode("b"), new BinaryNode(new IdentifierNode("c"), new IntegerNode(1), BinaryNode.IDX_ACCESS), BinaryNode.IDX_ACCESS), BinaryNode.IDX_ACCESS));
        successExpect("a[\"test\"]", new BinaryNode(new IdentifierNode("a"), new StringNode("test"), BinaryNode.IDX_ACCESS));
        success("a[1][2]");
    }

    @Test
    public void testFunctionCall() {
        this.rule = parser.function_call;
        successExpect("fun(1, a, 2+4)", new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(1), new IdentifierNode("a"), new BinaryNode(new IntegerNode(2), new IntegerNode(4), BinaryNode.ADD))));
        successExpect("fun()", new FunctionCallNode(new IdentifierNode("fun"), null));
    }

    @Test
    public void testOperations() {
        this.rule = parser.numerical_operation;
        successExpect("1 - 1", new BinaryNode(new IntegerNode(1), new IntegerNode(1), BinaryNode.SUB));
        successExpect("1 / 2+ 1", new BinaryNode(new BinaryNode(new IntegerNode(1), new IntegerNode(2), BinaryNode.DIV), new IntegerNode(1), BinaryNode.ADD));
        successExpect("-1 + 1", new BinaryNode(new UnaryNode(new IntegerNode(1), UnaryNode.NEGATION), new IntegerNode(1), BinaryNode.ADD));
        successExpect("5 * 4 + 20", new BinaryNode(new BinaryNode(new IntegerNode(5), new IntegerNode(4), BinaryNode.MUL), new IntegerNode(20), BinaryNode.ADD));
        successExpect("5 + 20 * 4", new BinaryNode(new IntegerNode(5), new BinaryNode(new IntegerNode(20), new IntegerNode(4), BinaryNode.MUL), BinaryNode.ADD));
        successExpect("5 + 4 % 2 - 6", new BinaryNode(new BinaryNode(new IntegerNode(5), new BinaryNode(new IntegerNode(4), new IntegerNode(2), BinaryNode.MOD), BinaryNode.ADD), new IntegerNode(6), BinaryNode.SUB));

        successExpect("(1 + 1) + 1000", new BinaryNode(new BinaryNode(new IntegerNode(1), new IntegerNode(1), BinaryNode.ADD), new IntegerNode(1000), BinaryNode.ADD));
        successExpect("1 + (1 + 1000)", new BinaryNode(new IntegerNode(1), new BinaryNode(new IntegerNode(1), new IntegerNode(1000), BinaryNode.ADD), BinaryNode.ADD));

        successExpect("1 + (90 * 85)", new BinaryNode(new IntegerNode(1), new BinaryNode(new IntegerNode(90), new IntegerNode(85), BinaryNode.MUL), BinaryNode.ADD));
        successExpect("(1 + 90) * 85", new BinaryNode(new BinaryNode(new IntegerNode(1), new IntegerNode(90), BinaryNode.ADD), new IntegerNode(85), BinaryNode.MUL));

        successExpect("- 5 * 1", new UnaryNode(new BinaryNode(new IntegerNode(5), new IntegerNode(1), BinaryNode.MUL), UnaryNode.NEGATION));
        failure("5 * - 1");

        // this is weird
        success("5 + -1");

    }

    @Test
    public void testBooleanValues() {
        this.rule = parser.bool;
        successExpect("True", new BoolNode(true));
        successExpect("False", new BoolNode(false));
        successExpect("(((True)))", new BoolNode(true));
    }

    @Test
    public void testBooleanComparisons() {
        this.rule = parser.bool;
        successExpect("1 + 1 <= a", new BinaryNode(new BinaryNode(new IntegerNode(1), new IntegerNode(1), BinaryNode.ADD), new IdentifierNode("a"), BinaryNode.LEQ));
        successExpect("1 + 1 > a - b", new BinaryNode(new BinaryNode(new IntegerNode(1), new IntegerNode(1), BinaryNode.ADD), new BinaryNode(new IdentifierNode("a"), new IdentifierNode("b"), BinaryNode.SUB), BinaryNode.G));
        successExpect("1 < 2 + 1", new BinaryNode(new IntegerNode(1), new BinaryNode(new IntegerNode(2), new IntegerNode(1), BinaryNode.ADD), BinaryNode.L));
        success("a == (b > d)");
        successExpect("(a < b) > d", new BinaryNode(new BinaryNode(new IdentifierNode("a"), new IdentifierNode("b"), BinaryNode.L), new IdentifierNode("d"), BinaryNode.G));
        successExpect("a < (b > d)", new BinaryNode(new IdentifierNode("a"), new BinaryNode(new IdentifierNode("b"), new IdentifierNode("d"), BinaryNode.G), BinaryNode.L));
        successExpect("True == False", new BinaryNode(new BoolNode(true), new BoolNode(false), BinaryNode.EQ));
    }

    @Test
    public void testLogicalOperations() {
        this.rule = parser.bool;
        successExpect("True or False", new BinaryNode(new BoolNode(true), new BoolNode(false), BinaryNode.OR));
        successExpect("(1+1 == 2) and True", new BinaryNode(new BinaryNode(new BinaryNode(new IntegerNode(1), new IntegerNode(1), BinaryNode.ADD), new IntegerNode(2), BinaryNode.EQ), new BoolNode(true), BinaryNode.AND));
        successExpect("b != False", new BinaryNode(new IdentifierNode("b"), new BoolNode(false), BinaryNode.NEQ));
        successExpect("a and b or c", new BinaryNode(new BinaryNode(new IdentifierNode("a"), new IdentifierNode("b"), BinaryNode.AND),new IdentifierNode("c"), BinaryNode.OR));
        successExpect("a or b and c", new BinaryNode(new IdentifierNode("a"), new BinaryNode(new IdentifierNode("b"), new IdentifierNode("c"), BinaryNode.AND), BinaryNode.OR));

        successExpect("1+1 < 2 and True or b == not False and True", new BinaryNode(new BinaryNode(new BinaryNode(new BinaryNode(new IntegerNode(1), new IntegerNode(1), BinaryNode.ADD), new IntegerNode(2), BinaryNode.L), new BoolNode(true), BinaryNode.AND), new BinaryNode(new BinaryNode(new IdentifierNode("b"), new UnaryNode(new BoolNode(false), UnaryNode.NOT), BinaryNode.EQ), new BoolNode(true), BinaryNode.AND), BinaryNode.OR));
        successExpect("a and not b", new BinaryNode(new IdentifierNode("a"), new UnaryNode(new IdentifierNode("b"), UnaryNode.NOT), BinaryNode.AND));
        successExpect("a or not b", new BinaryNode(new IdentifierNode("a"), new UnaryNode(new IdentifierNode("b"), UnaryNode.NOT), BinaryNode.OR));
        successExpect("not a and b", new BinaryNode(new UnaryNode(new IdentifierNode("a"), UnaryNode.NOT), new IdentifierNode("b"), BinaryNode.AND));

    }

    @Test
    public void testLogicalNegation() {
        this.rule = parser.bool;
        successExpect("not a", new UnaryNode(new IdentifierNode("a"), UnaryNode.NOT));
        successExpect("not True", new UnaryNode(new BoolNode(true), UnaryNode.NOT));
        successExpect("not (a < b)", new UnaryNode(new BinaryNode(new IdentifierNode("a"), new IdentifierNode("b"), BinaryNode.L), UnaryNode.NOT));
    }

    @Test
    public void testArray() {
        this.rule = parser.array;
        successExpect("[1, 2]", new ArrayNode(Arrays.asList(new ASTNode[]{new IntegerNode(1), new IntegerNode(2)})));
        successExpect("[1+2, 3]", new ArrayNode(Arrays.asList(
                new BinaryNode(new IntegerNode(1), new IntegerNode(2), BinaryNode.ADD),
                new IntegerNode(3)
        )));
        successExpect("[:7]", new ArrayNode(new IntegerNode(7)));
        successExpect("[ :\t4/2]", new ArrayNode(new BinaryNode(new IntegerNode(4), new IntegerNode(2), BinaryNode.DIV)));
        successExpect("[\t]", new ArrayNode((List<ASTNode>) null));
        failure("[:True]");
        failure("[:\"This should fail\"]");
    }

    @Test
    public void testMap() {
        this.rule = parser.map;
        successExpect("{1 : 6/5,\t \"hello\"  : fun(2)}", new MapNode(Arrays.asList(
                new BinaryNode(new IntegerNode(1), new BinaryNode(new IntegerNode(6), new IntegerNode(5), BinaryNode.DIV), BinaryNode.PAIR),
                new BinaryNode(new StringNode("hello"), new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new ASTNode[]{new IntegerNode(2)})), BinaryNode.PAIR)
        )));
        successExpect("{True : False}", new MapNode(Arrays.asList(
                new BinaryNode(new BoolNode(true), new BoolNode(false), BinaryNode.PAIR)
        )));
        failure("{ 'hello");
        failure("{ 1 2 }");
        failure("{1-3}");
    }

    @Test
    public void testVariableAssignment() {
        this.rule = parser.variable_assignment;
        successExpect("a = 1", new VarAssignmentNode(new IdentifierNode("a"), new IntegerNode(1)));
        successExpect("a[3] = 1", new VarAssignmentNode(new BinaryNode(new IdentifierNode("a"), new IntegerNode(3), BinaryNode.IDX_ACCESS), new IntegerNode(1)));
        successExpect("a[3] = fun(2)", new VarAssignmentNode(new BinaryNode(new IdentifierNode("a"), new IntegerNode(3), BinaryNode.IDX_ACCESS), new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(2)))));
        failure("1 = 2");
        failure("fun(a) = b");
    }

    @Test
    public void testIf() {
        this.rule = parser.if_;
        successExpect("if True: x = 1 end", new IfNode(new BoolNode(true), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1)))), Arrays.asList(new ASTNode[]{})));
        successExpect("if True: x = 1 elsif False: x = 2 elsif b: x = 3 else: x = 4 end",
                new IfNode(new BoolNode(true), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1)))),
                        Arrays.asList(
                                new ElseNode(new BoolNode(false),  new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(2))))),
                                new ElseNode(new IdentifierNode("b"), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(3))))),
                                new ElseNode(null, new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(4))))))));
        failure("if True:");
        failure("if True end");
        failure("if: end");
    }

    @Test
    public void testWhile() {
        this.rule = parser.while_;
        successExpect("while True: i = i+1 a = b end", new WhileNode(new BoolNode(true), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("i"), new BinaryNode(new IdentifierNode("i"), new IntegerNode(1), BinaryNode.ADD)), new VarAssignmentNode(new IdentifierNode("a"), new IdentifierNode("b"))))));
        successExpect("while b: end", new WhileNode(new IdentifierNode("b"), new BlockNode(Arrays.asList(new ASTNode[]{}))));
        failure("while: a = b end");
        failure("while True a = b end");
        failure("while True: a = b");
    }

    @Test
    public void testFor() {
        this.rule = parser.for_;
        successExpect("for i in b: x = 1 end", new ForNode(new IdentifierNode("i"), new IdentifierNode("b"), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1))))));
        successExpect("for i in [1, 2, 3]: x = 1 end", new ForNode(new IdentifierNode("i"), new ArrayNode(Arrays.asList(new IntegerNode(1), new IntegerNode(2), new IntegerNode(3))), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1))))));
        successExpect("for i in range(10): x = 1 end", new ForNode(new IdentifierNode("i"), new UnaryNode(new IntegerNode(10), UnaryNode.RANGE), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1))))));
        successExpect("for i in indexer([1, 2, 3]): x = 1 end", new ForNode(new IdentifierNode("i"), new UnaryNode(new ArrayNode(Arrays.asList(new IntegerNode(1), new IntegerNode(2), new IntegerNode(3))), UnaryNode.INDEXER), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1))))));
        failure("for i in \"hello\": x = 1 end");
        failure("for i in b: x = 1");
        failure("for i b : x = 1 end");
        failure("for i in b x = 1 end");
    }

    @Test
    public void testFunctionDef() {
        this.rule = parser.function_def;
        successExpect("def fun(a, b): x = 1 return 2 end", new FunctionDefinitionNode(new IdentifierNode("fun"), Arrays.asList(new ParameterNode(new IdentifierNode("a")), new ParameterNode(new IdentifierNode("b"))), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1)), new UnaryNode(new IntegerNode(2), UnaryNode.RETURN)))));
        successExpect("def fun(a, b): x = 1 end", new FunctionDefinitionNode(new IdentifierNode("fun"), Arrays.asList(new ParameterNode(new IdentifierNode("a")), new ParameterNode(new IdentifierNode("b"))), new BlockNode(Arrays.asList(new VarAssignmentNode(new IdentifierNode("x"), new IntegerNode(1))))));
        successExpect("def fun(): end", new FunctionDefinitionNode(new IdentifierNode("fun"), null, new BlockNode(Arrays.asList(new ASTNode[]{}))));
        successExpect("def fun(): return end", new FunctionDefinitionNode(new IdentifierNode("fun"), null, new BlockNode(Arrays.asList(new UnaryNode(null, UnaryNode.RETURN)))));
        failure("def a[1](): end");
        failure("def fun: end");
        failure("fun(a,b): x = 1 end");
        failure("def fun(): return");
    }

    @Test
    public void testPrint() {
        this.rule = parser.print;
        /* print */
        successExpect("print(\"hello world\")", new UnaryNode(new StringNode("hello world"), UnaryNode.PRINT));
        successExpect("print(1)", new UnaryNode(new IntegerNode(1), UnaryNode.PRINT));
        successExpect("print(3+4)", new UnaryNode(new BinaryNode(new IntegerNode(3), new IntegerNode(4), BinaryNode.ADD), UnaryNode.PRINT));
        successExpect("print ( True )", new UnaryNode(new BoolNode(true), UnaryNode.PRINT));
        successExpect("print()", new UnaryNode(null, UnaryNode.PRINT));
        failure("print())");
        /* println */
        successExpect("println(\"hello world\")", new UnaryNode(new StringNode("hello world"), UnaryNode.PRINTLN));
        successExpect("println(1)", new UnaryNode(new IntegerNode(1), UnaryNode.PRINTLN));
        successExpect("println(3+4)", new UnaryNode(new BinaryNode(new IntegerNode(3), new IntegerNode(4), BinaryNode.ADD), UnaryNode.PRINTLN));
        successExpect("println ( True )", new UnaryNode(new BoolNode(true), UnaryNode.PRINTLN));
        successExpect("println()", new UnaryNode(null, UnaryNode.PRINTLN));
        failure("println(,)");
    }

    @Test
    public void testParseInt() {
        this.rule = parser.parse_int;
        successExpect("int(\"1\")", new UnaryNode(new StringNode("1"), UnaryNode.PARSE_INT));
        successExpect("int (\t\"122\")", new UnaryNode(new StringNode("122"), UnaryNode.PARSE_INT));
        successExpect("int(\"hello\")", new UnaryNode(new StringNode("hello"), UnaryNode.PARSE_INT));
        successExpect("int(fun(3, \"hello\"))", new UnaryNode(new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(3), new StringNode("hello"))), UnaryNode.PARSE_INT));
        failure("int(3)");
        failure("inte ()");
        failure("int()");
        failure("int(True)");
    }

    @Test
    public void sortTest() {
        this.rule = parser.sort;
        successExpect("sort([1, 2, \"ab\"])", new UnaryNode(new ArrayNode(Arrays.asList(new IntegerNode(1), new IntegerNode(2), new StringNode("ab"))), UnaryNode.SORT));
        successExpect("sort  (\tfun(3, \"hello\"))", new UnaryNode(new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(3), new StringNode("hello"))), UnaryNode.SORT));
        failure("sort()");
        failure("sort");
        failure("sort(3)");
    }

    @Test
    public void rangeTest() {
        this.rule = parser.range;
        successExpect("range(3)", new UnaryNode(new IntegerNode(3), UnaryNode.RANGE));
        successExpect("range\t( fun(3, \"hello\"))", new UnaryNode(new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(3), new StringNode("hello"))), UnaryNode.RANGE));
        failure("range()");
        failure("range");
        failure("range([1, 2])");
    }

    @Test
    public void indexerTest() {
        this.rule = parser.indexer;
        successExpect("indexer({\"a\" : 2})", new UnaryNode(new MapNode(Arrays.asList(new BinaryNode(new StringNode("a"), new IntegerNode(2), BinaryNode.PAIR))), UnaryNode.INDEXER));
        successExpect("indexer\t( fun(3, \"hello\"))", new UnaryNode(new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(3), new StringNode("hello"))), UnaryNode.INDEXER));
        successExpect("indexer([1, 2])", new UnaryNode(new ArrayNode(Arrays.asList(new IntegerNode(1), new IntegerNode(2))), UnaryNode.INDEXER));
        failure("indexer()");
        failure("indexer");
    }

}