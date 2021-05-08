import ast.ASTNode;
import interpreter.Interpreter;
import norswap.autumn.Autumn;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;
import norswap.autumn.positions.LineMap;
import norswap.autumn.positions.LineMapString;
import norswap.uranium.Reactor;
import norswap.utils.visitors.Walker;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static java.lang.System.exit;

public class NS {
    public static void main(String[] args) {
        String filepath = args[0];
        String[] nsargs = Arrays.stream(args).skip(1).toArray(String[]::new);
        try {
            Path file = Path.of(filepath);

            String content = Files.readString(file);

            NSParser grammar = new NSParser();
            ParseOptions options = ParseOptions.builder().get();
            ParseResult result = Autumn.parse(grammar.root, content, options);
            LineMap lineMap = new LineMapString(filepath, content);

            if (!result.fullMatch) {
                System.out.println(result.toString(lineMap, false));
                exit(1);
            }

            ASTNode tree = (ASTNode) result.topValue();
            Reactor reactor = new Reactor();
            Walker<ASTNode> walker = SemanticAnalysis.createWalker(reactor);
            walker.walk(tree);
            reactor.run();

            if (!reactor.errors().isEmpty()) {
                System.out.println(reactor.reportErrors(Object::toString));
                return;
            }

            Interpreter interpreter = new Interpreter(reactor, nsargs);
            Object res = interpreter.interpret(tree);
        } catch (IOException e) {
            System.err.println("Cannot find file "+filepath);
        }
    }
}
