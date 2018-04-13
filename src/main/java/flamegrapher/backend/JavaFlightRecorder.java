package flamegrapher.backend;

import static com.julienviet.childprocess.Process.spawn;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.julienviet.childprocess.Process;
import com.oracle.jmc.flightrecorder.CouldNotLoadRecordingException;
import com.oracle.jmc.flightrecorder.jdk.JdkTypeIDs;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import flamegrapher.backend.JsonOutputWriter.StackFrame;
import flamegrapher.model.Item;
import flamegrapher.model.Processes;
import io.vertx.core.CompositeFuture;
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
    @SuppressWarnings("rawtypes")
    public void list(Future<JsonArray> handler) {
        Future<Processes> processesFuture = Future.future();
        // Obtain the list of running Java processes
        jcmd(Collections.emptyList(), processesFuture, Processes::fromString);

        // For each process, obtain it's current status
        processesFuture.compose(processes -> {
            List<Future> futures = new ArrayList<>();
            for (Item item : processes.items()
                                      .values()) {
                String pid = item.getPid();
                // Will retrieve the status, i.e. recording or not recording, in
                // parallel for all processes.
                if (!item.getName()
                         .contains("jcmd")) {
                    Future<Item> itemFuture = Future.future();
                    futures.add(itemFuture);
                    status(pid, itemFuture);
                }
            }
            // Once all are done, process results.
            CompositeFuture.all(futures)
                           .setHandler(h -> {
                               if (h.succeeded()) {
                                   JsonArray results = new JsonArray();
                                   for (Future f : futures) {
                                       Item status = (Item) f.result();
                                       Item item = processes.items()
                                                            .get(status.getPid());
                                       item.setState(status.getState());
                                       item.setRecordingNumber(status.getRecordingNumber());
                                       JsonObject json = JsonObject.mapFrom(item);
                                       results.add(json);
                                   }
                                   handler.complete(results);
                               } else {
                                   handler.fail(h.cause());
                               }
                           });
        }, handler);
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
    public void status(String pid, Future<Item> handler) {
        jcmd(asList(pid, "JFR.check"), handler, Item::fromStatus);
    }

    private static String bypass(String s) {
        return s;
    }

    @Override
    public void dump(String pid, String recording, Future<JsonObject> handler) {
        // jcmd 8683 JFR.dump filename=./terst.jfr recording=1
        // jcmd 8683 JFR.dump
        // filename=/Users/lgomes/gitclones/flamegrapher/test.jfr recording=1
        String filename = filename(pid, recording);
        jcmd(asList(pid, "JFR.dump", "filename=" + filename, "recording=" + recording), handler, s -> {
            JsonObject json = new JsonObject();
            json.put("path", filename);
            return json;
        });
    }

    private String filename(String pid, String recording) {
        String filename = config.getString("flamegrapher.jfr-dump-path") + pid + "." + recording + ".jfr";
        return filename;
    }

    @Override
    public void stop(String pid, String recording, Future<Void> handler) {
        jcmd(asList(pid, "JFR.stop", "recording=" + recording), handler);
    }

    @Override
    public void flames(String pid, String recording, Future<StackFrame> handler) {
        String filename = filename(pid, recording);
        JfrParser parser = new JfrParser();
        vertx.<StackFrame>executeBlocking(future -> {
            try {
                // TODO Allow different types of events
                StackFrame json = parser.toJson(new File(filename), JdkTypeIDs.EXECUTION_SAMPLE);
                future.complete(json);
            } catch (IOException | CouldNotLoadRecordingException e) {
                handler.fail(e);
            }
        }, result -> {

            if (result.succeeded()) {
                handler.complete(result.result());
            } else {
                handler.fail(result.cause());
            }
        });
    }

    private <T> void jcmd(List<String> args, Future<T> handler) {
        jcmd(args, handler, null);
    }

    private <T> void jcmd(List<String> args, Future<T> handler, Function<String, T> transformer) {
        Process process = spawn(vertx, "jcmd", args);
        Future<String> processCompleteHandler = completeOnExit(handler, transformer);
        StringBuilder str = new StringBuilder();
        StringBuilder err = new StringBuilder();
        process.stdout()
               .handler(str::append);
        process.stderr()
               .handler(err::append);
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
