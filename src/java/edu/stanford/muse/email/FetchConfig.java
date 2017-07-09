package edu.stanford.muse.email;

public class FetchConfig implements java.io.Serializable {
	private static final long serialVersionUID = 1; // for compat, should have been 1

	public boolean downloadMessages;
	public boolean downloadAttachments;
	public Filter filter;
	
	public String toString()
	{
		return "downloadMessages: " + downloadMessages + " downloadAttachments: " + downloadAttachments + " filter: " + filter;
	}
}