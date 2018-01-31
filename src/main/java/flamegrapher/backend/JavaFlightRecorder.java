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
        
        // TODO
        // Define a template for processing the output and converting to string
        // The other commands basically call stuff without much output
        // Need to handle exceptions
        // Need to check errors from the output of the program to send back a msg
        // Need to read the JFR file in a non-blocking way
        // Integrate code of JFR to flame graph
        // Check if we could base the parsing logic on the the JFR coming with Java 9. Can we generate an executable?
        // Check http://hirt.se/blog/?p=920
        // Check https://steveperkins.com/using-java-9-modularization-to-ship-zero-dependency-native-apps/
        
        process.stdout()
               .handler(buf -> {
                   str.append(buf);
               }).endHandler(end -> {
                   Processes p = Processes.fromString(str.toString());
                   handler.complete(p);           
               });
    }
}
