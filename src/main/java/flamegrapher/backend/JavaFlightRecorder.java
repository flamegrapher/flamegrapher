package flamegrapher.backend;

import com.julienviet.childprocess.Process;

import flamegrapher.model.Processes;
import io.vertx.core.Future;
import io.vertx.core.Vertx;

public class JavaFlightRecorder implements Profiler {

    private final Vertx vertx;

    public JavaFlightRecorder(Vertx vertx) {
        this.vertx = vertx;
    }

    @Override
    public void list(Future<Processes> handler) {
        Process process = Process.spawn(vertx, "jcmd");
        StringBuilder str = new StringBuilder();
        
        process.stdout()
               .handler(buf -> {
                   str.append(buf);
               }).endHandler(end -> {
                   Processes p = Processes.fromString(str.toString());
                   handler.complete(p);           
               });
    }
}
