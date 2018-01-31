package flamegrapher.model;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Processes {
    
    @JsonProperty("processes")
    Map<String, String> pidName;
    
    public Processes() {
        pidName = new HashMap<String,String>();
    }
    
    public Map<String, String> getPidName() {
        return pidName;
    }

    public static Processes fromString(String string) {
        Processes p = new Processes();
        String[] lines = string.split("\n");
        for(String line : lines) {
            String[] parts = line.split(" ", 2);
            p.pidName.put(parts[0], parts[1]);
        }
        return p;
    }
}


