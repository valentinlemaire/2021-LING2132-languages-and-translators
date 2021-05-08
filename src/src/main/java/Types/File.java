package Types;

import interpreter.PassthroughException;
import norswap.utils.IO;

import java.io.*;

public class File {
    private BufferedReader reader;
    private BufferedWriter writer;

    public File(String filename, String mode) throws FileNotFoundException, IOException {
        if (mode.equals("r") || mode.equals("read")) {
            reader = new BufferedReader(new FileReader(filename));
        } else if (mode.equals("w") || mode.equals("write")) {
            writer = new BufferedWriter(new FileWriter(filename));
        } else {
            throw new IllegalArgumentException("Argument mode has to be \"r\"/\"read\" for reading or \"w\"/\"write\" for writing, not " + mode);
        }
    }

    public void close() throws IOException {
        if (reader != null)
            reader.close();
        if (writer != null)
            writer.close();
    }

    public String read() throws IOException {
        if (reader != null)
            return reader.readLine();
        else
            throw new IOException("Calling read on file in write mode");
    }

    public void write(Object o) throws IOException {
        String s = o.toString();
        if (writer != null)
            writer.write(s);
        else
            throw new IOException("Calling write on file in read mode");
    }
}
