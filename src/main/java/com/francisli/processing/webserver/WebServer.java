package com.francisli.processing.webserver;

import java.util.ArrayList;
import processing.core.PApplet;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

public class WebServer {
  protected static class RouteHandlerParams {
    protected RouteHandler handler;
    protected Request req;
    protected Response res;
    public RouteHandlerParams(RouteHandler handler, Request req, Response res) {
      this.handler = handler;
      this.req = req;
      this.res = res;
    }
  }

  protected PApplet parent;
  protected ArrayList<RouteHandlerParams> handlers = new ArrayList<RouteHandlerParams>();
  protected ArrayList<RouteHandlerParams> activeHandlers;

  public WebServer(PApplet parent) {
    this.parent = parent;
    parent.registerMethod("pre", this);
    parent.registerMethod("draw", this);
    parent.registerMethod("post", this);
    parent.registerMethod("dispose", this);
  }

  public WebServer(PApplet parent, int port) {
    this(parent);
    Spark.port(port);
  }

  protected Route createRoute(final RouteHandler handler) {
    return new Route() {
      public Object handle(Request req, Response res) {
        handler.background(req, res);
        RouteHandlerParams params = new RouteHandlerParams(handler, req, res);
        synchronized(WebServer.this) {
          handlers.add(params);
          while (handlers.contains(params)) {
            try {
              WebServer.this.wait();
            } catch (Exception e) { }
          }
        }
        return res.body();
      }
    };
  }

  public void GET(String path, final RouteHandler handler) {
    Spark.get(path, createRoute(handler));
  }

  public void POST(String path, final RouteHandler handler) {
    Spark.post(path, createRoute(handler));
  }

  public void pre() {
    synchronized(this) {
      activeHandlers = (ArrayList<RouteHandlerParams>) handlers.clone();
    }
    for (RouteHandlerParams params: activeHandlers) {
      params.handler.pre(params.req, params.res);
    }
  }

  public void draw() {
    for (RouteHandlerParams params: activeHandlers) {
      params.handler.draw(params.req, params.res);
    }
  }

  public void post() {
    synchronized(this) {
      handlers.removeAll(activeHandlers);
      activeHandlers = null;
      notifyAll();
    }
  }

  public void dispose() {
    Spark.stop();
  }
}
