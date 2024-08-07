package edu.stanford.muse.webapp;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

public class LoggingFilter implements Filter {

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
			throws java.io.IOException, javax.servlet.ServletException  { 
		String requestURL = ((HttpServletRequest) request).getRequestURL().toString();
		// we want to log only pages, not every little resource
		boolean logRequest = !requestURL.endsWith(".gif") && !requestURL.endsWith(".svg") && !requestURL.endsWith(".png") && !requestURL.endsWith(".jpg") && !requestURL.endsWith(".js") && !requestURL.endsWith(".css");
		if (requestURL.endsWith("muselog.jsp") || requestURL.endsWith("status") || requestURL.contains("serveImage") || requestURL.contains("serveAttachment"))
			logRequest = false;
		
		if (logRequest)
			JSPHelper.logRequest((HttpServletRequest) request);
		chain.doFilter(request,response);
		if (logRequest)
			JSPHelper.logRequestComplete((HttpServletRequest) request);
	} 

	@Override
	public void destroy() {
		JSPHelper.doLogging("Filter LoggingFilter destroyed");
		// TODO Auto-generated method stub

	}

	@Override
	public void init(FilterConfig arg0) {
		JSPHelper.doLogging("Filter LoggingFilter initialized");
		// TODO Auto-generated method stub		
	}
}
