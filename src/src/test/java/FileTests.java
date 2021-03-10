import norswap.autumn.Autumn;
import norswap.autumn.Parse;
import norswap.autumn.ParseOptions;
import norswap.autumn.ParseResult;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTests {
    public static void main(String[] args) {

        String[] files = new String[] {"assets/fibonacci.ns", "assets/fizzbuzz.ns", "assets/integers.ns", "assets/primes.ns", "assets/strings.ns"};

        for (String path : files) {
            try {
                Path file = Path.of(path);

                String content = Files.readString(file);

                Parser parser = new Parser();
                ParseOptions options = ParseOptions.wellFormednessCheck(true).trace(true).recordCallStack(true).get();
                ParseResult res = Autumn.parse(parser, content, options);

                System.out.println(path+" : "+res.toString());

            } catch (IOException e) {
                System.err.println("Error reading file.");
            }
        }
    }
}
