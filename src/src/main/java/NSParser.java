import norswap.autumn.Autumn;
import norswap.autumn.Grammar;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.autumn.actions.ActionContext;
import norswap.autumn.positions.LineMapString;

import ast.*;

import java.util.List;

public final class NSParser extends Grammar {

    public rule NEWLINE = str("\n");
    public rule HASH    = str("#");

    // Comments
    public rule not_line = seq(NEWLINE.not(), any);
    public rule line_comment = seq(HASH, not_line.at_least(0), str("\n").opt());

    { ws = choice(usual_whitespace, line_comment); }

    // EXPRESSIONS

    // Variable name parts (needs to be initialized here for lexer)

    public rule id_start = choice(alpha, "_");
    { id_part = choice(alphanum, "_"); }

    // Lexer

    public rule TRUE    = reserved("True").push($ -> new BoolNode(true));
    public rule FALSE   = reserved("False").push($ -> new BoolNode(false));
    public rule NONE    = reserved("None").push($ -> new NoneNode());

    public rule FOR      = reserved("for");
    public rule IN       = reserved("in");
    public rule WHILE    = reserved("while");
    public rule IF       = reserved("if");
    public rule ELSE     = reserved("else");
    public rule ELSIF    = reserved("elsif");
    public rule DEF      = reserved("def");
    public rule END      = reserved("end");
    public rule RETURN   = reserved("return");
    public rule FINAL    = reserved("final");
    public rule NOT      = reserved("not");
    public rule AND      = reserved("and");
    public rule OR       = reserved("or");

    public rule LPAREN   = word("(");
    public rule RPAREN   = word(")");
    public rule LBRACKET = word("[");
    public rule RBRACKET = word("]");
    public rule LBRACE   = word("{");
    public rule RBRACE   = word("}");
    public rule COLON    = word(":");
    public rule COMMA    = word(",");
    public rule QUOTE    = str("\"");
    public rule EQUAL    = word("=");
    public rule MUL      = word("*");
    public rule DIV      = word("/");
    public rule MOD      = word("%");
    public rule ADD      = word("+");
    public rule SUB      = word("-");
    public rule L        = word("<");
    public rule G        = word(">");
    public rule LEQ      = word("<=");
    public rule GEQ      = word(">=");
    public rule EQ       = word("==");
    public rule NEQ      = word("!=");


    // VALUES

    // Variable names
    public rule identifier_lit = identifier(seq(id_start, id_part.at_least(0))).push(ActionContext::str);

    public rule identifier = identifier_lit.push($ -> new IdentifierNode($.$0()));


    // Strings
    public rule not_quote = seq(QUOTE.not(), any).at_least(0).push(ActionContext::str);
    public rule string = seq(QUOTE, not_quote, QUOTE).word().push($ -> new StringNode($.$0()));

    // Integers
    public rule integer = seq(digit.at_least(1), not(identifier))
            .word()
            .push($ -> new IntegerNode(Long.parseLong($.str())));


    // Basic boolean values
    public rule boolean_values = choice(TRUE, FALSE);

    // Function calls
    public rule list = lazy(() -> left_expression()
            .operand(this.expression)
            .infix(COMMA)).push(ActionContext::$list);

    public rule function_call = lazy(() -> seq(identifier, LPAREN, list.or_push_null(), RPAREN))
                                .push($ -> new FunctionCallNode($.$0(), $.$1()));

    // Array and map access
    public rule indexer_access = lazy(() -> seq(choice(function_call, identifier), LBRACKET, this.expression ,RBRACKET))
            .push($ -> new BinaryNode($.$0(), $.$1(), BinaryNode.IDX_ACCESS));

    public rule multiple_indexer_access = lazy(() -> left_expression()
            .left(indexer_access)
            .suffix(seq(LBRACKET, this.expression, RBRACKET), $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.IDX_ACCESS)));

    public rule any_value = lazy(() -> choice(multiple_indexer_access, function_call, identifier, NONE));

    // NUMERICAL OPERATIONS

    // Multiplication, Division and Modulo
    public rule multiplication = lazy(() -> left_expression()
            .operand(this.numerical_operator)
            .infix(MUL, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.MUL))
            .infix(DIV, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.DIV))
            .infix(MOD, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.MOD)));

    // Addition and Subtraction
    public rule addition = lazy(() -> left_expression()
            .operand(choice(this.numerical_negation, multiplication))
            .infix(ADD, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.ADD))
            .infix(SUB, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.SUB)));

    // Negation
    public rule numerical_negation = lazy(() -> seq(SUB, multiplication))
            .push($ -> new UnaryNode($.$0(), UnaryNode.NEGATION));

    // Basic operands without operations or parenthesis
    public rule primary_numerical_operator = choice(addition);

    // Handling of parenthesis
    public rule paren_primary_numerical_operator = lazy(() -> seq(LPAREN, this.numerical_operation, RPAREN));

    // Used inside numerical operations
    public rule numerical_operator = lazy(() -> choice(paren_primary_numerical_operator, integer, any_value));

    // Final numerical operation
    public rule numerical_operation = choice(primary_numerical_operator, paren_primary_numerical_operator);

    // BOOLEAN OPERATIONS

    // Value comparison
    public rule comparison = lazy(() -> left_expression()
            .operand(choice(string, this.indexable, this.bool_operator, numerical_operation))
            .infix(L,   $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.L))
            .infix(G,   $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.G))
            .infix(LEQ, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.LEQ))
            .infix(GEQ, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.GEQ)));

    public rule eq_neq = lazy(() -> left_expression()
            .operand(choice(string, this.indexable, this.bool_operator, numerical_operation))
            .infix(EQ,  $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.EQ))
            .infix(NEQ, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.NEQ))
            .requireOperator());


    // Logical operations
    public rule and_operation = lazy(() -> left_expression()
            .operand(choice(eq_neq, comparison))
            .infix(AND, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.AND)));


    public rule or_operation = lazy(() -> left_expression()
            .operand(and_operation)
            .infix(OR, $ -> new BinaryNode($.$0(), $.$1(), BinaryNode.OR)));


    // Handling of parenthesis
    public rule paren_primary_bool_operator = lazy(() -> seq(LPAREN, choice(or_operation, this.logical_negation), RPAREN));

    // Used inside boolean operations
    public rule bool_operator = lazy(() -> choice(paren_primary_bool_operator, this.logical_negation, boolean_values));

    // Logical Negation
    public rule logical_negation = seq(NOT, choice(bool_operator, any_value))
                                    .push($ -> new UnaryNode($.$0(), UnaryNode.NOT));

    // Final boolean expression
    public rule bool = lazy(() -> choice(or_operation, logical_negation, paren_primary_bool_operator));


    // OBJECT DECLARATIONS (arrays and maps)

    // Array declaration
    public rule full_array = seq(LBRACKET, list.or_push_null(), RBRACKET).push($ -> new ArrayNode((List<ASTNode>) $.$0()));

    public rule empty_array = seq(LBRACKET, COLON, numerical_operation, RBRACKET).push($ -> new ArrayNode((ASTNode) $.$0()));

    public rule array = choice(full_array, empty_array);


    // Map declaration
    public rule map_element = lazy(() -> seq(this.expression, COLON, this.expression)).push($ -> new BinaryNode($.$0(), $.$1(), BinaryNode.PAIR));

    public rule map_elements_list = lazy(() -> left_expression()
            .operand(this.map_element)
            .infix(COMMA))
            .push(ActionContext::$list);

    public rule map = seq(LBRACE, map_elements_list.or_push_null(), RBRACE).push($ -> new MapNode($.$0()));

    public rule indexable = lazy(() -> choice(map, array, this.list_comprehension));


    // List comprehension
    public rule inlist_if = seq(IF, bool).push(ActionContext::$0);

    public rule list_comprehension = lazy(() -> seq(LBRACKET, this.expression, FOR, identifier, IN, choice(indexable, any_value), inlist_if.or_push_null(), RBRACKET))
                                                .push($ -> new ListComprehensionNode($.$0(), $.$1(), $.$2(), $.$3()));

    // Regrouping expressions
    public rule expression = choice(or_operation, NONE, indexable, string);

    // STATEMENTS

    public rule non_final_variable_assignment = seq(choice(multiple_indexer_access, identifier), EQUAL, expression)
                                        .push($ -> new VarAssignmentNode($.$0(), $.$1()));

    public rule final_variable_assignment = seq(FINAL, identifier, EQUAL, expression)
                                        .push($ -> new VarAssignmentNode($.$0(), $.$1(), true));

    public rule variable_assignment = choice(final_variable_assignment, non_final_variable_assignment);

    // return statement

    public rule return_ = seq(RETURN, this.expression.or_push_null()).push($ -> new UnaryNode($.$0(), UnaryNode.RETURN));

    // if
    public rule elsif_block = lazy(() -> seq(ELSIF, bool, COLON, this.statement_sequence))
                                .push($ -> new ElseNode($.$0(), $.$1()));

    public rule else_block = lazy(() -> seq(ELSE, COLON, this.statement_sequence)).push($ -> new ElseNode(null, $.$0()));

    public rule elsif_sequence = seq(elsif_block.at_least(0), else_block.opt()).push(ActionContext::$list);

    public rule if_ = lazy(() -> seq(IF, bool, COLON, this.statement_sequence, elsif_sequence.or_push_null(), END)).push($ -> new IfNode($.$0(), $.$1(), $.$2()));

    // while
    public rule while_ = lazy(() -> seq(WHILE, bool, COLON, this.statement_sequence, END))
                            .push($ -> new WhileNode($.$0(), $.$1()));


    // for (EXTRA)
    public rule for_ = lazy(() -> seq(FOR, identifier, IN, choice(indexable, any_value), COLON, this.statement_sequence, END))
                            .push($ -> new ForNode($.$0(), $.$1(), $.$2()));


    // Function definition
    public rule param = identifier.push($ -> new ParameterNode($.$0()));

    public rule param_list = left_expression()
                                .operand(param)
                                .infix(COMMA).push(ActionContext::$list);

    public rule function_def = lazy(() -> seq(DEF, identifier, LPAREN, param_list.or_push_null(), RPAREN, COLON, this.statement_sequence, END))
                        .push($ -> new FunctionDefinitionNode($.$0(), $.$1(), $.$2()));


    // Regrouping statements
    public rule statement = choice(function_def, if_, while_, for_, return_, variable_assignment);

    public rule statement_sequence = choice(statement, line_comment, expression).at_least(0).push($ -> new BlockNode($.$list()));

    // root parser
    public rule root = statement_sequence.push($ -> new RootNode($.$0()));

    @Override
    public rule root() {
        return root;
    }

    public ParseResult parse (String input) {
        ParseResult result = Autumn.parse(root, input, ParseOptions.get());
        if (result.fullMatch) {
            System.out.println(result);
        } else {
            // debugging
            System.out.println(result.toString(new LineMapString("name", input), false));
            // for users
            System.out.println(result.userErrorString(new LineMapString("name", input)));
        }
        return result;
    }
}