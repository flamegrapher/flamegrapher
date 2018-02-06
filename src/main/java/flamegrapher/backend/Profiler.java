package flamegrapher.backend;

import flamegrapher.model.Processes;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public interface Profiler {
    
    void list(Future<Processes> handler);

    void start(String pid, Future<Void> handler);
    
    void status(String pid, Future<String> handler);
    
    void dump(String pid, String recorder, Future<JsonObject> handler);
    
    void stop(String pid, String recording, Future<Void> handler);
    
    void flames(Future<JsonArray> handler);
}
