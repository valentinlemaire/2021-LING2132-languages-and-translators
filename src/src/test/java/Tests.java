import ast.*;

import org.testng.annotations.Test;
import norswap.autumn.TestFixture;


public class Tests extends TestFixture {

    Parser parser = new Parser();

    @Test
    public void testSum() {
        this.rule = parser.root;
        success("1 + 1");
    }

    @Test
    public void testOperations() {
        this.rule = parser.root;
        successExpect("1 + 1 + 1000", new AddNode(new AddNode(new IntegerNode(1), new IntegerNode(1)), new IntegerNode(1000)));
        successExpect("1 + 90 * 85", new AddNode(new IntegerNode(1), new MultNode(new IntegerNode(90), new IntegerNode(85))));
        successExpect("1 - 1", new SubNode(new IntegerNode(1), new IntegerNode(1)));
        successExpect("1 / 2+ 1", new AddNode(new DivNode(new IntegerNode(1), new IntegerNode(2)), new IntegerNode(1)));
        successExpect("-1 + 1", new AddNode(new IntegerNode(-1), new IntegerNode(1)));
        successExpect("5 * 4 + 20", new AddNode(new MultNode(new IntegerNode(5), new IntegerNode(4)), new IntegerNode(20)));
        successExpect("5 + 4 % 2 - 6", new SubNode(new AddNode(new IntegerNode(5), new ModNode(new IntegerNode(4), new IntegerNode(2))), new IntegerNode(6)));
        failure("5 + +1");
    }

    @Test
    public void testString() {
        this.rule = parser.string;
        successExpect("\"Coucou petite perruche\"", new StringNode("Coucou petite perruche"));
        failure("\" failing string");
        successExpect("\" this is'\t a test\n string\"", new StringNode(" this is'\t a test\n string"));

    }

    @Test
    public void testBool() {
        this.rule = parser.bool;
        // successExpect("True", new BoolNode(true));
        // successExpect("False", new BoolNode(false));
        successExpect("1 + 1 <= 5", new BoolNode("<=", new AddNode(new IntegerNode(1), new IntegerNode(1)), new IntegerNode(5)));
        //successExpect("True == False", new BoolNode("==", new BoolNode(true), new BoolNode(false)));

    }


}