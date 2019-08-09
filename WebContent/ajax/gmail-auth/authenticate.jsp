<%@page language="java" import="edu.stanford.muse.email.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@ page import="edu.stanford.epadd.util.OperationInfo" %>
<%@ page import="org.json.JSONObject" %>
<%@page contentType="application/json; charset=UTF-8" pageEncoding="UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@ page import="edu.stanford.muse.webapp.JSPHelper"%>
<%@ page import="org.json.JSONObject"%>
<%@ page import="edu.stanford.muse.email.GmailAuth.AuthenticatedUserInfo"%>
<%@ page import="edu.stanford.muse.email.GmailAuth.GoogleLoginVerifier"%>
<%
    // WARNING: SECURITY-CRITICAL CODE. Should not be modified without approval from Jaya.


    String idTokenString = request.getParameter("idToken");
	String accessTokenString = request.getParameter("accessToken");
    AuthenticatedUserInfo authUserInfo = null;
    String errorMessage = null;
    {

                JSPHelper.log.info("Google idToken:" + idTokenString);
                authUserInfo = GoogleLoginVerifier.verify(idTokenString,accessTokenString);
                if (authUserInfo == null) {
                    errorMessage ="Google token verification failed, Invalid ID token:" + idTokenString;
                }
                //set accessToken in authUserInfo.


    }

    if (authUserInfo == null) {
        JSONObject result = new JSONObject();
        JSPHelper.log.warn("User authentication failed with:" + errorMessage);
        result.put("error", errorMessage);
        result.put("status", 1);
        out.println (result.toString());
        return;
    }

    session.setAttribute("authInfo",authUserInfo);
    out.println("{\"status\":0}");
%>
