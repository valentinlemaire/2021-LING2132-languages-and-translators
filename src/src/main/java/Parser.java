import norswap.autumn.Autumn;
import norswap.autumn.Grammar;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.autumn.actions.ActionContext;
import norswap.autumn.positions.LineMapString;

import ast.*;

import java.util.List;

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
    public rule IN      = reserved("in");
    public rule WHILE   = reserved("while");
    public rule IF      = reserved("if");
    public rule ELSE    = reserved("else");
    public rule ELSIF   = reserved("elsif");
    public rule DEF     = reserved("def");
    public rule END     = reserved("end");
    public rule PRINT   = reserved("print");
    public rule PRINTLN = reserved("println");
    public rule RANGE   = reserved("range");
    public rule INDEXER = reserved("indexer");
    public rule RETURN  = reserved("return");
    public rule SORT    = reserved("sort");
    public rule INT     = reserved("int");
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
    public rule string = seq('"', not_quote, '"').word().push($ -> new StringNode($.$0()));

    // Integers
    public rule integer = seq(digit.at_least(1), not(identifier))
            .word()
            .push($ -> new IntegerNode(Integer.parseInt($.str())));

    // Basic boolean values
    public rule boolean_values = choice(TRUE, FALSE);

    // Function calls
    public rule list = lazy(() -> left_expression()
            .operand(this.expression)
            .infix(word(","))).push(ActionContext::$list);

    public rule function_call = lazy(() -> seq(identifier, word("("), list.or_push_null(), word(")")))
                                .push($ -> new FunctionCallNode($.$0(), $.$1()));

    // Array and map access
    public rule indexer_access = lazy(() -> seq(choice(function_call, identifier), word("["), this.expression ,word("]")))
            .push($ -> new BinaryNode($.$0(), $.$1(), BinaryNode.IDX_ACCESS));

    public rule multiple_indexer_access = lazy(() -> left_expression()
            .left(indexer_access)
            .suffix(seq(word("["), this.expression, word("]")), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.IDX_ACCESS)));


    // Program arguments
    public rule program_args = lazy(() -> seq(ARGS, word("["), this.expression, word("]")).push($ -> new UnaryNode($.$0(), UnaryNode.ARG_ACCESS)));

    public rule any_value = choice(multiple_indexer_access, function_call, program_args, identifier);

    // NUMERICAL OPERATIONS

    // Multiplication, Division and Modulo
    public rule multiplication = lazy(() -> left_expression()
            .operand(this.numerical_operator)
            .infix(word("*"), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.MUL))
            .infix(word("/"), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.DIV))
            .infix(word("%"), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.MOD)))
            .word();

    // Addition and Subtraction
    public rule addition = lazy(() -> left_expression()
            .operand(choice(multiplication, this.numerical_negation))
            .infix(word("+"), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.ADD))
            .infix(word("-"), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.SUB)))
            .word();

    // Negation
    public rule numerical_negation = seq(word("-"), multiplication)
            .push($ -> new UnaryNode($.$0(), UnaryNode.NEGATION));

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
            .infix(word("<"),  $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.L))
            .infix(word(">"),  $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.G))
            .infix(word("=="), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.EQ))
            .infix(word("<="), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.LEQ))
            .infix(word(">="), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.GEQ))
            .infix(word("!="), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.NEQ)))
            .word();


    // Logical operations
    public rule and_operation = lazy(() -> left_expression()
            .operand(comparison)
            .infix(AND, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.AND)))
            .word();


    public rule or_operation = lazy(() -> left_expression()
            .operand(and_operation)
            .infix(OR, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.OR)))
            .word();


    // Handling of brackets
    public rule bracketed_primary_bool_operator = lazy(() -> seq(word("("), choice(or_operation, this.logical_negation), word(")")));

    // Used inside boolean operations
    public rule bool_operator = lazy(() -> choice(bracketed_primary_bool_operator, any_value, boolean_values));

    // Logical Negation
    public rule logical_negation = seq(NOT, bool_operator)
                                    .push($ -> new UnaryNode($.$0(), UnaryNode.NOT));

    // Final boolean expression
    public rule bool = lazy(() -> choice(or_operation, logical_negation, bracketed_primary_bool_operator));


    // OBJECT DECLARATIONS (arrays and maps)

    // Array declaration
    public rule full_array = seq(word("["), list.or_push_null(), word("]")).push($ -> new ArrayNode((List<ASTNode>) $.$0()));

    public rule empty_array = seq(word("["), word(":"), numerical_operation, word("]")).push($ -> new ArrayNode((ASTNode) $.$0()));

    public rule array = choice(full_array, empty_array);

    // Map declaration
    public rule map_element = lazy(() -> seq(this.expression, word(":"), this.expression)).push($ -> new BinaryNode($.$0(), $.$1(), BinaryNode.PAIR));

    public rule map_elements_list = lazy(() -> left_expression()
            .operand(this.map_element)
            .infix(word(",")))
            .push(ActionContext::$list);

    public rule map = seq(word("{"), map_elements_list, word("}")).push($ -> new MapNode($.$0()));


    // Regrouping expressions
    public rule expression = choice(numerical_operation, string, map, array, bool, NONE);

    // STATEMENTS

    // Variable assignment
    public rule variable_assignment = left_expression()
                                        .left(choice(multiple_indexer_access, identifier))
                                        .infix(word("="))
                                        .right(expression)
                                        .push($ -> new BinaryNode($.$0(), $.$1(), BinaryNode.VAR_ASSGNMT));

    // return statement

    public rule return_ = seq(RETURN, this.expression.or_push_null()).push($ -> new UnaryNode($.$0(), UnaryNode.RETURN));

    // if

    public rule elsif_block = lazy(() -> seq(ELSIF, bool, word(":"), this.statement_sequence))
                                .push($ -> new ElseNode($.$0(), $.$1()));

    public rule else_block = lazy(() -> seq(ELSE, word(":"), this.statement_sequence)).push($ -> new ElseNode(null, $.$0()));

    public rule elsif_sequence = seq(elsif_block.at_least(1), else_block.opt()).push(ActionContext::$list);

    public rule if_ = lazy(() -> seq(IF, bool, word(":"), this.statement_sequence, elsif_sequence.or_push_null(), END)).push($ -> new IfNode($.$0(), $.$1(), $.$2()));

    // while
    public rule while_ = lazy(() -> seq(WHILE, bool, word(":"), this.statement_sequence, END))
                            .push($ -> new WhileNode($.$0(), $.$1()));


    // for (EXTRA)
    public rule for_ = lazy(() -> seq(FOR, identifier, IN, choice(any_value, array), word(":"), this.statement_sequence, END)) // TODO add range and indexer
                            .push($ -> new ForNode($.$0(), $.$1(), $.$2()));

    // Function definition
    public rule function_def = lazy(() -> seq(DEF, identifier, word("("), list.or_push_null(), word(")"), word(":"), this.statement_sequence, END))
                        .push($ -> new FunctionDefinitionNode($.$0(), $.$1(), $.$2()));

    // SPECIAL FUNCTIONS

    // print and println
    public rule print_in_line = seq(PRINT, word("("), expression.or_push_null(), word(")")).push($ -> new UnaryNode($.$0(), UnaryNode.PRINT));

    public rule print_new_line = seq(PRINTLN, word("("), expression.or_push_null(), word(")")).push($ -> new UnaryNode($.$0(), UnaryNode.PRINTLN));

    public rule print = choice(print_in_line, print_new_line);


    // Parsing strings into integers
    public rule parse_int = seq(INT, word("("), choice(string, any_value), word(")")).push($ -> new UnaryNode($.$0(), UnaryNode.PARSE_INT));

    // sort function
    public rule sort = seq(SORT, word("("), choice(array, any_value), word(")")).push($ -> new UnaryNode($.$0(), UnaryNode.SORT));

    // EXTRAS
    // range function
    public rule range = seq(RANGE, word("("), choice(integer, any_value), word(")")).push($ -> new UnaryNode($.$0(), UnaryNode.RANGE));

    // indexer function (for map)
    public rule indexer = seq(INDEXER, word("("), choice(map, any_value), word(")")).push($ -> new UnaryNode($.$0(), UnaryNode.INDEXER));


    public rule statement = choice(variable_assignment, if_, while_, for_, function_def, print, return_, parse_int, sort);

    public rule statement_sequence = choice(statement, line_comment, expression).at_least(0).push(ActionContext::$list);

    public rule root = statement_sequence;

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