package edu.stanford.muse.index;

/** tiny class representing a document with fixed content. meant mainly for dummy docs that hold a small string of text we need to index */
public class MemoryDocument extends Document implements java.io.Serializable {
	private static final long serialVersionUID = 1L;
	private static int num = 0;
	
	private final String contents;
	// note, not calling superclass constructor. docId remains 0.
	public MemoryDocument(String text)	{ 
		// warning: no sync used here
		super(Integer.toString(num++), ""); 
		contents = text;	
	}
	
	public String getContents() { return contents; }
}
