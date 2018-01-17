<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>    
<%@page language="java" import="java.util.*"%>    
<%@page language="java" import="edu.stanford.muse.email.*"%>    
<%@page language="java" import="edu.stanford.muse.webapp.*"%>    
<%@page language="java" import="edu.stanford.muse.util.*"%>    
<%@page language="java" import="edu.stanford.muse.index.*"%>    
<%   
	// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
	JSPHelper.setPageUncacheable(response);
	String mode = request.getParameter("mode");
	int status = 0;
	if ("appraisal".equals(mode))
		ModeConfig.mode = ModeConfig.Mode.APPRAISAL;
	else if ("processing".equals(mode))
		ModeConfig.mode = ModeConfig.Mode.PROCESSING;
	else if ("discovery".equals(mode))
		ModeConfig.mode = ModeConfig.Mode.DISCOVERY;
	else if ("delivery".equals(mode))
		ModeConfig.mode = ModeConfig.Mode.DELIVERY;
	else
		status = 1;

	if (ModeConfig.isDiscoveryMode())
		System.getProperty("muse.mode.public", "1"); // some legacy code depends on this property instead of the context param
	else
		System.clearProperty("muse.mode.public"); // some legacy code depends on this property instead of the context param

	if (!session.isNew()) {
		session.removeAttribute("userKey");
		session.removeAttribute("emailDocs");
		session.removeAttribute("archive");
		// cache dir?

		session.removeAttribute("museEmailFetcher");
		session.removeAttribute("statusProvider");

		// for good measure, delete everything in the session
		Enumeration<String> names = session.getAttributeNames();
		while (names.hasMoreElements())
			session.removeAttribute(names.nextElement());
	}
		
	JSONObject obj = new JSONObject();
	obj.put("status", status);
	out.println (obj);
	JSPHelper.log.info(obj);
%>   
