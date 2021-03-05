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

    //
    public rule numerical_value = choice(integer, variable_name);
    public rule expression = lazy(() -> choice(this.operation, string, this.bool)); // TODO: add array and map

    // Multiplication, Division and modulo
    public rule multiplication = left_expression()
            .operand(numerical_value)
            .infix(word("*"), $ -> new MultNode($.$0(), $.$1()))
            .infix(word("/"), $ -> new DivNode($.$0(), $.$1()))
            .infix(word("%"), $ -> new ModNode($.$0(), $.$1()))
            .requireOperator()
            .word();

    // Addition and Subtraction
    public rule addition = left_expression()
            .operand(choice(multiplication, numerical_value))
            .infix(word("+"), $ -> new AddNode($.$0(), $.$1()))
            .infix(word("-"), $ -> new SubNode($.$0(), $.$1()))
            .requireOperator()
            .word();

    // Operations on numerical values
    public rule operation = choice(addition, multiplication, numerical_value);

    // Booleans
    public rule comparator = choice(seq(choice(set("!=")), "="), seq(choice(set("<>")), opt("=")))
            .word()
            .push(ActionContext::str);

    public rule comparison = seq(operation,
            comparator,
            operation)
            .word()
            .push($ -> new BoolNode($.$1(), $.$0(), $.$2()));

    public rule bool = choice(comparison); // TODO : add True and False as reserved words


    // TODO : array and map declarations

    // STATEMENTS

    // Variable assignment
    public rule variable_assignment = seq(variable_name, word("="), operation)
                                        .word()
                                        .push($ -> new VariableAssignmentNode($.$0(), $.$1()));

    // if


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