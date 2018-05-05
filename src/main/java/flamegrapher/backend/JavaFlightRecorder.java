
package flamegrapher.backend;

import static com.julienviet.childprocess.Process.spawn;
import static io.vertx.core.CompositeFuture.all;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.hubrick.vertx.s3.client.S3Client;
import com.hubrick.vertx.s3.client.S3ClientOptions;
import com.hubrick.vertx.s3.model.request.AdaptiveUploadRequest;
import com.hubrick.vertx.s3.model.request.GetBucketRequest;
import com.hubrick.vertx.s3.model.request.GetObjectRequest;
import com.hubrick.vertx.s3.model.request.PutObjectRequest;
import com.julienviet.childprocess.Process;
import com.oracle.jmc.flightrecorder.CouldNotLoadRecordingException;
import com.oracle.jmc.flightrecorder.jdk.JdkTypeIDs;

import flamegrapher.backend.JsonOutputWriter.StackFrame;
import flamegrapher.model.Item;
import flamegrapher.model.JVM;
import flamegrapher.model.JVMType;
import flamegrapher.model.Processes;
import flamegrapher.model.State;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.RoutingContext;

public class JavaFlightRecorder implements Profiler {
    private static final Logger logger = LoggerFactory.getLogger(JavaFlightRecorder.class);
    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";
    /**
     * This JDK event type is not yet available on JdkTypeIDs class, but is used
     * starting with JFK 9. Method profiling is now split into
     * <code>com.oracle.jdk.ExecutionSample</code> and
     * <code>com.oracle.jdk.NativeMethodSample</code>
     */
    public static final String NATIVE_METHOD_SAMPLE = "com.oracle.jdk.NativeMethodSample";

    private final JsonObject config;
    private final Vertx vertx;
    private final S3Client s3Client;
    private final S3ClientOptions s3ClientOptions;
    String recordingOption = "name";
    private final String dumpsBucket;
    private final String flamesBucket;

    public JavaFlightRecorder(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        this.config = config;
        s3ClientOptions = new S3ClientOptions().setHostnameOverride(config.getString("FLAMEGRAPHER_S3_SERVER"))
                                               .setAwsRegion("us-east-1")
                                               .setAwsServiceName("s3")
                                               .setConnectTimeout(30000)
                                               .setGlobalTimeoutMs(30000L)
                                               .setDefaultPort(config.getInteger("FLAMEGRAPHER_S3_PORT", 80))
                                               .setAwsAccessKey(config.getString("FLAMEGRAPHER_S3_ACCESS_KEY"))
                                               .setAwsSecretKey(config.getString("FLAMEGRAPHER_S3_SECRET_KEY"));

        s3Client = new S3Client(vertx, s3ClientOptions);
        dumpsBucket = config.getString("FLAMEGRAPHER_S3_DUMPS_BUCKET", "dumps");
        flamesBucket = config.getString("FLAMEGRAPHER_S3_FLAMES_BUCKET", "flames");
        try {
            Files.createDirectories(Paths.get(workingDir()));
        } catch (IOException e) {
            logger.error("Unable to create work directory " + workingDir(), e);
            vertx.close();
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public void list(Future<JsonArray> handler) {
        Future<Processes> processesFuture = Future.future();
        // Obtain the list of running Java processes
        jcmd(Collections.emptyList(), processesFuture, Processes::fromString);

        // For each process, check if it's a HotSpot JDK (JFR is only available
        // for Hotspot for now).
        List<Future> jvms = new ArrayList<>();

        processesFuture.compose(processes -> {

            for (Item item : processes.items()
                                      .values()) {
                String pid = item.getPid();

                if (notJcmdItself(item)) {
                    Future<JVM> jvmFuture = Future.future();
                    jvms.add(jvmFuture);
                    // Check VM version and type
                    jcmd(asList(pid, "VM.version"), jvmFuture, JVM::fromVMVersion);
                }
            }

            // Once all are done, process results.
            List<Future> jfrChecks = new ArrayList<>();

            all(jvms).setHandler(h -> {
                if (h.succeeded()) {
                    for (Future f : jvms) {
                        JVM jvm = (JVM) f.result();
                        // Only HotSpot ships with JFR for now
                        if (JVMType.HOTSPOT.equals(jvm.getType())) {
                            Future<Item> statusCheck = Future.future();
                            jfrChecks.add(statusCheck);
                            // Will retrieve the status, i.e. recording or not
                            // recording, in parallel for all processes.
                            status(Integer.toString(jvm.getPid()), statusCheck);
                        }
                    }
                    statusResults(handler, processes, jfrChecks);
                } else {
                    handler.fail(h.cause());
                }
            });
        }, handler);
    }

    @SuppressWarnings("rawtypes")
    private void statusResults(Future<JsonArray> handler, Processes processes, List<Future> jfrChecks) {
        // Retrieve status results and assemble complete response
        all(jfrChecks).setHandler(c -> {
            if (c.succeeded()) {
                JsonArray results = new JsonArray();
                for (Future f : jfrChecks) {
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
                handler.fail(c.cause());
            }
        });
    }

    private boolean notJcmdItself(Item item) {
        return !item.getName()
                    .contains("jcmd");
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
        if (jvm.getMajorVersion() > 8) {
            args = asList(pid, "JFR.dump", "filename=" + filename, "name=" + recording);
        } else {
            args = asList(pid, "JFR.dump", "filename=" + filename, "recording=" + recording);
        }
        return args;
    }

    private String filename(String pid, String recording) {
        String filename = workingDir() + pid + "." + recording + ".jfr";
        return filename;
    }

    private String workingDir() {
        return config.getString("FLAMEGRAPHER_JFR_DUMP_PATH", "/tmp/flamegrapher/");
    }

    @Override
    public void save(String pid, String recording, Future<JsonObject> handler) {
        String filename = filename(pid, recording);
        vertx.fileSystem()
             .open(filename, new OpenOptions().setRead(true), asyncFile -> {
                 if (asyncFile.succeeded()) {
                     String key = Paths.get(filename)
                                       .getFileName()
                                       .toString();
                     s3Client.adaptiveUpload(dumpsBucket, key,
                             new AdaptiveUploadRequest(asyncFile.result()).withContentType("application/jfr-dump"),
                             response -> {
                                 JsonObject json = new JsonObject();
                                 json.put("key", key);
                                 json.put("url", s3Client.getHostname() + "/" + dumpsBucket + "/" + key);
                                 handler.complete(json);
                             }, e -> handler.fail(e));
                 } else {
                     handler.fail(asyncFile.cause());
                 }
             });
    }

    private String s3Server() {
        return "http://" + s3Client.getHostname() + ":" + s3ClientOptions.getDefaultPort();
    }

    @Override
    public void saveFlame(String pid, String recording, Future<JsonObject> handler) {
        String filename = filename(pid, recording);
        Future<StackFrame> f = Future.future();
        f.setHandler(r -> {
            if (r.succeeded()) {
                Buffer buf = Buffer.buffer(Json.encode(r.result()));
                String key = Paths.get(filename)
                                  .getFileName()
                                  .toString();
                s3Client.putObject(flamesBucket, key,
                        new PutObjectRequest(buf).withContentType(APPLICATION_JSON_CHARSET_UTF_8), response -> {
                            logger.info(response.getHeader());
                            JsonObject json = new JsonObject();
                            json.put("key", key);
                            json.put("url", s3Server() + "/" + flamesBucket + "/" + key);
                            handler.complete(json);
                        }, e -> handler.fail(e));
            } else {
                handler.fail(r.cause());
            }
        });
        generateFlame(filename, f);
    }

    @Override
    public void listDumps(Future<JsonArray> handler) {
        String dirName = workingDir();
        vertx.fileSystem()
             .readDir(dirName, ".+\\.jfr", dirStream -> {
                 if (dirStream.failed()) {
                     handler.fail(dirStream.cause());
                 } else {
                     JsonArray results = new JsonArray();
                     dirStream.result()
                              .stream()
                              .map(fullPath -> Paths.get(fullPath)
                                                    .getFileName())
                              .forEach(file -> {
                                  String[] components = file.getFileName()
                                                            .toString()
                                                            .split("\\.");
                                  JsonObject json = new JsonObject();
                                  json.put("pid", components[0]);
                                  json.put("recording", components[1]);
                                  results.add(json);
                              });
                     handler.complete(results);
                 }
             });
    }

    @Override
    public Future<Void> dumpFromLocal(String filename, RoutingContext rc) {
        Future<Void> status = Future.future();
        // File name example: 164722.1.jfr
        String path = workingDir() + "/" + filename;
        rc.response()
          .sendFile(path, status);
        return status;
    }

    @Override
    public void listSavedDumps(Future<JsonArray> handler) {
        s3Client.getBucket(dumpsBucket, new GetBucketRequest(), response -> {
            JsonArray result = new JsonArray();
            response.getData()
                    .getContentsList()
                    .stream()
                    .forEach(contents -> {
                        JsonObject json = new JsonObject();
                        json.put("key", contents.getKey());
                        json.put("url", s3Server() + "/" + dumpsBucket + "/" + contents.getKey());
                        result.add(json);
                    });
            handler.complete(result);
        }, e -> handler.fail(e));
    }

    @Override
    public void listSavedFlames(Future<JsonArray> handler) {
        s3Client.getBucket(flamesBucket, new GetBucketRequest(), response -> {
            JsonArray result = new JsonArray();
            response.getData()
                    .getContentsList()
                    .stream()
                    .forEach(contents -> {
                        JsonObject json = new JsonObject();
                        json.put("key", contents.getKey());
                        json.put("url", s3Server() + "/" + flamesBucket + "/" + contents.getKey());
                        result.add(json);
                    });
            handler.complete(result);
        }, e -> handler.fail(e));
    }

    @Override
    public void flameFromStorage(String storageKey, Future<StackFrame> handler) {
        s3Client.getObject(flamesBucket, storageKey, new GetObjectRequest(), storageResult -> {

            String filename = filename("storage", storageKey);
            generateFlame(filename, handler);
        }, e -> handler.fail(e));
    }

    private void generateFlame(String filename, Future<StackFrame> handler) {
        JfrParser parser = new JfrParser();
        vertx.<StackFrame>executeBlocking(future -> {
            try {
                logger.info("Processing file: " + filename);
                StackFrame json = parser.toJson(new File(filename), JdkTypeIDs.EXECUTION_SAMPLE, NATIVE_METHOD_SAMPLE);
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

    @Override
    public void stop(String pid, String recording, Future<Item> handler) {

        // First we need to check the JVM version
        Future<JVM> jvmFuture = Future.future();
        jcmd(asList(pid, "VM.version"), jvmFuture, JVM::fromVMVersion);

        // Once we got the JVM version, we can execute the stop
        jvmFuture.compose(jvm -> {
            String recordingParameter = (jvm.getMajorVersion() > 8 ? "name=" : "recording=");

            // Don't parse the output. If there's an error the jcmd method will
            // fail the future.
            // If we tried to stop a recording that was not running, we will
            // simply ignore it for now.
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
        generateFlame(filename, handler);
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
                logger.info(str);
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