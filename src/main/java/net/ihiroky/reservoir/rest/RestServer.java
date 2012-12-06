package net.ihiroky.reservoir.rest;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpContext;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import net.ihiroky.reservoir.Cache;
import net.ihiroky.reservoir.CountThreadFactory;
import net.ihiroky.reservoir.coder.JSONCoder;
import net.ihiroky.reservoir.coder.XMLCoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Application;
import javax.ws.rs.ext.RuntimeDelegate;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created on 12/10/19, 11:46
 *
 * @author Hiroki Itoh
 */
public class RestServer {

    private HttpServer server;
    private ExecutorService executor;
    private ReservoirService service;
    private Logger logger = LoggerFactory.getLogger(RestServer.class);

    public RestServer() {
        service = new ReservoirService();
    }

    public void start(String host, int port, String contextPath) throws IOException {
        start(host, port, contextPath, Collections.<String>emptyList());
    }

    public synchronized void start(String host, int port, String contextPath, Collection<String> noLogUrls) throws IOException {
        if (server == null) {
            HttpServer localServer = HttpServer.create(new InetSocketAddress(host, port), 0);

            RuntimeDelegate runtimeDelegate = RuntimeDelegate.getInstance();
            HttpHandler handler = runtimeDelegate.createEndpoint(
                    new ReservoirApplication(Arrays.asList(service), null), HttpHandler.class);
            HttpContext context = localServer.createContext(contextPath, handler);
            List<Filter> filterList = context.getFilters();
            filterList.add(new AccessLogFilter(contextPath, noLogUrls));
            if (noLogUrls.size() > 0) {
                logger.info("[start] no log urls: {}", noLogUrls);
            }

            executor = new ThreadPoolExecutor(1, Integer.MAX_VALUE, 60L, TimeUnit.SECONDS,
                    new SynchronousQueue<Runnable>(), new CountThreadFactory("RestServer/" + host + ":" + port));
            localServer.setExecutor(executor);
            server = localServer;
            localServer.start();

            logger.info("[start] start http server on {}", localServer.getAddress());
        }
    }

    public synchronized void stop() {
        if (server != null) {
            InetSocketAddress address = server.getAddress();
            server.stop(0);
            server = null;
            executor.shutdownNow();
            executor = null;
            logger.info("[stop] stop http server on {}", address);
        }
    }

    public synchronized boolean isStarted() {
        return server != null;
    }

    public <K, V> void addCache(Cache<K, V> cache, XMLCoder<K, V> xmlCoder, JSONCoder<K, V> jsonCoder) {
        service.addCache(cache, xmlCoder, jsonCoder);
    }

    public void removeCache(Cache<?, ?> cache) {
        service.removeCache(cache);
    }

    private static class ReservoirApplication extends Application {

        private Set<Class<?>> classes = new HashSet<Class<?>>();
        private Set<Object> singletons = new HashSet<Object>();

        public ReservoirApplication(List<?> singletons, List<Class<?>> classes) {
            this.singletons = (singletons != null) ?
                    new HashSet<Object>(singletons) : Collections.<Object>emptySet();
            this.classes = (classes != null) ?
                    new HashSet<Class<?>>(classes) : Collections.<Class<?>>emptySet();
        }

        @Override
        public Set<Object> getSingletons() {
            return singletons;
        }

        @Override
        public Set<Class<?>> getClasses() {
            return classes;
        }
    }
}
