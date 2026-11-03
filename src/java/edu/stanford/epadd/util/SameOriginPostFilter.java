package edu.stanford.epadd.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Stops other web sites from using the browser of someone running ePADD to delete or create files, e.g. with an img
 * tag or a hidden form. These endpoints only accept POST requests that were sent from ePADD's own pages.
 *
 * This does not authenticate anyone: a program on another computer that can reach ePADD can still set these headers.
 */
@WebFilter(filterName = "SameOriginPostFilter", urlPatterns = {"/ajax/delFolder", "/ajax/newFolder", "/ajax/sidecarFileDelete"})
public class SameOriginPostFilter implements Filter {

    @Override
    public void init(FilterConfig filterConfig) {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (!"POST".equals(request.getMethod())) {
            response.setHeader("Allow", "POST");
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        } else if (!isSameOrigin(request.getHeader("Origin"), request.getHeader("Referer"), request.getHeader("Host"))) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN);
        } else {
            chain.doFilter(request, response);
        }
    }

    @Override
    public void destroy() {
    }

    /**
     * Browsers send Origin with POST requests and don't let pages change it. Referer is only used if there is no
     * Origin. A request with neither is refused, so that this fails closed.
     */
    static boolean isSameOrigin(String origin, String referer, String host) {
        if (host == null || host.isEmpty())
            return false;
        String source = origin != null ? origin : referer;
        if (source == null)
            return false;
        try {
            String authority = new URI(source).getRawAuthority();
            return authority != null && authority.equalsIgnoreCase(host);
        } catch (URISyntaxException e) {
            return false;
        }
    }
}
