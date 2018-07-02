package flamegrapher;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import flamegrapher.backend.JavaFlightRecorder;
import io.vertx.config.ConfigRetriever;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.CompositeFuture;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {
    private static final Logger logger = LoggerFactory.getLogger(MainVerticle.class);

    private static final String APPLICATION_JSON_CHARSET_UTF_8 = "application/json; charset=utf-8";

    Integer serverPort;

    private String serverAddress;

    @Override
    public void start(Future<Void> fut) {
        JavaFlightRecorder jfr = new JavaFlightRecorder(vertx, config());
        Router router = Router.router(vertx);
        router.route()
              .handler(BodyHandler.create());

        router.route("/")
              .handler(routingContext -> {
                  HttpServerResponse response = routingContext.response();
                  response.putHeader("content-type", "text/html")
                          .end("<html><head><meta http-equiv=\"refresh\" content=\"5; url=/flames/\"></head><body><h1>Hello</h1></body></html>");
              });

        // Bind "/" to our hello message
        router.route("/flames/*")
              .handler(StaticHandler.create("flames")
                                    .setCachingEnabled(false));

        router.get("/api/list")
              .handler(rc -> {
                  jfr.list(newFuture(rc));
              })
              .failureHandler(this::failureHandler);

        router.post("/api/start/:pid")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  jfr.start(pid, newFuture(rc));
              })
              .failureHandler(this::failureHandler);

        router.get("/api/status/:pid")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  jfr.status(pid, newFuture(rc));
              });

        router.post("/api/dump/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  jfr.dump(pid, recording, newFuture(rc));
              });

        router.get("/api/dumps")
              .handler(rc -> {
                  jfr.listDumps(newFuture(rc));
              });

        router.get("/api/dump/:filename")
              .handler(rc -> {
                  String filename = rc.request()
                                 .getParam("filename");
                  Future<Void> status = jfr.dumpFromLocal(filename, rc);
                  status.setHandler(result -> {
                      if (result.failed()) {
                          managerError(rc, result);
                      }
                  });
              });

        router.get("/api/saves")
              .handler(rc -> {
                  jfr.listSavedDumps(newFuture(rc));
              });

        router.get("/api/saves/flames")
              .handler(rc -> {
                  jfr.listSavedFlames(newFuture(rc));
              });

        router.get("/api/saves/:key/flame")
              .handler(rc -> {
                  jfr.flameFromStorage(rc.request()
                                         .getParam("key"),
                          newFuture(rc));
              });

        router.post("/api/save/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  jfr.save(pid, recording, newFuture(rc));
              });

        router.post("/api/save/flame/:eventType/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  String eventType = rc.request()
                                       .getParam("eventType");

                  jfr.saveFlame(eventType, pid, recording, newFuture(rc));
              });

        router.post("/api/stop/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  jfr.stop(pid, recording, newFuture(rc));
              });

        router.get("/api/flames/:eventType/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  String eventType = rc.request()
                                       .getParam("eventType");

                  jfr.flames(eventType, pid, recording, newFuture(rc));
              });

        router.post("/form/file")
              .handler(rc -> {
                  Set<FileUpload> fileUploadSet = rc.fileUploads();
                  Iterator<FileUpload> fileUploadIterator = fileUploadSet.iterator();
                  @SuppressWarnings("rawtypes")
                  ArrayList<Future> all = new ArrayList<>();
                  while (fileUploadIterator.hasNext()) {
                      Future<String> future = Future.future();
                      all.add(future);
                      jfr.storeFileLocally(fileUploadIterator.next(), future);
                  }
                  CompositeFuture.all(all).setHandler(ar -> {
                      if (ar.succeeded()) {
                    	  rc.response().setStatusCode(200).end(Json.encodePrettily(ar.result().list()));
                      } else {
                          managerError(rc, ar);
                      }
                  });
              })
              .failureHandler(this::failureHandler);
              
        if (config().getBoolean("FLAMEGRAPHER_EPHEMERAL_PORT", Boolean.FALSE) != Boolean.TRUE) {
          serverPort = config().getInteger("FLAMEGRAPHER_HTTP_PORT", 8080);
        } else {
          try (ServerSocket s = new ServerSocket(0)) {
            serverPort = s.getLocalPort();
          } catch (IOException e) {
            logger.info("Unable to allocate ephemeral port", e);
            return;
          }          
        }
        serverAddress = config().getString("FLAMEGRAPHER_HTTP_SERVER", "0.0.0.0");
        Future<HttpServer> httpServerFuture = Future.future();
        vertx.createHttpServer()
             .requestHandler(router::accept)
             .listen(serverPort, serverAddress, httpServerFuture);

        httpServerFuture.compose(server -> {
            serverPort = server.actualPort();
            logger.info("Listening on http://" + (serverAddress.equals("0.0.0.0") ? "localhost" : serverAddress) + ":" + serverPort);
            
            fut.complete();
        }, fut);
    }

    private <T> Future<T> newFuture(RoutingContext rc) {
        Future<T> future = Future.future();
        future.setHandler(result -> {
            if (result.succeeded()) {
                rc.response()
                  .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
                  .end(Json.encodePrettily(result.result()));
            } else {
                managerError(rc, result);
            }
        });
        return future;
    }

    private void failureHandler(RoutingContext rc) {
        rc.response()
          .setStatusCode(500)
          .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
          .end(Json.encodePrettily(new ErrorResult("" + rc.failure())));

    }

    private static void managerError(RoutingContext rc, AsyncResult<?> result) {
        Throwable cause = result.cause();
        if (cause != null) {
            logger.error("Exception occured ", cause);
            rc.response()
              .setStatusCode(500)
              .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
              .end(Json.encodePrettily(new ErrorResult(result.cause()
                                                             .getMessage())));
        } else {
            logger.error("Future failed without exception " + result);
            rc.response()
              .setStatusCode(500)
              .putHeader(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_CHARSET_UTF_8)
              .end(Json.encodePrettily(new ErrorResult("Unknown error cause")));
        }
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        ConfigRetriever retriever = ConfigRetriever.create(vertx);
        retriever.getConfig(ar -> {
            if (ar.failed()) {
                logger.error("Unable to load configuration, using default.");
                vertx.deployVerticle(MainVerticle.class.getName());
            } else {
                vertx.deployVerticle(MainVerticle.class.getName(), new DeploymentOptions().setConfig(ar.result()));
            }
        });
    }
}
