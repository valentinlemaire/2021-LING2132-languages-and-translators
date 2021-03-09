import norswap.autumn.Autumn;
import norswap.autumn.Grammar;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.autumn.actions.ActionContext;
import norswap.autumn.positions.LineMapString;

import ast.*;

public final class Parser extends Grammar {

    // Comments
    public rule not_line = seq(str("\n").not(), any);
    public rule line_comment = seq("#", not_line.at_least(0), str("\n").opt());

    { ws = choice(usual_whitespace, line_comment); }

    // EXPRESSIONS

    // Variable name parts (needs to be initialized here for lexer)

    public rule id_start = choice(alpha, "_");
    { id_part = choice(alphanum, "_"); }

    // Lexer

    public rule TRUE    = reserved("True").as_val(new BoolNode(true));
    public rule FALSE   = reserved("False").as_val(new BoolNode(false));
    public rule NONE    = reserved("None").as_val(new NoneNode());

    public rule FOR     = reserved("for");
    public rule WHILE   = reserved("while");
    public rule IF      = reserved("if");
    public rule ELSE    = reserved("else");
    public rule DEF     = reserved("def");
    public rule END     = reserved("end");
    public rule PRINT   = reserved("print");
    public rule PRINTLN = reserved("println");
    public rule RANGE   = reserved("range");
    public rule INDEXER = reserved("indexer");
    public rule RETURN  = reserved("return");
    public rule SORT    = reserved("sort");
    public rule ARGS    = reserved("args");
    public rule NOT     = reserved("not");
    public rule AND     = reserved("and");
    public rule OR      = reserved("or");

    // VALUES

    // Variable names
    public rule identifier = identifier(seq(id_start, id_part.at_least(0)))
                                    .push($ -> new IdentifierNode($.str()));


    // Strings
    public rule not_quote = seq(str("\"").not(), any).at_least(0).push(ActionContext::str);
    public rule string = choice(seq('"', not_quote, '"')).word().push($ -> new StringNode($.$0()));

    // Integers
    public rule integer = seq(digit.at_least(1), not(identifier))
            .word()
            .push($ -> new IntegerNode(Integer.parseInt($.str())));

    // Basic boolean values
    public rule boolean_values = choice(TRUE, FALSE);

    // Function calls
    public rule function_args = lazy(() -> left_expression()
            .operand(this.expression)
            .infix(word(",")))
            .push(ActionContext::$list);

    public rule function_call = seq(identifier, word("("), function_args.or_push_null(), word(")"))
            .push($ -> new FunctionCallNode($.$0(), $.$1()));

    // Array and map access TODO: double array indexing not supported : a[1][2]
    public rule indexer_access = lazy(() -> seq(choice(function_call, identifier), word("["), this.expression ,word("]")))
                                .push($ -> new IndexerAccessNode($.$0(), $.$1()));


    public rule multiple_indexer_access = lazy(() -> left_expression()
                                            .left(indexer_access)
                                            .suffix(seq(word("["), this.expression, word("]")), $ -> new IndexerAccessNode($.$0(), $.$1())));


    public rule any_value = choice(multiple_indexer_access, function_call, identifier);


    // NUMERICAL OPERATIONS

    // Multiplication, Division and Modulo
    public rule multiplication = lazy(() -> left_expression()
            .operand(this.numerical_operator)
            .infix(word("*"), $ -> new MultNode($.$0(), $.$1()))
            .infix(word("/"), $ -> new DivNode ($.$0(), $.$1()))
            .infix(word("%"), $ -> new ModNode ($.$0(), $.$1())))
            .word();

    // Addition and Subtraction
    public rule addition = lazy(() -> left_expression()
            .operand(choice(multiplication, this.numerical_negation))
            .infix(word("+"), $ -> new AddNode($.$0(), $.$1()))
            .infix(word("-"), $ -> new SubNode($.$0(), $.$1())))
            .word();

    // Negation
    public rule numerical_negation = seq(word("-"), multiplication)
            .push($ -> new NegationNode($.$0()));

    // Basic operands without operations or brackets
    public rule primary_numerical_operator = choice(addition, numerical_negation);

    // Handling of brackets
    public rule bracketed_primary_numerical_operator = lazy(() -> seq(word("("), this.numerical_operation, word(")")));

    // Used inside numerical operations
    public rule numerical_operator = lazy(() -> choice(bracketed_primary_numerical_operator, integer, any_value));

    // Final numerical operation
    public rule numerical_operation = choice(primary_numerical_operator, bracketed_primary_numerical_operator);

    // BOOLEAN OPERATIONS

    // Value comparison
    public rule comparison = lazy(() -> left_expression()
            .operand(choice(numerical_operation, this.bool_operator))
            .infix(word("<"),  $ -> new ComparisonNode(ComparisonNode.L,   $.$0(), $.$1()))
            .infix(word(">"),  $ -> new ComparisonNode(ComparisonNode.G,   $.$0(), $.$1()))
            .infix(word("=="), $ -> new ComparisonNode(ComparisonNode.EQ,  $.$0(), $.$1()))
            .infix(word("<="), $ -> new ComparisonNode(ComparisonNode.LEQ, $.$0(), $.$1()))
            .infix(word(">="), $ -> new ComparisonNode(ComparisonNode.GEQ, $.$0(), $.$1()))
            .infix(word("!="), $ -> new ComparisonNode(ComparisonNode.NEQ, $.$0(), $.$1()))
            .requireOperator())
            .word();


    // Logical operations
    public rule logical_operation = lazy(() -> left_expression()
            .operand(this.bool_operator)
            .infix(AND, $ -> new AndNode($.$0(), $.$1()))
            .infix(OR,  $ -> new OrNode ($.$0(), $.$1())))
            .word();

    // Basic operands without operations and brackets
    public rule primary_bool_operator = lazy(() -> choice(comparison, logical_operation));

    // Handling of brackets
    public rule bracketed_primary_bool_operator = lazy(() -> seq(word("("), this.bool, word(")")));

    // Used inside boolean operations
    public rule bool_operator = lazy(() -> choice(bracketed_primary_bool_operator, this.logical_negation, boolean_values, any_value));

    // Logical Negation
    public rule logical_negation = seq(NOT, bool_operator)
                                    .push($ -> new NotNode($.$0()));

    // Final boolean expression
    public rule bool = lazy(() -> choice(primary_bool_operator, bracketed_primary_bool_operator));


    // OBJECT DECLARATIONS (arrays and maps)

    // Array declaration
    // TODO
    /* Tried to make it work, but not yet working */
    public rule not_bracket = seq(str("]").not(), any).at_least(0).push(ActionContext::str);
    public rule array = choice(seq('[', not_bracket.sep(0, ','), ']')).word().push($ -> new ArrayNode($.$0()));

    // Map declaration
    // TODO

    // Regrouping expressions
    public rule expression = choice(numerical_operation, string, bool); // TODO : add array and map declarations


    // STATEMENTS

    // Variable assignment
    public rule variable_assignment = left_expression()
                                        .left(any_value)
                                        .infix(word("="))
                                        .right(expression)
                                        .push($ -> new VariableAssignmentNode($.$0(), $.$1()));
    // if
    // TODO

    // while
    // TODO

    // for (EXTRA)
    // TODO

    // Function definition
    // TODO

    // SPECIAL FUNCTIONS

    // print and println
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

    public rule root = choice(expression, statement, line_comment).at_least(0);

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