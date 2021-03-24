import norswap.autumn.ParseResult;

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

                NSParser parser = new NSParser();
                System.out.println("Test "+(i+1)+"/"+files.length+" : "+files[i]);
                ParseResult res = parser.parse(content);

            } catch (IOException e) {
                System.err.println("Error reading file.");
            }
        }
    }
}
