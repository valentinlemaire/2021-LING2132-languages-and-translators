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
        } catch (PassthroughException ignored) { }
    }


    /*******************************************
                        TESTS
     *******************************************/

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

}
