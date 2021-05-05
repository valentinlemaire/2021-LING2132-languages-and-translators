import ast.ASTNode;
import interpreter.Interpreter;
import interpreter.None;
import interpreter.PassthroughException;
import norswap.autumn.AutumnTestFixture;
import norswap.utils.TestFixture;
import norswap.utils.visitors.Walker;
import norswap.uranium.Reactor;
import org.junit.Test;

import java.lang.annotation.Annotation;

import static norswap.utils.Util.cast;


public class InterpreterUnitTests extends TestFixture {

    private String input;
    private ASTNode tree;
    private Reactor reactor;
    private Interpreter interpreter;

    private final NSParser grammar = new NSParser();
    private final AutumnTestFixture autumnFixture = new AutumnTestFixture();

    {
        autumnFixture.rule = grammar.root();
        autumnFixture.runTwice = false;
        autumnFixture.bottomClass = this.getClass();
    }

    /*******************************************
                        UTILS
     *******************************************/

    protected void configureSemanticAnalysis (Reactor reactor, Object ast) {
        Walker<ASTNode> walker = SemanticAnalysis.createWalker(reactor);
        walker.walk(((ASTNode) ast));
    }

    protected Object parse (String input) {
        this.input = input;
        return autumnFixture.success(input).topValue();
    }


    protected void successExpect(String input, Object expected) {
        tree = (ASTNode) parse(input);
        reactor = new Reactor();
        configureSemanticAnalysis(reactor, tree);
        reactor.run();
        assertTrue(reactor.errors().isEmpty(), "Semantic analysis failed: "+reactor.reportErrors(Object::toString));


        interpreter = new Interpreter(reactor);
        Object result = interpreter.interpret(tree);
        assertEquals(result, expected, 1, () -> "");
    }

    protected void failure(String input) {
        tree = (ASTNode) parse(input);
        reactor = new Reactor();
        configureSemanticAnalysis(reactor, tree);
        reactor.run();
        assertTrue(reactor.errors().isEmpty(), "Semantic analysis failed: "+reactor.reportErrors(Object::toString));

        interpreter = new Interpreter(reactor);

        try {
            interpreter.interpret(tree);
            throw new RuntimeException("Interpretation succeeded when it was expected to fail");
        } catch (RuntimeException ignored) { }
    }


    /*******************************************
                        TESTS
     *******************************************/

    // PRIMITIVE LITERALS

    @Test
    public void testInteger() {
        successExpect("1", 1);
        successExpect("124", 124);
    }

    @Test
    public void testBool() {
        successExpect("True", true);
        successExpect("False", false);
    }

    @Test
    public void testNone() {
        successExpect("None", None.INSTANCE);
    }

    @Test
    public void testString() {
        successExpect("\"test\"", "test");
        successExpect("\"this\nis\na\ntest\"", "this\nis\na\ntest");
    }

    // VARIABLES

    @Test
    public void testIdentifier() {
        // TODO
    }

    @Test
    public void testVarAssignment() {
        // TODO
    }

    // COLLECTIONS

    @Test
    public void testMap() {
        // TODO
    }

    @Test
    public void testArrray() {
        // TODO
    }

    // UNARY OPERATIONS

    @Test
    public void testRange() {
        // TODO
    }

    @Test
    public void testIndexer() {
        // TODO
    }

    @Test
    public void testSort() {
        // TODO
    }

    @Test
    public void testParseInt() {
        // TODO
    }

    @Test
    public void testPrint() {
        // TODO
    }

    @Test
    public void testNegation() {
        // TODO
    }

    @Test
    public void testNot() {
        // TODO
    }

    @Test
    public void testReturn() {
        // TODO
    }

    @Test
    public void testLen() {
        // TODO
    }

    // BINARY OPERATIONS

    @Test
    public void testArithmeticOperation() {
        // TODO
    }

    @Test
    public void testLogicOperation() {
        // TODO
    }

    @Test
    public void testEqualityComparison() {
        // TODO
    }

    @Test
    public void testInequalityComparison() {
        // TODO
    }

    @Test
    public void testIndexAccess() {
        // TODO
    }

    // STATEMENTS

    @Test
    public void testIf() {
        // TODO
    }

    @Test
    public void testFor() {
        // TODO
    }

    @Test
    public void testWhile() {
        // TODO
    }

    // FUNCTIONS

    @Test
    public void testFunctionDefinition() {
        // TODO
    }

    @Test
    public void testFunctionCall() {
        // TODO
    }

}
