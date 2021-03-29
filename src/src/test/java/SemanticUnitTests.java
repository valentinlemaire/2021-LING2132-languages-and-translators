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

    @Override protected Object parse (String input) {
        this.input = input;
        return autumnFixture.success(input).topValue();
    }

    @Override protected String astNodeToString (Object ast) {
        return ast.toString();
    }

    // ---------------------------------------------------------------------------------------------

    @Override protected void configureSemanticAnalysis (Reactor reactor, Object ast) {
        Walker<ASTNode> walker = SemanticAnalysis.createWalker(reactor);
        walker.walk(((ASTNode) ast));
    }

    // ---------------------------------------------------------------------------------------------

    @Test public void testRange() {
        successInput(   "a = range(12)");
        successInput(   "a = range(len(args))");
    }

    @Test public void testFunctionDefinition() {
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

    @Test public void testIfCondition() {
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

    @Test public void testWhileLoop() {
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

    @Test public void testForLoop() {
        // TODO
    }

}
