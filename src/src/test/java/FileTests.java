import ast.ASTNode;
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

public class FileTests {
    public static void main(String[] args) {

        String[] files = new String[] {"fibonacci.ns", "fizzbuzz.ns", "integers.ns", "primes.ns", "strings.ns"};

        for (int i = 0; i < files.length; i++) {
            try {
                Path file = Path.of("src/assets/"+files[i]);

                String content = Files.readString(file);

                NSParser grammar = new NSParser();
                ParseOptions options = ParseOptions.builder().recordCallStack(true).get();
                ParseResult result = Autumn.parse(grammar.root, content, options);
                LineMap lineMap = new LineMapString(files[i], content);
                System.out.println(result.toString(lineMap, false));

                if (!result.fullMatch)
                    continue;

                ASTNode tree = (ASTNode) result.topValue();
                Reactor reactor = new Reactor();
                Walker<ASTNode> walker = SemanticAnalysis.createWalker(reactor);
                walker.walk(tree);
                reactor.run();

                if (!reactor.errors().isEmpty()) {
                    System.out.println(reactor.reportErrors(it ->
                            it.toString()));
                    return;
                }

            } catch (IOException e) {
                System.err.println("Error reading file.");
            }
        }
    }
}
