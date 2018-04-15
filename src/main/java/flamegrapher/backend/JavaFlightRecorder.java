
package flamegrapher.backend;

import static com.julienviet.childprocess.Process.spawn;
import static java.util.Arrays.asList;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;

import com.hubrick.vertx.s3.client.S3Client;
import com.hubrick.vertx.s3.client.S3ClientOptions;
import com.hubrick.vertx.s3.model.request.AdaptiveUploadRequest;
import com.julienviet.childprocess.Process;
import com.oracle.jmc.flightrecorder.CouldNotLoadRecordingException;
import com.oracle.jmc.flightrecorder.jdk.JdkTypeIDs;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import flamegrapher.backend.JsonOutputWriter.StackFrame;
import flamegrapher.model.Processes;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.file.OpenOptions;
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
    final S3ClientOptions clientOptions;
    final S3Client s3Client;
    final String recordingOption = "name";

    public JavaFlightRecorder(Vertx vertx) {
        this.vertx = vertx;
        this.config = ConfigFactory.load();
        clientOptions = new S3ClientOptions()
            .setHostnameOverride(config.getString("flamegrapher.s3-server"))
            .setAwsRegion("us-east-1")
            .setAwsServiceName("s3")
            .setAwsAccessKey(config.getString("flamegrapher.s3-access-key"))
            .setAwsSecretKey(config.getString("flamegrapher.s3-secret-key"));
        s3Client = new S3Client(vertx, clientOptions);
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
        String filename = filename(pid, recording);
        jcmd(asList(pid, "JFR.dump", "filename=" + filename, recordingOption + "=" + recording),
        handler, 
        s -> {
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
        jcmd(asList(pid, "JFR.stop", recordingOption + "=" + recording), handler);
    }

    public void save(String pid, String recording, Future<Void> handler) {
        String filename = filename(pid, recording);
        vertx.fileSystem().open(filename, new OpenOptions().setRead(true), asyncFile -> {
            if (asyncFile.succeeded()) {
                s3Client.adaptiveUpload(
                    config.getString("flamegrapher.s3-bucket"),
                    filename,
                    new AdaptiveUploadRequest(asyncFile.result()).withContentType("application/jfr-dump"),
                    response -> {
                        System.out.println("Response from AWS: " + response.getHeader().getContentType());
                        handler.complete();
                        vertx.fileSystem().delete(filename, asyncDelete -> {
                            System.out.println("Deleted: " + filename);
                        });
                    },
                    e -> handler.fail(e)
                );
            } else {
                handler.fail(asyncFile.cause());
            }
        });
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
