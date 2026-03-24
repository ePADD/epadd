package edu.stanford.muse.email;

public class FetchConfig implements java.io.Serializable {
	private static final long serialVersionUID = 1; // for compat, should have been 1

	public boolean downloadMessages;
	public boolean downloadAttachments;
	public boolean skipDuplicates = false; // when false, duplicate emails are imported instead of being dropped
	public Filter filter;
	
	public String toString()
	{
		return "downloadMessages: " + downloadMessages + " downloadAttachments: " + downloadAttachments + " skipDuplicates: " + skipDuplicates + " filter: " + filter;
	}
}