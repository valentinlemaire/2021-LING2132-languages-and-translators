package Types;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static interpreter.Interpreter.recConvertToString;
import static norswap.utils.Util.cast;

public class PolymorphMap {
    private HashMap<Long, Object> integerMap;
    private HashMap<String, Object> stringMap;
    private HashMap<Boolean, Object> boolMap;

    public PolymorphMap() {
        integerMap = new HashMap<>();
        stringMap  = new HashMap<>();
        boolMap    = new HashMap<>();
    }

    public void put(Object key, Object value) {
        if (key instanceof Long) {
            integerMap.put((Long) key, value);
        } else if (key instanceof String) {
            stringMap.put((String) key, value);
        } else if (key instanceof Boolean) {
            boolMap.put((Boolean) key, value);
        }
    }

    public Object get(Object key) {
        Object res = null;
        if (key instanceof Long) {
            res = integerMap.get((Long) key);
        } else if (key instanceof String) {
            res = stringMap.get((String) key);
        } else if (key instanceof Boolean) {
            res = boolMap.get((Boolean) key);
        }

        if (res == null)
            throw new RuntimeException("Invalid key : " + cast(key));
        else
            return res;
    }

    public Object[] keys() {
        Object[] keys = new Object[integerMap.size()+stringMap.size()+boolMap.size()];
        int i = 0;
        for (Long key : integerMap.keySet()) {
            keys[i] = key;
            i += 1;
        }
        for (String key : stringMap.keySet()) {
            keys[i] = key;
            i += 1;
        }
        for (Boolean key : boolMap.keySet()) {
            keys[i] = key;
            i += 1;
        }
        return keys;
    }

    public Object[][] entries() {
        Object[][] entries = new Object[integerMap.size()+stringMap.size()+boolMap.size()][2];
        int i = 0;
        for (Map.Entry<Long, Object> entry : integerMap.entrySet()) {
            entries[i][0] = entry.getKey();
            entries[i][1] = entry.getValue();
            i += 1;
        }
        for (Map.Entry<String, Object> entry : stringMap.entrySet()) {
            entries[i][0] = entry.getKey();
            entries[i][1] = entry.getValue();
            i += 1;
        }
        for (Map.Entry<Boolean, Object> entry : boolMap.entrySet()) {
            entries[i][0] = entry.getKey();
            entries[i][1] = entry.getValue();
            i += 1;
        }
        return entries;
    }

    public int size() {
        return integerMap.size() + stringMap.size() + boolMap.size();
    }

    public String toString() {
        return "{"+ Arrays.stream(this.entries()).map((e) -> recConvertToString(e[0]) + ": " + recConvertToString(e[1])).collect(Collectors.joining(", ")) + "}";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PolymorphMap that = (PolymorphMap) o;
        return Objects.equals(integerMap, that.integerMap) && Objects.equals(stringMap, that.stringMap) && Objects.equals(boolMap, that.boolMap);
    }
}
