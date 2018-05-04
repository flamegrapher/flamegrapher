package flamegrapher.backend;

import flamegrapher.backend.JsonOutputWriter.StackFrame;
import flamegrapher.model.Item;
import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;

public interface Profiler {
    
    void list(Future<JsonArray> handler);

    void start(String pid, Future<Item> handler);
    
    void status(String pid, Future<Item> handler);
    
    void dump(String pid, String recorder, Future<JsonObject> handler);
    
    void stop(String pid, String recording, Future<Item> handler);
    
    void flames(String pid, String recording, Future<StackFrame> handler);

    void save(String pid, String recording, Future<JsonObject> handler);
    
    void listDumps(Future<JsonArray> handler);

    void listSavedDumps(Future<JsonArray> handler);

    void listSavedFlames(Future<JsonArray> handler);

    void flameFromStorage(String storageKey, Future<StackFrame> handler);

    void saveFlame(String pid, String recording, Future<JsonObject> handler);

    Future<Void> dumpFromLocal(String filename, RoutingContext rc);
}
