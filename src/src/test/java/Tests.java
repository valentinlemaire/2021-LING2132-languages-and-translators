import ast.*;

import org.testng.annotations.Test;
import norswap.autumn.TestFixture;

import java.util.ArrayList;
import java.util.Arrays;


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
        successExpect("[\t]", new ArrayNode(Arrays.asList(new ASTNode[]{})));
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
}