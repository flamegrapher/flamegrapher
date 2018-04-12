package flamegrapher;

import flamegrapher.backend.JavaFlightRecorder;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

public class MainVerticle extends AbstractVerticle {

    @Override
    public void start(Future<Void> fut) {
        JavaFlightRecorder jfr = new JavaFlightRecorder(vertx);
        Router router = Router.router(vertx);
        router.route()
              .handler(BodyHandler.create());

        router.route("/")
              .handler(routingContext -> {
                  HttpServerResponse response = routingContext.response();
                  response.putHeader("content-type", "text/html")
                          .end("<h1>Hello</h1>");
              });

        // Bind "/" to our hello message
        router.route("/flames/*")
              .handler(StaticHandler.create("flames"));
              .setCachingEnabled(false));

        router.get("/api/list")
              .handler(rc -> {
                  jfr.list(newFuture(rc));
              });

        router.get("/api/start/:pid")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  jfr.start(pid, newFuture(rc));
              });

        router.get("/api/status/:pid")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  jfr.status(pid, newFuture(rc));
              });

        router.get("/api/dump/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  jfr.dump(pid, recording, newFuture(rc));
              });

        router.get("/api/stop/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  jfr.stop(pid, recording, newFuture(rc));
              });

        router.get("/api/flames/:pid/:recording")
              .handler(rc -> {
                  String pid = rc.request()
                                 .getParam("pid");
                  String recording = rc.request()
                                       .getParam("recording");
                  jfr.flames(pid, recording, newFuture(rc));
              });

        vertx.createHttpServer()
             .requestHandler(router::accept)
             .listen(config().getInteger("http.port", 8080), result -> {
                 if (result.succeeded()) {
                     fut.complete();
                 } else {
                     fut.fail(result.cause());
                 }
             });
    }

    private <T> Future<T> newFuture(RoutingContext rc) {
        Future<T> future = Future.future();
        future.setHandler(result -> {
            if (result.succeeded()) {
                rc.response()
                  .putHeader("content-type", "application/json; charset=utf-8")
                  .end(Json.encodePrettily(result.result()));
            } else {
                managerError(result);
            }
        });
        return future;
    }

    private static void managerError(AsyncResult<?> result) {
        // TODO: Manage errors properly
        System.err.println(result.cause());
    }

    public static void main(String[] args) {
        Vertx.vertx()
             .deployVerticle(MainVerticle.class.getName());
    }
}
