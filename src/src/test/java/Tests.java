import org.apache.tools.ant.types.Environment;
import org.testng.annotations.Test;
import norswap.autumn.TestFixture;

public class Tests extends TestFixture {
    Parser parser = new Parser();


    @Test
    public void testSum() {
        this.rule = parser.root;
        success("1 + 1");
    }

}