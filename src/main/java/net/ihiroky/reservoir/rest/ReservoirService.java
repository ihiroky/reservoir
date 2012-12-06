package net.ihiroky.reservoir.rest;

import net.ihiroky.reservoir.Cache;
import net.ihiroky.reservoir.coder.JSONCoder;
import net.ihiroky.reservoir.coder.XMLCoder;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HEAD;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * Created on 12/10/19, 10:37
 *
 * @author Hiroki Itoh
 */
@Path("/")
public class ReservoirService {

    private ConcurrentMap<String, Container> cacheMap;

    static class Container {
        final Cache<Object, Object> cache;
        final XMLCoder<Object, Object> xmlCoder;
        final JSONCoder<Object, Object> jsonCoder;

        @SuppressWarnings("unchecked")
        Container(Cache<?, ?> cache, XMLCoder<?, ?> xmlCoder, JSONCoder<?, ?> jsonCoder) {
            this.cache = (Cache<Object, Object>) cache;
            this.xmlCoder = (XMLCoder<Object, Object>) xmlCoder;
            this.jsonCoder = (JSONCoder<Object, Object>) jsonCoder;
        }

        @Override
        public int hashCode() {
            int hash = cache.hashCode();
            if (xmlCoder != null) {
                hash ^= xmlCoder.hashCode();
            }
            if (jsonCoder != null) {
                hash ^= jsonCoder.hashCode();
            }
            return hash;
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof Container)) {
                return false;
            }
            Container that = (Container) object;
            return this.cache == that.cache
                    && this.xmlCoder == that.xmlCoder
                    && this.jsonCoder == that.jsonCoder;
        }
    }

    private static final String HEADER_CACHE_NAME = "X-CACHE-NAME";
    private static final String HEADER_CACHE_SIZE = "X-CACHE-SIZE";
    private static final String HEADER_CACHE_INDEX_CLASS = "X-CACHE-INDEX";
    private static final String HEADER_CACHE_ACCESSOR_CLASS = "X-CACHE-ACCESSOR";
    private static final String HEADER_CACHE_KEY_RESOLVER_CLASS = "X-CACHE-KEY-RESOLVER";
    private static final String HEADER_CACHE_XML_CODER_CLASS = "X-CACHE-XML-CODER";
    private static final String HEADER_CACHE_JSON_CODER_CLASS = "X-CACHE-JSON-CODER";
    private static final int HTTP_NOT_IMPLEMENTED = HttpURLConnection.HTTP_NOT_IMPLEMENTED;

    public ReservoirService() {
        cacheMap = new ConcurrentHashMap<String, Container>();
    }

    // compile time K, V type check.
    public <K, V> void addCache(Cache<K, V> cache, XMLCoder<K, V> xmlCoder, JSONCoder<K, V> jsonCoder) {
        if (cache == null) {
            throw new NullPointerException("cache");
        }
        if (xmlCoder == null && jsonCoder == null) {
            throw new IllegalArgumentException("xmlCoder and jsonCoder is null.");
        }
        Container container = new Container(cache, xmlCoder, jsonCoder);
        if (cacheMap.putIfAbsent(cache.getName(), container) != null) {
            throw new IllegalStateException(cache.getName() + (" already exists."));
        }
    }

    public void removeCache(Cache<?, ?> cache) {
        cacheMap.remove(cache.getName(), cache);
    }

    @GET
    @Path("/{cache}/{key}")
    @Produces(MediaType.TEXT_PLAIN)
    public String getValue(@PathParam("cache") String cache, @PathParam("key") String key) {
        Container container = cacheMap.get(cache);
        return (container != null) ? container.cache.referEntry(key) : null;
    }

    private void checkNull(Container container, String cache) {
        if (container == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND).header(HEADER_CACHE_NAME, cache).build());
        }
    }

    @GET
    @Path("/{cache}/{key}")
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public StreamingOutput getValueXML(
            @PathParam("cache") final String cache,
            @PathParam("key") final String key,
            @QueryParam("regex") final boolean regex) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                Container container = cacheMap.get(cache);
                checkNull(container, cache);
                try {
                    if (regex) {
                        container.xmlCoder.write(Pattern.compile(key), container.cache, outputStream);
                    } else {
                        container.xmlCoder.write(key, container.cache, outputStream);
                    }
                } catch (UnsupportedOperationException uoe) {
                    throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
                } catch (Exception e) {
                    throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @PUT
    @Path("/{cache}")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public Response putValueXML(
            @PathParam("cache") final String cache, InputStream inputStream) {
        Container container = cacheMap.get(cache);
        checkNull(container, cache);
        try {
            container.xmlCoder.read(container.cache, inputStream);
            return Response.noContent().build();
        } catch (UnsupportedOperationException uoe) {
            throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }


    @DELETE
    @Path("/{cache}/{key}")
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public StreamingOutput deleteValueXML(
            @PathParam("cache") final String cache,
            @PathParam("key") final String key,
            @QueryParam("regex") final boolean regex) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                Container container = cacheMap.get(cache);
                checkNull(container, cache);
                try {
                    if (regex) {
                        container.xmlCoder.delete(Pattern.compile(key), container.cache, outputStream);
                    } else {
                        container.xmlCoder.delete(key, container.cache, outputStream);
                    }
                } catch (UnsupportedOperationException uoe) {
                    throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
                } catch (Exception e) {
                    throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @DELETE
    @Path("/{cache}")
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public Response deleteValueXML(
            @PathParam("cache") final String cache, InputStream inputStream) {
        Container container = cacheMap.get(cache);
        checkNull(container, cache);
        try {
            container.xmlCoder.delete(container.cache, inputStream);
            return Response.noContent().build();
        } catch (UnsupportedOperationException uoe) {
            throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @GET
    @Path("/{cache}/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamingOutput getValueJSON(
            @PathParam("cache") final String cache,
            @PathParam("key") final String key,
            @QueryParam("regex") final boolean regex) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                Container container = cacheMap.get(cache);
                checkNull(container, cache);
                try {
                    if (regex) {
                        container.jsonCoder.write(Pattern.compile(key), container.cache, outputStream);
                    } else {
                        container.jsonCoder.write(key, container.cache, outputStream);
                    }
                } catch (UnsupportedOperationException uoe) {
                    throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
                } catch (Exception e) {
                    throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @PUT
    @Path("/{cache}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response putValueJSON(
            @PathParam("cache") final String cache, InputStream inputStream) {
        Container container = cacheMap.get(cache);
        checkNull(container, cache);
        try {
            container.jsonCoder.read(container.cache, inputStream);
            return Response.noContent().build();
        } catch (UnsupportedOperationException uoe) {
            throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @DELETE
    @Path("/{cache}/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    public StreamingOutput deleteValueJSON(
            @PathParam("cache") final String cache,
            @PathParam("key") final String key,
            @QueryParam("regex") final boolean regex) {
        return new StreamingOutput() {
            @Override
            public void write(OutputStream outputStream) throws IOException, WebApplicationException {
                Container container = cacheMap.get(cache);
                checkNull(container, cache);
                try {
                    if (regex) {
                        container.jsonCoder.delete(Pattern.compile(key), container.cache, outputStream);
                    } else {
                        container.jsonCoder.delete(key, container.cache, outputStream);
                    }
                } catch (UnsupportedOperationException uoe) {
                    throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
                } catch (Exception e) {
                    throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
                }
            }
        };
    }

    @DELETE
    @Path("/{cache}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteValueJSON(
            @PathParam("cache") final String cache, InputStream inputStream) {
        Container container = cacheMap.get(cache);
        checkNull(container, cache);
        try {
            container.jsonCoder.delete(container.cache, inputStream);
            return Response.noContent().build();
        } catch (UnsupportedOperationException uoe) {
            throw new WebApplicationException(uoe, HTTP_NOT_IMPLEMENTED);
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    @HEAD
    @Path("/{cache}")
    public Response getMetadata(@PathParam("cache") final String cache) {
        Container container = cacheMap.get(cache);
        checkNull(container, cache);
        Cache<?, ?> c = container.cache;
        Response.ResponseBuilder responseBuilder = Response.ok()
                .header(HEADER_CACHE_NAME, c.getName())
                .header(HEADER_CACHE_SIZE, c.size())
                .header(HEADER_CACHE_INDEX_CLASS, c.getIndexClassName())
                .header(HEADER_CACHE_ACCESSOR_CLASS, c.getCacheAccessorClassName())
                .header(HEADER_CACHE_KEY_RESOLVER_CLASS, c.getStringKeyResolverClassName());
        if (container.xmlCoder != null) {
            responseBuilder.header(HEADER_CACHE_XML_CODER_CLASS, container.xmlCoder.getClass().getName());
        }
        if (container.jsonCoder != null) {
            responseBuilder.header(HEADER_CACHE_JSON_CODER_CLASS, container.jsonCoder.getClass().getName());
        }
        return responseBuilder.build();
    }
}
