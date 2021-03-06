import norswap.autumn.Autumn;
import norswap.autumn.Grammar;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.autumn.actions.ActionContext;
import norswap.autumn.positions.LineMapString;

import ast.*;

public final class Parser extends Grammar {

    // Lexer
    // TODO : using reserved makes the whole thing broken
    // public rule TRUE = reserved("True").as_val(new BoolNode(true));
    // public rule FALSE = reserved("False").as_val(new BoolNode(false));

    public rule EQ  = word("==");
    public rule LEQ = word("<=");
    public rule GEQ = word(">=");
    public rule NEQ = word("!=");
    public rule L   = word("<");
    public rule G   = word(">");

    // Comments
    public rule not_line = seq(str("\n").not(), any);
    public rule line_comment = seq("#", not_line.at_least(0), str("\n").opt());

    { ws = choice(usual_whitespace, line_comment); }

    // EXPRESSIONS

    // Variables
    public rule variable_literal = seq(choice(alpha, "_"), choice(alphanum, "_").at_least(0))
            .word()
            .push($ -> $.str());

    public rule variable_name = seq(opt("-").word(), variable_literal)
            .word()
            .push($ -> $.$list().size() == 2 ? new VariableNode($.$1(), ((String) $.$0()).replace(" ", "")) : new VariableNode($.$0(), "+"));

    // Strings
    public rule not_quote = seq(str("\"").not(), any).at_least(0).push($ -> $.str());
    public rule string = choice(seq('"', not_quote, '"')).word().push($ -> new StringNode($.$0()));

    // Integers
    public rule integer = seq(opt("-"), digit.at_least(1), not(variable_name))
            .word()
            .push($ -> new IntegerNode(Integer.parseInt($.str().replace(" ", ""))));

    // Regrouping numerical values
    public rule numerical_value = choice(integer, variable_name);

    // Multiplication, Division and modulo
    public rule multiplication = lazy(() -> left_expression()
            .operand(choice(this.bracketed_operation, numerical_value))
            .infix(word("*"), $ -> new MultNode($.$0(), $.$1()))
            .infix(word("/"), $ -> new DivNode ($.$0(), $.$1()))
            .infix(word("%"), $ -> new ModNode ($.$0(), $.$1()))
            .requireOperator())
            .word();

    // Addition and Subtraction
    public rule addition = lazy(() -> left_expression()
            .operand(choice(this.bracketed_operation, multiplication, numerical_value))
            .infix(word("+"), $ -> new AddNode($.$0(), $.$1()))
            .infix(word("-"), $ -> new SubNode($.$0(), $.$1()))
            .requireOperator())
            .word();

    // Operations on numerical values
    public rule unbracketed_operation = choice(addition, multiplication, numerical_value);

    public rule bracketed_operation = lazy(() -> seq(word("("), this.operation, word(")")));

    public rule operation = lazy(() -> choice(bracketed_operation, unbracketed_operation));

    // Booleans
    public rule unbracketed_comparison = lazy(() -> left_expression()
            .operand(choice(operation, this.bracketed_comparison))
            .infix(EQ,  $ -> new BoolNode(BoolNode.EQ,  $.$0(), $.$1()))
            .infix(LEQ, $ -> new BoolNode(BoolNode.LEQ, $.$0(), $.$1()))
            .infix(GEQ, $ -> new BoolNode(BoolNode.GEQ, $.$0(), $.$1()))
            .infix(NEQ, $ -> new BoolNode(BoolNode.NEQ, $.$0(), $.$1()))
            .infix(L,   $ -> new BoolNode(BoolNode.L,   $.$0(), $.$1()))
            .infix(G,   $ -> new BoolNode(BoolNode.G,   $.$0(), $.$1()))
            .requireOperator())
            .word();

    public rule bracketed_comparison = lazy(() -> seq(word("("), this.comparison, word(")")));

    public rule comparison = choice(bracketed_comparison, unbracketed_comparison);

    public rule bool = lazy(() -> choice(comparison)); // TODO add true and false

    // Array declaration
    // TODO

    // Map declaration
    // TODO

    // Regrouping expressions
    public rule expression = choice(operation, string, bool); // TODO: add array and map


    // STATEMENTS

    // Variable assignment
    public rule variable_assignment = seq(variable_name, word("="), operation)
                                        .word()
                                        .push($ -> new VariableAssignmentNode($.$0(), $.$1()));

    // if
    // TODO

    // while
    // TODO

    // for (EXTRA)
    // TODO

    // Function definition
    // TODO

    // SPECIAL FUNCTIONS AND OBJECT ACCESSES

    // Array access
    // TODO

    // Map access
    // TODO

    // print
    // TODO

    // Program arguments
    // TODO

    // Parsing strings into integers
    // TODO

    // EXTRAS
    // range function
    // TODO

    // indexer function (for map)
    // TODO




    public rule statement = choice(variable_assignment);

    public rule root = choice(expression, statement);

    @Override
    public rule root() {
        return root;
    }

    public ParseResult parse (String input) {
        ParseResult result = Autumn.parse(root, input, ParseOptions.get());
        if (result.fullMatch) {
            System.out.println(result.toString());
        } else {
            // debugging
            System.out.println(result.toString(new LineMapString(input), false, "<input>"));
            // for users
            System.out.println(result.userErrorString(new LineMapString(input), "<input>"));
        }
        return result;
    }
}