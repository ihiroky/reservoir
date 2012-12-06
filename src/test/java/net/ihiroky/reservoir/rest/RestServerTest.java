package net.ihiroky.reservoir.rest;

import net.ihiroky.reservoir.Cache;
import net.ihiroky.reservoir.Reservoir;
import net.ihiroky.reservoir.StringResolver;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Random;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/22, 17:49
 *
 * @author Hiroki Itoh
 */
public class RestServerTest {

    private static Cache<Integer, Integer> integerCache;
    private static RestServer restServer;
    private static String url;

    private static final String CHARSET = "UTF-8";

    @BeforeClass
    public static void beforeClass() throws Exception {
        System.setProperty("http.nonProxyHosts", "localhost|127.0.0.1");

        integerCache = Reservoir.newCacheBuilder().name("RestServerTest").build();
        restServer = new RestServer();
        Random random = new Random(System.currentTimeMillis());
        int port = 0;
        for (int i = 0; i < 10; i++) {
            // non ephemeral port over 1024.
            port = random.nextInt(32768 - 1025) + 1025;
            try {
                restServer.start("localhost", port, "/test");
                break;
            } catch (Exception e) {
            }
        }
        if (!restServer.isStarted()) {
            fail("failed to start RestServer.");
        }
        integerCache.setStringKeyResolver(StringResolver.INTEGER);
        integerCache.put(0, 10);
        integerCache.put(1, 11);
        integerCache.put(2, 12);
        integerCache.put(3, 13);
        integerCache.put(4, 14);
        restServer.addCache(integerCache, new IntegerXMLCoder(), new IntegerJSONCoder());
        url = "http://localhost:" + port + "/test/" + integerCache.getName();
        System.out.println(url);
    }

    @AfterClass
    public static void afterClass() {
        restServer.stop();
        integerCache.dispose();
    }

    private String url(String key, boolean regex) throws Exception {
        return url + "/" + ((key != null) ? URLEncoder.encode(key, CHARSET) : "") + (regex ? "?regex=true" : "");
    }

    private String get(String url, String accept) throws Exception {
        URL targetUrl = new URL(url);
        HttpURLConnection con = (HttpURLConnection) targetUrl.openConnection();
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setConnectTimeout(3000);
        con.setReadTimeout(3000);
        con.setAllowUserInteraction(false);
        con.setRequestMethod("GET");
        con.setRequestProperty("Accept", accept);
        con.connect();

        InputStream in = con.getInputStream();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buffer = new byte[256];
            for (int read; (read = in.read(buffer)) != -1; ) {
                out.write(buffer, 0, read);
            }
            return out.toString(CHARSET);
        } finally {
            in.close();
        }
    }

    private void put(String url, String contentType, String content) throws Exception {
        URL targetUrl = new URL(url);
        HttpURLConnection con = (HttpURLConnection) targetUrl.openConnection();
        con.setDoInput(true);
        con.setDoOutput(true);
        con.setUseCaches(false);
        con.setConnectTimeout(3000);
        con.setReadTimeout(3000);
        con.setAllowUserInteraction(false);
        con.setRequestMethod("PUT");
        con.setRequestProperty("Content-Type", contentType);
        con.connect();

        OutputStream out = con.getOutputStream();
        try {
            out.write(content.getBytes(CHARSET));
        } finally {
            out.close();
        }
        assertThat(con.getResponseCode(), is(HttpURLConnection.HTTP_NO_CONTENT));
    }

    private HttpURLConnection head(String url) throws Exception {
        URL targetUrl = new URL(url);
        HttpURLConnection con = (HttpURLConnection) targetUrl.openConnection();
        con.setUseCaches(false);
        con.setConnectTimeout(3000);
        con.setReadTimeout(3000);
        con.setAllowUserInteraction(false);
        con.setRequestMethod("HEAD");
        con.connect();
        assertThat(con.getResponseCode(), is(HttpURLConnection.HTTP_OK));
        return con;
    }

    @Test
    public void testPlain() throws Exception {
        String result = get(url("1", false), "text/plain");
        assertThat(result, is("11"));
    }

    @Test
    public void testGetXML() throws Exception {
        String result = get(url("1", false), "text/xml");
        assertThat(result, is("<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"1\">11</entry>"
                + "</entries>"));

        result = get(url("^[23]$", true), "text/xml");
        assertThat(result, is("<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"3\">13</entry>"
                + "<entry key=\"2\">12</entry>"
                + "</entries>"));

        result = get(url("1", false), "application/xml");
        assertThat(result, is("<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"1\">11</entry>"
                + "</entries>"));
    }

    @Test
    public void testPutXML() throws Exception {
        assertThat(integerCache.get(10), is(nullValue()));
        String content = "<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"10\">20</entry>"
                + "</entries>";
        put(url(null, false), "text/xml", content);
        assertThat(integerCache.get(10), is(20));

        assertThat(integerCache.get(11), is(nullValue()));
        assertThat(integerCache.get(12), is(nullValue()));
        content = "<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"11\">21</entry>"
                + "<entry key=\"12\">22</entry>"
                + "</entries>";
        put(url(null, false), "text/xml", content);
        assertThat(integerCache.get(11), is(21));
        assertThat(integerCache.get(12), is(22));

        assertThat(integerCache.get(13), is(nullValue()));
        content = "<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"13\">23</entry>"
                + "</entries>";
        put(url(null, false), "application/xml", content);
        assertThat(integerCache.get(13), is(23));
    }

    private String json(int key, int value) {
        return "{\"k\":" + key + ",\"v\":" + value + "}";
    }

    @Test
    public void testGetJSON() throws Exception {
        String result = get(url("1", false), "application/json");
        assertThat(result, is("[" + json(1, 11) + "]"));

        result = get(url("^[23]$", true), "application/json");
        assertThat(result, is("[" + json(3, 13) + "," + json(2, 12) + "]"));
    }

    @Test
    public void testPutJSON() throws Exception {
        assertThat(integerCache.get(20), is(nullValue()));
        String content = "[" + json(20, 30) + "]";
        put(url(null, false), "application/json", content);
        assertThat(integerCache.get(20), is(30));

        assertThat(integerCache.get(21), is(nullValue()));
        assertThat(integerCache.get(22), is(nullValue()));
        content = "[" + json(21, 31) + "," + json(22, 32) + "]";
        put(url(null, false), "application/json", content);
        assertThat(integerCache.get(21), is(31));
        assertThat(integerCache.get(22), is(32));
    }
}
