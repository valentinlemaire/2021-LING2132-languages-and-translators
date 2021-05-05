package interpreter;

public class None {
    public static final None INSTANCE = new None();
    private None() {}

    public String toString() {
        return "None";
    }
}

