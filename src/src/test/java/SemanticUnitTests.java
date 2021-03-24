import ast.ASTNode;
import norswap.autumn.AutumnTestFixture;
import norswap.autumn.positions.LineMapString;
import norswap.uranium.Reactor;
import norswap.uranium.UraniumTestFixture;
import norswap.utils.visitors.Walker;
import org.testng.annotations.Test;

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
}
