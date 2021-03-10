import ast.*;

import org.testng.annotations.Test;
import norswap.autumn.TestFixture;

import java.util.Arrays;
import java.util.List;


public class Tests extends TestFixture {

    Parser parser = new Parser();

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
        successExpect("a[1]", new IndexerAccessNode(new IdentifierNode("a"), new IntegerNode(1)));
        successExpect("a[b]", new IndexerAccessNode(new IdentifierNode("a"), new IdentifierNode("b")));
        successExpect("a[b[c[1]]]", new IndexerAccessNode(new IdentifierNode("a"), new IndexerAccessNode(new IdentifierNode("b"), new IndexerAccessNode(new IdentifierNode("c"), new IntegerNode(1)))));
        successExpect("a[\"test\"]", new IndexerAccessNode(new IdentifierNode("a"), new StringNode("test")));
        success("a[1][2]");
    }

    @Test
    public void testFunctionCall() {
        this.rule = parser.function_call;
        successExpect("fun(1, a, 2+4)", new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(1), new IdentifierNode("a"), new AddNode(new IntegerNode(2), new IntegerNode(4)))));
        successExpect("fun()", new FunctionCallNode(new IdentifierNode("fun"), null));
    }

    @Test
    public void testArgs() {
        this.rule = parser.program_args;
        successExpect("args[1]", new ArgAccessNode(new IntegerNode(1)));
        successExpect("args[a+b]", new ArgAccessNode(new AddNode(new IdentifierNode("a"), new IdentifierNode("b"))));
    }

    @Test
    public void testOperations() {
        this.rule = parser.numerical_operation;
        successExpect("1 - 1", new SubNode(new IntegerNode(1), new IntegerNode(1)));
        successExpect("1 / 2+ 1", new AddNode(new DivNode(new IntegerNode(1), new IntegerNode(2)), new IntegerNode(1)));
        successExpect("-1 + 1", new AddNode(new NegationNode(new IntegerNode(1)), new IntegerNode(1)));
        successExpect("5 * 4 + 20", new AddNode(new MultNode(new IntegerNode(5), new IntegerNode(4)), new IntegerNode(20)));
        successExpect("5 + 20 * 4", new AddNode(new IntegerNode(5), new MultNode(new IntegerNode(20), new IntegerNode(4))));
        successExpect("5 + 4 % 2 - 6", new SubNode(new AddNode(new IntegerNode(5), new ModNode(new IntegerNode(4), new IntegerNode(2))), new IntegerNode(6)));

        successExpect("(1 + 1) + 1000", new AddNode(new AddNode(new IntegerNode(1), new IntegerNode(1)), new IntegerNode(1000)));
        successExpect("1 + (1 + 1000)", new AddNode(new IntegerNode(1), new AddNode(new IntegerNode(1), new IntegerNode(1000))));

        successExpect("1 + (90 * 85)", new AddNode(new IntegerNode(1), new MultNode(new IntegerNode(90), new IntegerNode(85))));
        successExpect("(1 + 90) * 85", new MultNode(new AddNode(new IntegerNode(1), new IntegerNode(90)), new IntegerNode(85)));

        successExpect("- 5 * 1", new NegationNode(new MultNode(new IntegerNode(5), new IntegerNode(1))));
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
        successExpect("1 + 1 <= a", new ComparisonNode(ComparisonNode.LEQ, new AddNode(new IntegerNode(1), new IntegerNode(1)), new IdentifierNode("a")));
        successExpect("1 + 1 > a - b", new ComparisonNode(ComparisonNode.G, new AddNode(new IntegerNode(1), new IntegerNode(1)), new SubNode(new IdentifierNode("a"), new IdentifierNode("b"))));
        successExpect("1 < 2 + 1", new ComparisonNode(ComparisonNode.L, new IntegerNode(1), new AddNode(new IntegerNode(2), new IntegerNode(1))));
        success("a == (b > d)");
        successExpect("(a < b) > d", new ComparisonNode(ComparisonNode.G, new ComparisonNode(ComparisonNode.L, new IdentifierNode("a"), new IdentifierNode("b")), new IdentifierNode("d")));
        successExpect("a < (b > d)", new ComparisonNode(ComparisonNode.L, new IdentifierNode("a"), new ComparisonNode(ComparisonNode.G, new IdentifierNode("b"), new IdentifierNode("d"))));
        successExpect("True == False", new ComparisonNode(ComparisonNode.EQ, new BoolNode(true), new BoolNode(false)));
    }

    @Test
    public void testLogicalOperations() {
        this.rule = parser.bool;
        successExpect("True or False", new OrNode(new BoolNode(true), new BoolNode(false)));
        successExpect("(1+1 == 2) and True", new AndNode(new ComparisonNode(ComparisonNode.EQ, new AddNode(new IntegerNode(1), new IntegerNode(1)), new IntegerNode(2)), new BoolNode(true)));
        successExpect("b != False", new ComparisonNode(ComparisonNode.NEQ, new IdentifierNode("b"), new BoolNode(false)));
    }

    @Test
    public void testLogicalNegation() {
        this.rule = parser.bool;
        successExpect("not a", new NotNode(new IdentifierNode("a")));
        successExpect("not True", new NotNode(new BoolNode(true)));
        successExpect("not (a < b)", new NotNode(new ComparisonNode(ComparisonNode.L, new IdentifierNode("a"), new IdentifierNode("b"))));
        successExpect("not a and b", new AndNode(new NotNode(new IdentifierNode("a")), new IdentifierNode("b")));
    }

    @Test
    public void testArray() {
        this.rule = parser.array;
        successExpect("[1, 2]", new ArrayNode(Arrays.asList(new ASTNode[]{new IntegerNode(1), new IntegerNode(2)})));
        successExpect("[1+2, 3]", new ArrayNode(Arrays.asList(
                new AddNode(new IntegerNode(1), new IntegerNode(2)),
                new IntegerNode(3)
        )));
        successExpect("[:7]", new ArrayNode(new IntegerNode(7)));
        successExpect("[ :\t4/2]", new ArrayNode(new DivNode(new IntegerNode(4), new IntegerNode(2))));
        successExpect("[\t]", new ArrayNode((List<ASTNode>) null));
        failure("[:True]");
        failure("[:\"This should fail\"]");
    }

    @Test
    public void testMap() {
        this.rule = parser.map;
        successExpect("{1 : 6/5,\t \"hello\"  : fun(2)}", new MapNode(Arrays.asList(
                new PairNode(new IntegerNode(1), new DivNode(new IntegerNode(6), new IntegerNode(5))),
                new PairNode(new StringNode("hello"), new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new ASTNode[]{new IntegerNode(2)})))
        )));
        successExpect("{True : False}", new MapNode(Arrays.asList(
                new PairNode(new BoolNode(true), new BoolNode(false))
        )));
        failure("{ 'hello");
        failure("{ 1 2 }");
        failure("{1-3}");
    }

    @Test
    public void testVariableAssignment() {
        this.rule = parser.variable_assignment;
        successExpect("a = 1", new VariableAssignmentNode(new IdentifierNode("a"), new IntegerNode(1)));
        successExpect("a[3] = 1", new VariableAssignmentNode(new IndexerAccessNode(new IdentifierNode("a"), new IntegerNode(3)), new IntegerNode(1)));
        successExpect("a[3] = fun(2)", new VariableAssignmentNode(new IndexerAccessNode(new IdentifierNode("a"), new IntegerNode(3)), new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(2)))));
        failure("1 = 2");
        failure("fun(a) = b");
    }

    @Test
    public void testIf() {
        this.rule = parser.if_;
        successExpect("if True: x = 1 end", new IfNode(new BoolNode(true), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(1))), null));
        successExpect("if True: x = 1 elsif False: x = 2 elsif b: x = 3 else: x = 4 end",
                new IfNode(new BoolNode(true), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(1))),
                        Arrays.asList(
                                new ElseNode(new BoolNode(false),  Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(2)))),
                                new ElseNode(new IdentifierNode("b"), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(3)))),
                                new ElseNode(null, Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(4)))))));
        failure("if True:");
        failure("if True end");
        failure("if: end");
    }

    @Test
    public void testWhile() {
        this.rule = parser.while_;
        successExpect("while True: i = i+1 a = b end", new WhileNode(new BoolNode(true), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("i"), new AddNode(new IdentifierNode("i"), new IntegerNode(1))), new VariableAssignmentNode(new IdentifierNode("a"), new IdentifierNode("b")))));
        successExpect("while b: end", new WhileNode(new IdentifierNode("b"), Arrays.asList(new ASTNode[]{})));
        failure("while: a = b end");
        failure("while True a = b end");
        failure("while True: a = b");
    }

    @Test
    public void testFor() {
        this.rule = parser.for_;
        successExpect("for i in b: x = 1 end", new ForNode(new IdentifierNode("i"), new IdentifierNode("b"), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(1)))));
        successExpect("for i in [1, 2, 3]: x = 1 end", new ForNode(new IdentifierNode("i"), new ArrayNode(Arrays.asList(new IntegerNode(1), new IntegerNode(2), new IntegerNode(3))), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(1)))));
        failure("for i in \"hello\": x = 1 end");
        failure("for i in b: x = 1");
        failure("for i b : x = 1 end");
        failure("for i in b x = 1 end");
    }

    @Test
    public void testFunctionDef() {
        this.rule = parser.function_def;
        successExpect("def fun(a, b): x = 1 return 2 end", new FunctionDefinitionNode(new IdentifierNode("fun"), Arrays.asList(new IdentifierNode("a"), new IdentifierNode("b")), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(1)), new ReturnNode(new IntegerNode(2)))));
        successExpect("def fun(a, b): x = 1 end", new FunctionDefinitionNode(new IdentifierNode("fun"), Arrays.asList(new IdentifierNode("a"), new IdentifierNode("b")), Arrays.asList(new VariableAssignmentNode(new IdentifierNode("x"), new IntegerNode(1)))));
        successExpect("def fun(): end", new FunctionDefinitionNode(new IdentifierNode("fun"), null, Arrays.asList(new ASTNode[]{})));
        successExpect("def fun(): return end", new FunctionDefinitionNode(new IdentifierNode("fun"), null, Arrays.asList(new ReturnNode(null))));
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
        successExpect("print(3+4)", new UnaryNode(new AddNode(new IntegerNode(3), new IntegerNode(4)), UnaryNode.PRINT));
        successExpect("print ( True )", new UnaryNode(new BoolNode(true), UnaryNode.PRINT));
        successExpect("print()", new UnaryNode(null, UnaryNode.PRINT));
        failure("print())");
        /* println */
        successExpect("println(\"hello world\")", new UnaryNode(new StringNode("hello world"), UnaryNode.PRINTLN));
        successExpect("println(1)", new UnaryNode(new IntegerNode(1), UnaryNode.PRINTLN));
        successExpect("println(3+4)", new UnaryNode(new AddNode(new IntegerNode(3), new IntegerNode(4)), UnaryNode.PRINTLN));
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
        successExpect("indexer({\"a\" : 2})", new UnaryNode(new MapNode(Arrays.asList(new PairNode(new StringNode("a"), new IntegerNode(2)))), UnaryNode.INDEXER));
        successExpect("indexer\t( fun(3, \"hello\"))", new UnaryNode(new FunctionCallNode(new IdentifierNode("fun"), Arrays.asList(new IntegerNode(3), new StringNode("hello"))), UnaryNode.INDEXER));
        failure("indexer()");
        failure("indexer");
        failure("indexer([1, 2])");
    }
}