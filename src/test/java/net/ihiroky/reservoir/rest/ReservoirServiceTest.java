package net.ihiroky.reservoir.rest;

import net.ihiroky.reservoir.Cache;
import net.ihiroky.reservoir.Reservoir;
import net.ihiroky.reservoir.StringResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

/**
 * Created on 12/10/22, 9:29
 *
 * @author Hiroki Itoh
 */
public class ReservoirServiceTest {

    ReservoirService service;
    Cache<Integer, Integer> integerCache;
    Collection<Cache<?, ?>> disposeSet;

    static final String KEY_CACHE_NAME = "ReservoirServiceTest";

    @Before
    public void before() {
        disposeSet = new HashSet<Cache<?, ?>>();
        integerCache = Reservoir.newCacheBuilder().name(KEY_CACHE_NAME).build();
        integerCache.put(0, 10);
        integerCache.put(1, 11);
        integerCache.put(2, 12);
        integerCache.put(3, 13);
        integerCache.setStringKeyResolver(StringResolver.INTEGER);
        disposeSet.add(integerCache);
        service = new ReservoirService();
        service.addCache(integerCache, new IntegerXMLCoder(), new IntegerJSONCoder());
    }

    @After
    public void after() {
        for (Cache<?, ?> cache : disposeSet) {
            cache.dispose();
        }
    }

    @Test
    public void testGetValue() throws Exception {
        String result = service.getValue(KEY_CACHE_NAME, "2");
        assertThat(result, is("12"));

        result = service.getValue(KEY_CACHE_NAME, "-1");
        assertThat(result, is(nullValue()));
    }

    @Test
    public void testGetValueXML() throws Exception {
        StreamingOutput output = service.getValueXML(KEY_CACHE_NAME, "2", false);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString("UTF-8"),
                is("<?xml version=\"1.0\" ?><entries><entry key=\"2\">12</entry></entries>"));

        output = service.getValueXML(KEY_CACHE_NAME, "-1", false);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(),
                is("<?xml version=\"1.0\" ?><entries></entries>"));

        output = service.getValueXML(KEY_CACHE_NAME, "[13]", true);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(),
                is("<?xml version=\"1.0\" ?><entries><entry key=\"3\">13</entry><entry key=\"1\">11</entry></entries>"));

        output = service.getValueXML(KEY_CACHE_NAME, "[4-9]", true);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(),
                is("<?xml version=\"1.0\" ?><entries></entries>"));
    }

    @Test
    public void testPutValueXML() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(("<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"13\">23</entry>"
                + "<entry key=\"11\">21</entry>"
                + "</entries>").getBytes("UTF-8"));
        int beforeSize = integerCache.size();
        service.putValueXML(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(11), is(21));
        assertThat(integerCache.get(13), is(23));
        assertThat(integerCache.size(), is(beforeSize + 2));

        in = new ByteArrayInputStream(("<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"12\">22</entry>"
                + "</entries>").getBytes("UTF-8"));
        beforeSize = integerCache.size();
        service.putValueXML(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(12), is(22));
        assertThat(integerCache.size(), is(beforeSize + 1));


        in = new ByteArrayInputStream(("<?xml version=\"1.0\" ?><entries>"
                + "</entries>").getBytes("UTF-8"));
        beforeSize = integerCache.size();
        service.putValueXML(KEY_CACHE_NAME, in);
        assertThat(integerCache.size(), is(beforeSize));
    }

    @Test
    public void testDeleteValueXMLByURL() throws Exception {
        int beforeSize = integerCache.size();
        StreamingOutput output = service.deleteValueXML(KEY_CACHE_NAME, "1", false);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("<?xml version=\"1.0\" ?><entries><entry key=\"1\">11</entry></entries>"));
        assertThat(integerCache.get(1), is(nullValue()));
        assertThat(integerCache.size(), is(beforeSize - 1));

        beforeSize = integerCache.size();
        output = service.deleteValueXML(KEY_CACHE_NAME, "[02]", true);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"2\">12</entry>"
                + "<entry key=\"0\">10</entry>"
                + "</entries>"));
        assertThat(integerCache.get(0), is(nullValue()));
        assertThat(integerCache.get(2), is(nullValue()));
        assertThat(integerCache.size(), is(beforeSize - 2));
    }

    @Test
    public void testDeleteValueXMLByStream() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(("<?xml version=\"1.0\" ?><entries>"
                + "<entry key=\"3\" />"
                + "<entry key=\"1\" />"
                + "</entries>").getBytes("UTF-8"));
        int beforeSize = integerCache.size();
        service.deleteValueXML(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(1), is(nullValue()));
        assertThat(integerCache.get(3), is(nullValue()));
        assertThat(integerCache.size(), is(beforeSize - 2));

        in = new ByteArrayInputStream(("<?xml version=\"1.0\" ?><entries>"
                + "</entries>").getBytes("UTF-8"));
        beforeSize = integerCache.size();
        service.deleteValueXML(KEY_CACHE_NAME, in);
        assertThat(integerCache.size(), is(beforeSize));
    }

    private String json(int key, int value) {
        return "{\"k\":" + key + ",\"v\":" + value + "}";
    }

    @Test
    public void testGetValueJSON() throws Exception {
        StreamingOutput output = service.getValueJSON(KEY_CACHE_NAME, "2", false);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("[" + json(2, 12) + "]"));

        output = service.getValueJSON(KEY_CACHE_NAME, "-1", false);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("[]"));

        output = service.getValueJSON(KEY_CACHE_NAME, "[13]", true);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("[" + json(3, 13) + "," + json(1, 11) + "]"));

        output = service.getValueJSON(KEY_CACHE_NAME, "[4-9]", true);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("[]"));
    }

    @Test
    public void testPutValueJSON() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(
                ("[" + json(23, 33) + "," + json(21, 31) + "]").getBytes("UTF-8"));
        int beforeSize = integerCache.size();
        service.putValueJSON(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(21), is(31));
        assertThat(integerCache.get(23), is(33));
        assertThat(integerCache.size(), is(beforeSize + 2));

        in = new ByteArrayInputStream(("[" + json(22, 32) + "]").getBytes("UTF-8"));
        beforeSize = integerCache.size();
        service.putValueJSON(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(22), is(32));
        assertThat(integerCache.size(), is(beforeSize + 1));

        in = new ByteArrayInputStream(json(24, 34).getBytes("UTF-8")); // no [ and ]
        beforeSize = integerCache.size();
        service.putValueJSON(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(24), is(34));
        assertThat(integerCache.size(), is(beforeSize + 1));

        in = new ByteArrayInputStream("[]".getBytes("UTF-8"));
        beforeSize = integerCache.size();
        service.putValueJSON(KEY_CACHE_NAME, in);
        assertThat(integerCache.size(), is(beforeSize));
    }

    @Test
    public void testDeleteValueJSONByURL() throws Exception {
        int beforeSize = integerCache.size();
        StreamingOutput output = service.deleteValueJSON(KEY_CACHE_NAME, "1", false);
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("[" + json(1, 11) + "]"));
        assertThat(integerCache.get(1), is(nullValue()));
        assertThat(integerCache.size(), is(beforeSize - 1));

        beforeSize = integerCache.size();
        output = service.deleteValueJSON(KEY_CACHE_NAME, "[02]", true);
        result = new ByteArrayOutputStream();
        output.write(result);
        assertThat(result.toString(), is("[" + json(2, 12) + "," + json(0, 10) + "]"));
        assertThat(integerCache.get(0), is(nullValue()));
        assertThat(integerCache.get(2), is(nullValue()));
        assertThat(integerCache.size(), is(beforeSize - 2));
    }

    @Test
    public void testDeleteValueJSONByStream() throws Exception {
        ByteArrayInputStream in = new ByteArrayInputStream(("[{\"k\":3},{\"k\":1}]").getBytes("UTF-8"));
        int beforeSize = integerCache.size();
        service.deleteValueJSON(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(1), is(nullValue()));
        assertThat(integerCache.get(3), is(nullValue()));
        assertThat(integerCache.size(), is(beforeSize - 2));

        in = new ByteArrayInputStream(("[]").getBytes("UTF-8"));
        beforeSize = integerCache.size();
        service.deleteValueJSON(KEY_CACHE_NAME, in);
        assertThat(integerCache.size(), is(beforeSize));

        in = new ByteArrayInputStream("{\"k\":2}".getBytes());
        beforeSize = integerCache.size();
        service.deleteValueJSON(KEY_CACHE_NAME, in);
        assertThat(integerCache.get(2), is(nullValue()));
        assertThat(integerCache.size(), is(beforeSize - 1));
    }

    @Test
    public void testGetMetadata() throws Exception {
        Response response = service.getMetadata(KEY_CACHE_NAME);
        MultivaluedMap<String, Object> header = response.getMetadata();
        assertThat(header.get("X-CACHE-NAME"), is((Object) Arrays.asList(KEY_CACHE_NAME)));
        assertThat(header.get("X-CACHE-SIZE"), is((Object) Arrays.asList(4)));
        assertThat(header.get("X-CACHE-INDEX"), is((Object) Arrays.asList("net.ihiroky.reservoir.index.ConcurrentLRUIndex")));
        assertThat(header.get("X-CACHE-ACCESSOR"), is((Object) Arrays.asList("net.ihiroky.reservoir.storage.HeapStorageAccessor")));
        assertThat(header.get("X-CACHE-KEY-RESOLVER"), is((Object) Arrays.asList("net.ihiroky.reservoir.StringResolver$3")));
        assertThat(header.get("X-CACHE-XML-CODER"), is((Object) Arrays.asList("net.ihiroky.reservoir.rest.IntegerXMLCoder")));
        assertThat(header.get("X-CACHE-JSON-CODER"), is((Object) Arrays.asList("net.ihiroky.reservoir.rest.IntegerJSONCoder")));
        assertThat(header.size(), is(7));
    }
}
