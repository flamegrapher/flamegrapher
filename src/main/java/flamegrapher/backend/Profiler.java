package flamegrapher.backend;

import flamegrapher.model.Processes;
import io.vertx.core.Future;

public interface Profiler {
    
    void list(Future<Processes> handler);

}
