package flamegrapher.backend;

import static com.julienviet.childprocess.Process.spawn;
import static java.util.Arrays.asList;

import java.util.Collections;
import java.util.List;
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
        jcmd(Collections.emptyList(), handler, Processes::fromString);
    }

    @Override
    public void start(String pid, Future<Void> handler) {
        // First run jcmd 8683 VM.unlock_commercial_features
        // Then run jcmd 8683 JFR.start
        Future<Void> firstHandler = Future.future();
        firstHandler.setHandler(s -> {
            if (s.succeeded()) {
                jcmd(asList(pid, "JFR.start"), handler);                
            } else {
                handler.fail(s.cause());
            }
        });
        jcmd(asList(pid, "VM.unlock_commercial_features"), firstHandler);
    }
    
    @Override
    public void status(String pid, Future<String> handler) {
        jcmd(asList(pid, "JFR.check"), handler, JavaFlightRecorder::bypass);
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
        jcmd(asList(pid, "JFR.dump", "filename=" + filename, "recording=" + recording),
            handler, 
            s -> {
                JsonObject json = new JsonObject();
                json.put("path", filename);
                return json;
            });
    }

    @Override
    public void stop(String pid, String recording, Future<Void> handler) {
        jcmd(asList(pid, "JFR.stop", "recording=" + recording), handler);
    }

    @Override
    public void flames(Future<JsonArray> handler) {
        // TODO Auto-generated method stub
    }

    private <T> void jcmd(List<String> args, Future<T> handler) {
        jcmd(args, handler, null);
    }
    
    private <T> void jcmd(List<String> args, Future<T> handler, Function<String, T> transformer) {
        Process process = spawn(vertx, "jcmd", args);
        Future<String> processCompleteHandler = completeOnExit(handler, transformer);
        StringBuilder str = new StringBuilder();
        StringBuilder err = new StringBuilder();
        process.stdout().handler(str::append);
        process.stderr().handler(err::append);
        process.exitHandler(status -> { 
            if (status.intValue() != 0) {
                processCompleteHandler.fail(new ProfilerError("jcmd status code was " + status + ", stderr=" + err));
            } else {
                processCompleteHandler.complete(str.toString());
            }
        });
    }

    private <T> Future<String> completeOnExit(Future<T> future, Function<String, T> transformer) {
        Future<String> f = Future.future();
        f.compose(s -> {
            if (transformer != null) {
                future.complete(transformer.apply(s));
            } else {
                future.complete();
            }
        }, future);
        return f;
    }

}
