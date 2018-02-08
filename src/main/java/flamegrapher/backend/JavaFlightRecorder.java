package flamegrapher.backend;

import static com.julienviet.childprocess.Process.spawn;
import static java.util.Arrays.asList;

import java.util.function.Function;

import com.julienviet.childprocess.Process;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import flamegrapher.model.Processes;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JavaFlightRecorder implements Profiler {

    // TODO
    // Define a template for processing the output and converting to string
    // The other commands basically call stuff without much output
    // Need to handle exceptions
    // Need to check errors from the output of the program to send back a
    // msg
    // Need to read the JFR file in a non-blocking way
    // Integrate code of JFR to flame graph
    // Check if we could base the parsing logic on the the JFR coming with
    // Java 9. Can we generate an executable?
    // Check http://hirt.se/blog/?p=920
    // Check
    // https://steveperkins.com/using-java-9-modularization-to-ship-zero-dependency-native-apps/

    // Try to make something that will push back the response to the UI when
    // it's ready
    // e.g. profile for 60s this PID (fire and forget)
    // Result is pushed back when ready

    // Add config

    private final Config config;
    private final Vertx vertx;

    public JavaFlightRecorder(Vertx vertx) {
        this.vertx = vertx;
        this.config = ConfigFactory.load();
    }

    @Override
    public void list(Future<Processes> handler) {
        Process process = Process.spawn(vertx, "jcmd");
        output(process, Processes::fromString, handler);
    }

    @Override
    public void start(String pid, Future<Void> handler) {
        // First run jcmd 8683 VM.unlock_commercial_features
        // Then run jcmd 8683 JFR.start
        Process unlock = Process.spawn(vertx, "jcmd", asList(pid, "VM.unlock_commercial_features"));
        unlock.exitHandler(status -> {
            // TODO check status value
            // Assuming it's OK now
            Process.spawn(vertx, "jcmd", asList(pid, "JFR.start"))
                   .exitHandler(startStatus -> {
                       System.out.println("Exit status of JFR start: " + startStatus.intValue());
                       handler.complete();
                   });
        });
    }

    @Override
    public void status(String pid, Future<String> handler) {
        Process check = spawn(vertx, "jcmd", asList(pid, "JFR.check"));
        output(check, JavaFlightRecorder::bypass, handler);
        // TODO: Parse the recording

        // lgomes$ jcmd 8683 JFR.check
        // 8683:
        // Recording: recording=1 name="Recording 1" (running)
    }

    private static String bypass(String s) {
        return s;
    }

    @Override
    public void dump(String pid, String recording, Future<JsonObject> handler) {
        // jcmd 8683 JFR.dump filename=./terst.jfr recording=1
        // jcmd 8683 JFR.dump
        // filename=/Users/lgomes/gitclones/flamegrapher/test.jfr recording=1
        String filename = config.getString("flamegrapher.jfr-dump-path") + pid + "." + recording + ".jfr";
        spawn(vertx, "jcmd", asList(pid, "JFR.dump", "filename=" + filename, "recording=" + recording)).exitHandler(
                status -> {
                    // TODO check status code!
                    JsonObject json = new JsonObject();
                    json.put("path", filename);
                    handler.complete(json);
                });
    }

    @Override
    public void stop(String pid, String recording, Future<Void> handler) {
        spawn(vertx, "jcmd", asList(pid, "JFR.stop", "recording=" + recording)).exitHandler(status -> {
            // TODO Handle errors
            handler.complete();
        });
    }

    @Override
    public void flames(Future<JsonArray> handler) {
        // TODO Auto-generated method stub
    }

    private <T> void output(Process process, Function<String, T> extract, Future<T> handler) {
        StringBuilder str = new StringBuilder();
        process.stdout()
               .handler(buf -> {
                   str.append(buf);
               })
               .endHandler(end -> {
                   T object = extract.apply(str.toString());
                   handler.complete(object);
               });
    }
}
