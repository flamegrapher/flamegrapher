package flamegrapher.model;

import java.util.HashMap;
import java.util.Map;

public class Processes {
    
    //@JsonProperty("processes")
    Map<String, Item> items;
    
    public Processes() {
        items = new HashMap<String,Item>();
    }
    
    public Map<String, Item> items() {
        return items;
    }

    public static Processes fromString(String string) {
        Processes p = new Processes();
        String[] lines = string.split("\n");
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
