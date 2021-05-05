package Types;

import java.util.HashMap;

import static norswap.utils.Util.cast;

public class PolymorphMap {
    private HashMap<Integer, Object> integerMap;
    private HashMap<String, Object> stringMap;
    private HashMap<Boolean, Object> boolMap;

    public PolymorphMap() {
        integerMap = new HashMap<>();
        stringMap  = new HashMap<>();
        boolMap    = new HashMap<>();
    }

    public void put(Object key, Object value) {
        if (key instanceof Integer) {
            integerMap.put((Integer) key, value);
        } else if (key instanceof String) {
            stringMap.put((String) key, value);
        } else if (key instanceof Boolean) {
            boolMap.put((Boolean) key, value);
        }
    }

    public Object get(Object key) {
        Object res = null;
        if (key instanceof Integer) {
            res = integerMap.get((Integer) key);
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
}
