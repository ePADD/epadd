<%@ page import="edu.stanford.muse.webapp.JSPHelper" %>
<%@ page import="edu.stanford.muse.util.Util" %>
<%
    // this JSP just ensures that we log the reasons for the error or exception to the warnings log, and then forwards to "/epadd/error"
    // if the page that met an error has a /ajax/ URL, it returns a JSON with status and error code
    JSPHelper.doLoggingWarnings("Error in JSP, code " +  request.getAttribute("javax.servlet.error.status_code") + ", Exception type: " + request.getAttribute("javax.servlet.error.exception_type"));
    Throwable throwable = (Throwable) request.getAttribute("javax.servlet.error.exception");
    if (throwable != null) {
        Util.print_exception(throwable, JSPHelper.log);
    }

    String request_uri = (String) request.getAttribute("javax.servlet.error.request_uri");
    if (request_uri != null && request_uri.contains ("/ajax/")) {
        response.setContentType("application/x-javascript");
        out.println ("{\"status\": 1, \"error\": 'Sorry, there was an error in the ePADD backend. Please retry after some time.'}");
    } else {
        response.sendRedirect("/epadd/error");
        // we prefer to include rather than forward because forwarding is more fragile
    }
%>