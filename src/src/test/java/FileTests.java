public class FileTests {
    public static void main(String[] args) {

        String[][] commands = new String[][]{
                new String[]{"src/assets/fibonacci.ns", "50"},
                new String[]{"src/assets/fizzbuzz.ns"},
                new String[]{"src/assets/prime.ns", "50"},
                new String[]{"src/assets/sort.ns", "34", "56", "341", "1", "-34", "12", "44", "95", "103", "11", "12"},
                new String[]{"src/assets/uniq.ns", "hello", "bonjour", "ciao", "hello", "ciao", "coucou", "yo", "yo", "yo", "ciao"},
                new String[]{"src/assets/extensions.ns", "10"}
        };


        for (int i = 0; i < commands.length; i++) {
            System.out.println("------------- TEST "+(i+1)+" -------------");
            System.out.println("  "+commands[i][0].replace("src/assets/", ""));
            System.out.println("----------------------------------");

            NS.main(commands[i]);
            System.out.println("");
        }
    }
}
