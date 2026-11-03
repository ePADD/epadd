package edu.stanford.epadd.util;

import org.junit.Test;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class SameOriginPostFilterTest {

    private static final String HOST = "localhost:9099";
    private static final String OWN_ORIGIN = "http://localhost:9099";

    /** Runs the filter for a request and returns the error it sent, or 0 if it passed the request on. */
    private static int run(String method, String origin, String referer, String host) throws Exception {
        Map<String, String> headers = new HashMap<>();
        headers.put("Origin", origin);
        headers.put("Referer", referer);
        headers.put("Host", host);

        HttpServletRequest request = (HttpServletRequest) Proxy.newProxyInstance(
                SameOriginPostFilterTest.class.getClassLoader(), new Class<?>[]{HttpServletRequest.class},
                (proxy, m, args) -> {
                    if (m.getName().equals("getMethod"))
                        return method;
                    if (m.getName().equals("getHeader"))
                        return headers.get((String) args[0]);
                    throw new UnsupportedOperationException(m.getName());
                });

        AtomicInteger error = new AtomicInteger();
        HttpServletResponse response = (HttpServletResponse) Proxy.newProxyInstance(
                SameOriginPostFilterTest.class.getClassLoader(), new Class<?>[]{HttpServletResponse.class},
                (proxy, m, args) -> {
                    if (m.getName().equals("sendError"))
                        error.set((Integer) args[0]);
                    return null;
                });

        AtomicBoolean passedOn = new AtomicBoolean();
        FilterChain chain = (req, res) -> passedOn.set(true);

        new SameOriginPostFilter().doFilter(request, response, chain);

        assertEquals("The request must either be refused or passed on", error.get() == 0, passedOn.get());
        return error.get();
    }

    @Test
    public void passesOnPostFromEpaddsOwnPages() throws Exception {
        assertEquals(0, run("POST", OWN_ORIGIN, OWN_ORIGIN + "/epadd/export.jsp", HOST));
    }

    @Test
    public void usesRefererIfThereIsNoOrigin() throws Exception {
        assertEquals(0, run("POST", null, OWN_ORIGIN + "/epadd/export.jsp", HOST));
        assertEquals(403, run("POST", null, "http://evil.example/page", HOST));
    }

    @Test
    public void refusesGet() throws Exception {
        // e.g. <img src="http://localhost:9099/epadd/ajax/delFolder?folder=...">
        assertEquals(405, run("GET", null, "http://evil.example/page", HOST));
        assertEquals(405, run("GET", OWN_ORIGIN, null, HOST));
    }

    @Test
    public void refusesPostFromAnotherSite() throws Exception {
        assertEquals(403, run("POST", "http://evil.example", null, HOST));
        // Origin decides, whatever the Referer says
        assertEquals(403, run("POST", "http://evil.example", OWN_ORIGIN + "/epadd/", HOST));
    }

    @Test
    public void refusesLookalikeOrigins() throws Exception {
        assertEquals(403, run("POST", "http://localhost:9099.evil.example", null, HOST));
        assertEquals(403, run("POST", "http://localhost:9099@evil.example", null, HOST));
        assertEquals(403, run("POST", "http://localhost:1234", null, HOST));
    }

    @Test
    public void refusesSandboxedAndMissingOrigins() throws Exception {
        assertEquals(403, run("POST", "null", OWN_ORIGIN + "/epadd/", HOST));
        assertEquals(403, run("POST", null, null, HOST));
        assertEquals(403, run("POST", "not a uri", null, HOST));
        assertEquals(403, run("POST", OWN_ORIGIN, null, null));
    }

    @Test
    public void hostComparisonIgnoresCase() {
        assertTrue(SameOriginPostFilter.isSameOrigin("http://LOCALHOST:9099", null, HOST));
    }
}
