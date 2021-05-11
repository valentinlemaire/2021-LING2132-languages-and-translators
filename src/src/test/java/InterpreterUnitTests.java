import Types.PolymorphArray;
import Types.PolymorphMap;
import ast.ASTNode;
import interpreter.Interpreter;
import interpreter.None;
import norswap.autumn.AutumnTestFixture;
import norswap.utils.TestFixture;
import norswap.utils.visitors.Walker;
import norswap.uranium.Reactor;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;


public class InterpreterUnitTests extends TestFixture {

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
        return autumnFixture.success(input).topValue();
    }


    protected void successExpect(String input, Object expected) {
        tree = (ASTNode) parse(input);
        reactor = new Reactor();
        configureSemanticAnalysis(reactor, tree);
        reactor.run();
        assertTrue(reactor.errors().isEmpty(), "Semantic analysis failed: "+reactor.reportErrors(Object::toString));


        interpreter = new Interpreter(reactor, new String[0]);
        Object result = interpreter.interpret(tree);
        assertEquals(result, expected, 1, () -> "");
    }

    protected void failure(String input) {
        tree = (ASTNode) parse(input);
        reactor = new Reactor();
        configureSemanticAnalysis(reactor, tree);
        reactor.run();
        assertTrue(reactor.errors().isEmpty(), "Semantic analysis failed: "+reactor.reportErrors(Object::toString));

        interpreter = new Interpreter(reactor, new String[0]);

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
        successExpect("1", (long) 1);

        successExpect("124", (long) 124);
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
    public void testVariables() {
        successExpect("a = 1\na", (long) 1);

        successExpect("a = \"yo\"\na", "yo");

        successExpect("a = None\na", None.INSTANCE);

        successExpect("a = True\na", true);
    }

    // COLLECTIONS

    @Test
    public void testMap() {
        PolymorphMap map = new PolymorphMap();
        map.put((long) 1, "yo");
        map.put("test", (long) 3);
        map.put(false, None.INSTANCE);
        successExpect("{1:\"yo\", \"test\":3, False:None}", map);

        PolymorphMap map2 = new PolymorphMap();
        map2.put((long) 1, "yo");
        map2.put(true, map);
        map2.put("this is getting complicated", new PolymorphArray((long) 1, None.INSTANCE, false));

        successExpect("{1: \"yo\", True: {1:\"yo\", \"test\":3, False:None}, \"this is getting complicated\": [1, None, False]}", map2);

        failure("a = [None]\n" +
                      "{a[0]:2}");

        failure("a = {True: {3:5}}\n" +
                      "{a[True]:2}");

    }

    @Test
    public void testArray() {
        successExpect("[None, 1, \"yo\", False]", new PolymorphArray(None.INSTANCE, (long) 1, "yo", false));

        successExpect("[:4]", new PolymorphArray(None.INSTANCE, None.INSTANCE, None.INSTANCE, None.INSTANCE));

        failure("def f(x):\n" +
                      "  return None\n" +
                      "end\n" +
                      "[:f(1)]");
    }

    @Test
    public void testListComprehension() {
        successExpect("a = [True, None, \"yo\", None, 4]\n" +
                            "b = [x for x in a if x != None]\n" +
                            "b", new PolymorphArray(true, "yo", (long) 4));

        successExpect("a = [True, None, \"yo\", None, 4]\n" +
                            "b = [[x] for x in a if x != None]\n" +
                            "b", new PolymorphArray(new PolymorphArray(true), new PolymorphArray("yo"), new PolymorphArray((long) 4)));

        successExpect("a = range(10)\n" +
                            "b = [x+1 for x in a if x % 2 == 0]\n" +
                            "b", new PolymorphArray((long) 1, (long) 3, (long) 5, (long) 7, (long) 9));

        successExpect("def f(a):\n" +
                            "  return a+1\n" +
                            "end\n" +
                            "a = range(10)\n" +
                            "b = [f(x) for x in a if x < 3]\n" +
                            "b", new PolymorphArray((long) 1, (long) 2, (long) 3));

        failure(  "def f(a):\n" +
                        "  return a+1\n" +
                        "end\n" +
                        "a = range(10)\n" +
                        "b = [x+1 for x in a if f(x)]\n" +
                        "b");



    }

    // UNARY OPERATIONS

    @Test
    public void testRange() {
        successExpect("range(3)", new PolymorphArray((long) 0, (long) 1, (long) 2));

        successExpect("a = 5\n" +
                            "range(a)", new PolymorphArray((long) 0, (long) 1, (long) 2, (long) 3, (long) 4));

        failure("def f(x):\n" +
                      "  return None\n" +
                      "end\n" +
                      "range(f(1))");
    }

    @Test
    public void testIndexer() {
        successExpect("a = {1: 3, \"yo\":None}\n" +
                            "indexer(a)", new PolymorphArray((long) 1, "yo"));

        successExpect("a = [1, 2, 3]\n" +
                            "indexer(a)", new PolymorphArray((long) 0, (long) 1, (long) 2));

        failure("def f(x):\n" +
                      "  return None\n" +
                      "end\n" +
                      "indexer(f(1))");
    }

    @Test
    public void testSort() {
        successExpect("a = [2, 5, 3]\n" +
                            "sort(a)", new PolymorphArray((long) 2, (long) 3, (long) 5));

        successExpect("a = [\"a\", \"c\", \"b\"]\n" +
                            "sort(a)", new PolymorphArray("a", "b", "c"));

        failure("a = [1, \"yo\"]\n" +
                      "sort(a)");
    }

    @Test
    public void testParseInt() {
        successExpect("int(\"2\")", (long) 2);

        successExpect("int(\"-2\")", (long) -2);

        failure("def f(x):\n" +
                      "  return None\n" +
                      "end\n" +
                      "int(f(1))");
    }

    @Test
    public void testPrint() {
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));

        successExpect("print(\"test\")", None.INSTANCE);
        String expected = "test";

        successExpect("println(\"test\")", None.INSTANCE);
        expected += "test\n";

        successExpect("println(1)", None.INSTANCE);
        expected += "1\n";

        successExpect("println({1:3, True:None, \"yo\": [1, None, False]})", None.INSTANCE);
        expected += "{1: 3, \"yo\": [1, None, False], True: None}\n";

        successExpect("println({1:{2: None, 3: 5}, True:None})", None.INSTANCE);
        expected += "{1: {2: None, 3: 5}, True: None}\n";
        assertEquals(expected, outContent.toString());

    }

    @Test
    public void testIO() {
        // read
        successExpect("f = open(\"src/assets/read_tests.txt\", \"r\")\n" +
                "read(f)", "Hello World");
        successExpect("f = open(\"src/assets/read_tests.txt\", \"r\")\n" +
                "a = read(f)\n" +
                "read(f)", "This is a remix");
        successExpect("f = open(\"src/assets/read_tests.txt\", \"r\")\n" +
                "a = read(f)" +
                "while  a != \"\":\n" +
                "   a = read(f)\n" +
                "end\n" +
                "read(f)", "NS is a real thing");
        successExpect("f = open(\"src/assets/read_tests.txt\", \"r\")\n" +
                "i = 1\n" +
                "while read(f) != \"\":\n" +
                "   i = i+1\n" +
                "end\n" +
                "read(f)", "NS is a real thing");
        successExpect("f = open(\"src/assets/read_tests.txt\", \"r\")\n" +
                "i = 1\n" +
                "while read(f) != \"\":\n" +
                "   i = i+1\n" +
                "end\n" +
                "i", (long) 4);

        // write
        successExpect("f = open(\"src/assets/write_tests.txt\", \"w\")\n" +
                "for i in range(5):\n" +
                "   write(f, i)\n" +
                "end\n" +
                "close(f)\n" +
                "f = open(\"src/assets/write_tests.txt\", \"r\")\n" +
                "read(f)\n read(f)\n read(f)", "2");

        // failures
        failure("f = open(\"src/assets/non_existant.txt\", \"w\")");
        failure("f = open(\"src/assets/write_tests.txt\", \"w\")\n" +
                "read(f)");
        failure("f = open(\"src/assets/read_tests.txt\", \"r\")\n" +
                "write(f, \"Hello World\")");
        failure("a = 2\n" +
                "read(a)");

    }

    @Test
    public void testNegation() {
        successExpect("a = 5\n" +
                            "b = -a\n" +
                            "b", (long) -5);

        successExpect("a = -10\n" +
                            "b = -a + 1\n" +
                            "b", (long) 11);

        failure("def f(x):\n" +
                      "  return None\n" +
                      "end\n" +
                      "b = -f(1)\n" +
                      "b");
    }

    @Test
    public void testNot() {
        successExpect("a = True\n" +
                "b = not a\n" +
                "b", false);

        successExpect("a = False\n" +
                "b = not a\n" +
                "b", true);

        failure("def f(x):\n" +
                      "  return None\n" +
                      "end\n" +
                      "b = not f(1)\n" +
                      "b");
    }


    @Test
    public void testLen() {
        successExpect("len([1, 2, 3])", (long) 3);

        successExpect("len({1: 2, 3: None})", (long) 2);

        failure("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "len(f(1))");
    }

    // BINARY OPERATIONS

    @Test
    public void testArithmeticOperation() {
        // basic operations
        successExpect("5+2", (long) 7);
        successExpect("5-2", (long) 3);
        successExpect("5*2", (long) 10);
        successExpect("5/2", (long) 2);
        successExpect("5%2", (long) 1);

        // with variables
        successExpect("a = 5\n" +
                "a+2", (long) 7);
        successExpect("a = 5\n" +
                "a-2", (long) 3);
        successExpect("a = 5\n" +
                "a*2", (long) 10);
        successExpect("a = 5\n" +
                "a/2", (long) 2);
        successExpect("a = 5\n" +
                "a%2", (long) 1);

        // with invalid return values in functions
        failure("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "f(1)+2");
        failure("def f(x):\n" +
                "  return True\n" +
                "end\n" +
                "f(1)-2");
        failure("def f(x):\n" +
                "  return \"hello world\"\n" +
                "end\n" +
                "f(1)*2");
        failure("def f(x):\n" +
                "  return False\n" +
                "end\n" +
                "f(1)/2");
        failure("def f(x):\n" +
                "  return [1, 2, 3]\n" +
                "end\n" +
                "f(1)%2");
    }

    @Test
    public void testLogicOperation() {
        // basic operations
        successExpect("True and True", true);
        successExpect("True and False", false);
        successExpect("False or True", true);
        successExpect("False or False", false);

        // with variables
        successExpect("a = False\n" +
                "a and True", false);
        successExpect("a = False\n" +
                "a and False", false);
        successExpect("a = True\n" +
                "a or False", true);
        successExpect("a = True\n" +
                "a or True", true);

        // with invalid return values in functions
        failure("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "f(1) and True");
        failure("def f(x):\n" +
                "  return 3\n" +
                "end\n" +
                "f(1) or True");
        failure("def f(x):\n" +
                "  return \"hello world\"\n" +
                "end\n" +
                "f(1) and True");
        failure("def f(x):\n" +
                "  return {1 : 2}\n" +
                "end\n" +
                "f(1) or True");
        failure("def f(x):\n" +
                "  return [1, 2, 3]\n" +
                "end\n" +
                "f(1) and False");

        // functions working
        successExpect("def f(x):\n" +
                "  return True\n" +
                "end\n" +
                "f(1) and False", false);
        successExpect("def f(x):\n" +
                "  return {\"hello\" : True}\n" +
                "end\n" +
                "f(1)[\"hello\"] or False", true);
        successExpect("def f(x):\n" +
                "  return {False : True}\n" +
                "end\n" +
                "f(1)[False] or False", true);
        successExpect("def f(x):\n" +
                "  return [True, False, 2, 3]\n" +
                "end\n" +
                "f(1)[1] or False", false);
    }

    @Test
    public void testEqualityComparison() {
        successExpect("True == True", true);
        successExpect("3 == 3", true);
        successExpect("2 == 3", false);
        successExpect("3 != 2", true);
        successExpect("\"hello\" == \"hello\"", true);
        successExpect("\"hello\" != \"world\"", true);
        successExpect("[True] == [True]", true);
        successExpect("[\"hello\", \"world\"] == [\"hello\", \"world\"]", true);
        successExpect("[\"hello\", \"world\"] == [True]", false);


        successExpect("def f(x):\n" +
                "  return [True, False, 2, 3]\n" +
                "end\n" +
                "f(1)[1] == False", true);
        successExpect("def f(x):\n" +
                "  return [True, False, 2, 3]\n" +
                "end\n" +
                "f(1)[2] == 2", true);
        successExpect("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "f(1) == None", true);
        failure("def f(x):\n" +
                "  x = x+1\n" +
                "end\n" +
                "f(1) == None");
    }

    @Test
    public void testInequalityComparison() {
        successExpect("3 <= 3", true);
        successExpect("2 >= 3", false);
        successExpect("3 > 2", true);
        successExpect("\"hello\" <= \"hello\"", true);
        successExpect("\"hello\" < \"world\"", true);

        successExpect("def f(x):\n" +
                "  return [True, False, 2, 3]\n" +
                "end\n" +
                "f(1)[2] <= 2", true);
        successExpect("def f(x):\n" +
                "  return [True, False, 2, 3]\n" +
                "end\n" +
                "f(1)[2] > 2", false);
        failure("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "f(1) > 3");
        failure("def f(x):\n" +
                "  x = x+1\n" +
                "end\n" +
                "f(1) < 3");
    }

    @Test
    public void testIndexAccess() {
        // Map
        successExpect("a = {\"\": \"Hi\", \"hello\": 3, \"world\": 2}\n" +
                "a[\"\"]", "Hi");
        successExpect("a = {\"\": \"Hi\", \"hello\": 3, \"world\": 2}\n" +
                "a[\"hello\"]", (long) 3);
        failure("a = {\"\": \"Hi\", \"hello\": 3, \"world\": 2}\n" +
                "a[\"World\"]");
        successExpect("a = {2: \"Hi\", True: 3, \"world\": 2}\n" +
                "a[2]", "Hi");
        successExpect("a = {2: \"Hi\", True: 3, \"world\": 2}\n" +
                "a[3 == 3]", (long) 3);
        successExpect("a = {2: \"Hi\", True: 3, \"world\": 2}\n" +
                "a[\"world\"]", (long) 2);

        // Arrays
        successExpect("a = [1, 2, \"Hello\", \"World\", True, False]\n" +
                "a[0]", (long) 1);
        successExpect("a = [1, 2, \"Hello\", \"World\", True, False]\n" +
                "a[2]", "Hello");
        successExpect("a = [1, 2, \"Hello\", \"World\", True, False]\n" +
                "a[4]", true);
        failure("a = [1, 2, \"Hello\", \"World\", True, False]\n" +
                "a[6]");

        // Different types
        failure("def f(x):\n" +
                "  return \"Hello\"\n" +
                "end\n" +
                "f(1)[4]");
        failure("def f(x):\n" +
                "  return {True: False, 2: 3}\n" +
                "end\n" +
                "f(1)[0]");
        failure("def f(x):\n" +
                "  return 3\n" +
                "end\n" +
                "f(1)[0]");
        failure("def f(x, y):\n" +
                "  return x or y\n" +
                "end\n" +
                "f(True, False)[0]");
    }

    // STATEMENTS

    @Test
    public void testIf() {
        successExpect("a = 0\n" +
                            "if True:\n" +
                            "  a = 1\n" +
                            "end\n" +
                            "a", (long) 1);

        successExpect("a = 0\n" +
                            "if True:\n" +
                            "  a = 1\n" +
                            "else:\n" +
                            "  a = 2\n" +
                            "end\n" +
                            "a", (long) 1);

        successExpect("a = 0\n" +
                            "if not True:\n" +
                            "  a = 1\n" +
                            "elsif True:\n" +
                            "  a = 2\n" +
                            "else:\n" +
                            "  a = 3\n" +
                            "end\n" +
                            "a", (long) 2);

        successExpect("a = True\n" +
                            "if not a:\n" +
                            "  a = 1\n" +
                            "elsif a:\n" +
                            "  a = \"yo\"\n" +
                            "else:\n" +
                            "  a = {2: False}\n" +
                            "end\n" +
                            "a", "yo");

        failure("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "if not f(1):\n" +
                "  a = 1\n" +
                "else:\n" +
                "  a = {2: False}\n" +
                "end\n");
    }

    @Test
    public void testFor() {
        successExpect("b = 1\n" +
                            "for a in [{1:2, 3:4}, \"yo\", 2]:\n" +
                            "  b = a\n" +
                            "end\n" +
                            "b", (long) 2);

        successExpect("b = 1\n" +
                            "for a in range(3):\n" +
                            "  b = a\n" +
                            "end\n" +
                            "b", (long) 2);

        successExpect("b = 1\n" +
                            "for a in indexer({\"yo\":2, 3:4}):\n" +
                            "  b = a\n" +
                            "end\n" +
                            "b", "yo");

        failure("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "b = 1\n" +
                "for a in f(1):\n" +
                "  b = a\n" +
                "end\n" +
                "b");
    }

    @Test
    public void testWhile() {
        successExpect("i = 1\n" +
                "while(i < 3):\n" +
                "    i = i+1\n" +
                "end\n" +
                "i\n", (long) 3);
        successExpect("i = 1\n" +
                "b = 1" +
                "while(i < 3):\n" +
                "    i = i+1\n" +
                "    b = 2\n" +
                "end\n" +
                "b", (long) 2);
        failure("def f(x):\n" +
                "  return None\n" +
                "end\n" +
                "b = 1\n" +
                "while f(1) > 2:\n" +
                "  b = 0\n" +
                "end\n" +
                "b");
    }

    // FUNCTIONS

    @Test
    public void testFunction() {
        successExpect("def f(x):\n" +
                "  return x + 2\n" +
                "end\n" +
                "3", (long) 3);
        successExpect("def f(x):\n" +
                "  x + 2\n" +
                "end\n" +
                "f(2)", None.INSTANCE);
        failure("def f(x):\n" +
                "  x + 2\n" +
                "end\n" +
                "a = f(2)");
        successExpect("def f(x):\n" +
                "  x + 2\n" +
                "  return None\n" +
                "end\n" +
                "a = f(2)\n" +
                "a", None.INSTANCE);
        successExpect("def f(x):\n" +
                "  return x + 2\n" +
                "end\n" +
                "f(2)", (long) 4);
        successExpect("def f(x, y):\n" +
                "  return x or y\n" +
                "end\n" +
                "f(True, False)", true);

        successExpect("def f(x, y):\n" +
                            "  if not not x:\n" +
                            "    return y\n" +
                            "  end\n" +
                            "  return \"nope\"\n" +
                            "end\n" +
                            "f(not False, \"yep\")", "yep");
    }
}
