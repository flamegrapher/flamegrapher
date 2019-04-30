package flamegrapher.model;

import static java.lang.System.lineSeparator;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class Processes {

    Map<String, Item> items;

    public Processes() {
        items = new HashMap<String,Item>();
    }

    public Map<String, Item> items() {
        return items;
    }

    @Override
    public String toString() {
        String mapAsString = items.keySet().stream()
            .map(key -> key + "=" + items.get(key))
            .collect(Collectors.joining(", ", "{", "}"));
        return mapAsString;
    }

    public static Processes fromString(String string) {
        Processes p = new Processes();
        String[] lines = string.split(lineSeparator());
        for(String line : lines) {
            String[] parts = line.split(" ", 2);
            String pid = parts[0];
            String name = parts[1];
            Item item = new Item(pid, name);
            p.items.put(pid, item);
        }
        return p;
    }
}
