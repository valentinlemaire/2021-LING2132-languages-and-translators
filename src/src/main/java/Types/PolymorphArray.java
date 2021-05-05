package Types;

import interpreter.Interpreter;
import interpreter.None;

import java.util.Arrays;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class PolymorphArray implements Iterable<Object> {
    private Object[] array;

    public PolymorphArray(Object... array) {
        this.array = Arrays.stream(array).toArray();
    }

    public void set(int index, Object value) {
        if (index >= array.length || index < 0) {
            throw new ArrayIndexOutOfBoundsException("Index "+index+" for array of size "+array.length);
        }
        array[index] = value;
    }

    public Object get(int index) {
        if (index >= array.length || index < 0) {
            throw new ArrayIndexOutOfBoundsException("Index "+index+" for array of size "+array.length);
        }
        return array[index];
    }

    public int size() {
        return array.length;
    }

    public PolymorphArray clone() {
        return new PolymorphArray(array.clone());
    }

    public void sort() {
        Arrays.sort(array);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolymorphArray that = (PolymorphArray) o;
        return Arrays.equals(array, that.array);
    }

    @Override
    public Iterator<Object> iterator() {
        return Arrays.stream(array).iterator();
    }

    public String toString() {
        return "["+Arrays.stream(array).map(Interpreter::recConvertToString).collect(Collectors.joining(", "))+"]";
    }
}
