/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package edu.stanford.muse.datacache;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
////import org.apache.commons.logging.LogFactory;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;

public class Blob implements Serializable {

private static final Logger log =  LogManager.getLogger(Blob.class);

private final static long serialVersionUID = 1L;

public long size = -1;
protected String filename;
public String contentType;

public boolean processedSuccessfully = false;

	public void setContentHash(byte[] contentHash) {
		this.contentHash = contentHash;
	}

	// we currently store content hash both as byte array and string
//@SuppressWarnings("unused")
private byte[] contentHash;
//private String content_hash_string;

public Date modifiedDate;
/*
transient private static final Parser parser = new AutoDetectParser();
transient private static final ParseContext context = new ParseContext();*/

private String getName() { return filename; }
public String getResourceURI() { return this.filename; }
// public String getContentHash() { return this.content_hash_string; }
public Date getModifiedDate() { return modifiedDate; }

public long getSize() { return size; }

public String[] getTypes() { return null; } /* dummy */

	public String[] getCacheURIs() { return null; } /* dummy */

public String[] getTopics()
{
    /* empty for now */
    return new String[0];
}

public String toString()
{
	return "filename " + filename + " size = " + size + "\n";
}

	/** a blob is the same as another one if it has the same name, and the same content hash */
public boolean equals (Object o)
{
	if (!(o instanceof Blob))
		return false;
	Blob b = (Blob) o;
//	return (b.filename != this.filename && Util.byteArrayToHexString(b.contentHash).equals(Util.byteArrayToHexString(this.contentHash)) && b.size == this.size);
//	return (b.filename != this.filename && b.size == this.size);

	if (this.contentHash == null || b.contentHash == null)
		return false;
	if (this.filename == null || b.filename == null)
		return false;
	if (!this.filename.equals(b.filename))
		return false;
	return (Arrays.equals(b.contentHash, this.contentHash));
}

public int hashCode()
{
    if (size == -1)
        log.warn ("Hashcode called on blob without size being set first");
	int filenameHash = (filename != null) ? filename.hashCode() : 0;
	return filenameHash ^ Arrays.hashCode(contentHash);
}

//	public int hashCode()
//	{
//		return this.filename.hashCode() ^ Long.toString(this.size).hashCode();
//	}

//public String getURI() { return null; }

    public static class BlobStats {
        public long unique_data_size;
        public long total_data_size;
        public long n_unique_pics;
        public long n_total_pics;

        public BlobStats(long unique_data_size, long total_data_size,
                long n_unique_pics, long n_total_pics) {
            this.unique_data_size = unique_data_size;
            this.total_data_size = total_data_size;
            this.n_unique_pics = n_unique_pics;
            this.n_total_pics = n_total_pics;
        }
    }
}