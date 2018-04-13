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
import flamegrapher.model.JVM;
import flamegrapher.model.Processes;
import flamegrapher.model.State;
import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class JavaFlightRecorder implements Profiler {

    // TODO
    // * Add logging
    // * Handle errors in the UI (HTTP 500)
    // * Validate with Java 9
    // * Other event types
    // * Add hook to update the listed processes every 30s automatically (or add button to do it manually?)
    // * Fallback to standard vertx config
    // * Run VM.version on each process to filter out OpenJDK and old JVMs while running list 
    // $ jcmd 49371 VM.version
    // 49371:
    // OpenJDK 64-Bit Server VM version 25.152-b12
    // JDK 8.0_152

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
    public void start(String pid, Future<Item> handler) {
        // First run jcmd 8683 VM.unlock_commercial_features
        // Then run jcmd 8683 JFR.start
        Future<Void> firstHandler = Future.future();
        jcmd(asList(pid, "VM.unlock_commercial_features"), firstHandler);
        firstHandler.setHandler(s -> {
            if (s.succeeded()) {
                jcmd(asList(pid, "JFR.start"), handler, Item::fromStart);
            } else {
                handler.fail(s.cause());
            }
        });

    }

    @Override
    public void status(String pid, Future<Item> handler) {
        jcmd(asList(pid, "JFR.check"), handler, Item::fromStatus);
    }

    @Override
    public void dump(String pid, String recording, Future<JsonObject> handler) {

        String filename = filename(pid, recording);
        // First we need to check the JVM version
        Future<JVM> jvmFuture = Future.future();
        jcmd(asList(pid, "VM.version"), jvmFuture, JVM::fromVMVersion);

        // Once we got the JVM version, we can execute the dump
        jvmFuture.compose(jvm -> {
            List<String> args = new ArrayList<>();
            args = argumentsForDump(pid, recording, filename, jvm);
            // Execute the dump
            jcmd(args, handler, s -> {
                JsonObject json = new JsonObject();
                json.put("path", filename);
                return json;
            });
            
        }, handler);
    }

    private List<String> argumentsForDump(String pid, String recording, String filename, JVM jvm) {
        List<String> args;
        // Command changed starting with JDK 9
        if (jvm.getMajorVersion() == 9) {
            args = asList(pid, "JFR.dump", "filename=" + filename, "name=" + recording);
        } else {
            args = asList(pid, "JFR.dump", "filename=" + filename, "recording=" + recording);
        }
        return args;
    }

    private String filename(String pid, String recording) {
        String filename = config.getString("flamegrapher.jfr-dump-path") + pid + "." + recording + ".jfr";
        return filename;
    }

    @Override
    public void stop(String pid, String recording, Future<Item> handler) {
        
        // First we need to check the JVM version
        Future<JVM> jvmFuture = Future.future();
        jcmd(asList(pid, "VM.version"), jvmFuture, JVM::fromVMVersion);

        // Once we got the JVM version, we can execute the stop
        jvmFuture.compose(jvm -> {
            String recordingParameter = (jvm.getMajorVersion() == 9 ? "name=" : "recording=");
            
            // Don't parse the output. If there's an error the jcmd method will fail
            // the future.
            // If we tried to stop a recording that was not running, we will simply
            // ignore it for now.
            jcmd(asList(pid, "JFR.stop", recordingParameter + recording), handler, s -> {
                Item i = new Item(pid);
                i.setState(State.NOT_RECORDING);
                i.setRecordingNumber(recording);
                return i;
            });
        }, handler);
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

    private <T> void jcmd(List<String> args, Future<Void> handler) {
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