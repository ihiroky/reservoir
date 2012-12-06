package net.ihiroky.reservoir.rest;

import com.sun.net.httpserver.Filter;
import com.sun.net.httpserver.HttpExchange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FilterInputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created on 12/10/19, 12:12
 *
 * @author Hiroki Itoh
 */
public class AccessLogFilter extends Filter {

    private Set<String> noLogUrlSet;
    private Logger logger = LoggerFactory.getLogger(AccessLogFilter.class);

    public AccessLogFilter(String contextPath, Collection<String> noLogUrls) {
        Set<String> set = new HashSet<String>();
        for (String url : noLogUrls) {
            set.add(contextPath + '/' + url);
        }
        this.noLogUrlSet = set;
    }

    @Override
    public void doFilter(HttpExchange httpExchange, Chain chain)
            throws IOException {

        long start = System.currentTimeMillis();
        SizeCountInputStream in = new SizeCountInputStream(httpExchange.getRequestBody());
        SizeCountOutputStream out = new SizeCountOutputStream(httpExchange.getResponseBody());
        httpExchange.setStreams(in, out);
        chain.doFilter(httpExchange);
        long time = System.currentTimeMillis() - start;

        URI uri = httpExchange.getRequestURI();
        String rawPath = uri.getRawPath();
        if (!noLogUrlSet.contains(rawPath)) {
            logger.info("remote:[{}], uri:[{}], input:[{}], output:[{}], time:[{}]",
                    httpExchange.getRemoteAddress(), uri, in.length.get(), out.length.get(), time);
        } else {
            logger.debug("no log url request : {}", rawPath);
        }
    }

    @Override
    public String description() {
        return "write access log.";
    }

    private static class SizeCountInputStream extends FilterInputStream {

        AtomicLong length;

        SizeCountInputStream(InputStream in) {
            super(in);
            length = new AtomicLong();
        }

        @Override
        public int read() throws IOException {
            int read = super.read();
            if (read != -1) {
                length.incrementAndGet();
            }
            return read;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int read = super.read(b, off, len);
            if (read != -1) {
                length.addAndGet(len);
            }
            return read;
        }
    }

    private static class SizeCountOutputStream extends FilterOutputStream {

        AtomicLong length;

        /**
         * @param out
         */
        SizeCountOutputStream(OutputStream out) {
            super(out);
            length = new AtomicLong();
        }

        @Override
        public void write(byte[] data, int offset, int length) throws IOException {
            super.write(data, offset, length);
            this.length.addAndGet(length);
        }

        @Override
        public void write(int b) throws IOException {
            super.write(b);
            this.length.incrementAndGet();
        }
    }
}
