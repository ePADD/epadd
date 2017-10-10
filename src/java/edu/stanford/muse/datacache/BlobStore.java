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

import com.google.common.collect.Multimap;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;

public class BlobStore implements Serializable {

    public Set<Blob> uniqueBlobs = new LinkedHashSet<Blob>();

    // mapping of each data item to a data id
    private Map<Blob, Integer> id_map = new LinkedHashMap<Blob, Integer>();
    private Map<Blob, URL> urlMap = new LinkedHashMap<Blob, URL>(); // -- seems this is not really used
    // data id's are just assigned sequentially starting from 0
    private int next_data_id = 0;

    // mapping of each data to its views
    private Map<Blob, Map<String,Object>> views = new LinkedHashMap<>();

    public Multimap<Blob, String> getBlobToKeywords() {
        return blobToKeywords;
    }

    public void setBlobToKeywords(Multimap<Blob, String> blobToKeywords) {
        this.blobToKeywords = blobToKeywords;
    }

    public Collection<String> getKeywordsForBlob (Blob b) {
        return blobToKeywords.get(b);
    }

    private Multimap<Blob, String> blobToKeywords;

    private static Log log = LogFactory.getLog(BlobStore.class);
    private final static long serialVersionUID = 1L;

    private String dir; // base dir where this data store keeps it files
    private static final String META_DATA_FILENAME = ".MetaData"; // filename where meta data is kept

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    //remove final later if we want this to be user configurable
//pdfbox sometimes crashes the VM mysteriously on Macs
    private final static boolean NO_CONVERT_PDFS = System.getProperty("pdf.thumbnails") == null;

    /**
     * @param dir directory where this store keeps its data
     */
    public BlobStore(String dir) throws IOException {
        log.info("Opening file repository in " + dir);
        this.dir = dir;
        File dir_file = new File(dir);
        if (!dir_file.exists())
            dir_file.mkdirs();
        else {
            // could also lock the dir to make sure no other data store object uses this dir inadvertently
            File f = new File(this.dir + File.separatorChar + META_DATA_FILENAME);
            if (f.exists()) {
                unpack();
                log.info("File repository in " + dir + " has " + uniqueBlobs.size() + " entries");
            }
        }
    }

    /**
     * use with care!
     */
    public void setDir(String dir) {
        this.dir = dir;
    }

    /**
     * copies the selected blobs to a new file blobstore at the given path
     */
    public BlobStore createCopy(String path, Collection<Blob> blobs) throws IOException {
        BlobStore fbs = new BlobStore(path); // no owner field
        for (Blob b : blobs) {
            try {
                // path can be something like E:\/Users/xyz
                // get_URL(b) can return something like file:E:\/Users\hangal\ePADD archive of hangal@gmail.com\blobs
                String urlString = get_URL(b); // .replaceAll("\\\\", "/");
                // urlString can be something like file:E://Users/hangal/ePADD archive of hangal@gmail.com/blobs
                // very importantly, the E:/Users/... needs to be made E://
                //	    urlString = urlString.replaceAll(":/", "://"); //
                URL url = new URL(urlString); // replaceAll expects a regex, so it needs to get a \\, so we need to write \\\\ !
                fbs.add(b, url.openStream()); // get_URL returns things with \ sometimes
            /* this code is causing trouble, so drop these views. openstreams are not serializable, dunno why they are kept with the views
			for (String view: getViews(b))
				fbs.addView(b, view, new URL(getViewURL(b, view)).openStream());
				*/
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
        }
        fbs.pack();
        return fbs;
    }

    /**
     * directory where this store keeps it data
     */
    public String getDir() {
        return dir;
    }

    private synchronized void unpack() throws IOException {
        String f = this.dir + File.separatorChar + META_DATA_FILENAME;
        try {
            ObjectInputStream ois = new ObjectInputStream(new FileInputStream(f));
            unpack_from_stream(ois);
            ois.close();
        } catch (ClassNotFoundException cnfe) {
            log.warn("Unable to read existing metadata file for blobstore: " + f + "\nDeleting it...");
            boolean b = new File(f).delete();
            log.warn("delete succeeded:" + b);
        }
    }

    public synchronized void pack() throws IOException {
        // write to a tmp file first, then rename -- we don't want to trash the if there is a disk full or disk error or something....
        String f = this.dir + File.separatorChar + META_DATA_FILENAME;
        String tmp = f + ".tmp";
        ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(tmp));
        pack_to_stream(oos);
        oos.close();

        File F = new File(f);
        if (F.exists()) {
            boolean b = F.delete();
            if (!b)
                log.warn("Failed to delete blobs metadata file");
        }

        boolean success = new File(tmp).renameTo(new File(f));
        if (!success)
            log.warn("Metadata rename failed... packing may be incomplete!");

        log.info("packed datastore: " + this);
    }

    /**
     * full filename for data
     */
    private String full_filename(Blob b) {
        return full_filename(b, b.filename);
    }

    /**
     * full filename for an arbitrary file associated with d
     */
    private String full_filename(Blob b, String fname) {
        int idx = index(b);

        if (idx < 0)
            return fname;
        else
            return idx + "." + fname;
    }

    /**
     * add a new piece of data with content copied from is.
     * if owner is not set on the data, we will set it to this data store's owner.
     * computes content hash and sets it.
     * will close the stream when done.
     * Performance critical!
     * returns # of bytes in inputstream
     */
    public long add(Blob blob, InputStream is) throws IOException {
        long nBytes = -1;
        /* we no longer assume that if 2 blobs have the same name and file size, their contents must be the same!
        synchronized (this) {
            if (this.contains(blob)) {
                log.info("Item is already present: " + blob);
                return nBytes;
            }

            add(blob);
            Util.ASSERT(this.contains(blob));
        }
        */
        // release the lock here because we dont want a long op like reading the object's stream
        // and storing the file to be serialized across threads.

        // copy the stream, if it throws IOException, then we cancel the item
        try {
            // this code is slightly tricky because blob equality (and therefore its membership in this blob store) depends on the hash , which we don't have yet.
            // so we first read the stream into a temp file. As we read the temp file, we get its checksum. We use the checksum to initialize the blob object completely
            // and check if it already exists in the blob store. if not, we assign it in the id_map etc. and then rename the temp file to the proper location in the blob store.

            DigestInputStream din = new DigestInputStream(is, MessageDigest.getInstance("SHA-1"));
            log.info(" adding file to blob store = " + blob.filename);
            Path tmpPath = Files.createTempFile(new File(System.getProperty("java.io.tmpdir")).toPath(), "epadd.", ".temp");
            File tmpFile = tmpPath.toFile();
            nBytes = Util.copy_stream_to_file(din, tmpFile.getAbsolutePath());

            // now set the blob size and digest
            blob.size = nBytes; // overwrite this -- earlier we had the part size stored in blob size
            // be careful -- add the blob only after its size and SHA-1 has been updated
            byte byteArray[] = din.getMessageDigest().digest();
            blob.setContentHash(byteArray);

            // ok, now blob is finally set up and can be compared
            // if the blob is already present, fetching the bytes was a waste, but there would no other way to know its checksum.
            // we'll delete the file that is in the tmp dir

            if (uniqueBlobs.contains(blob)) {
                tmpFile.delete();
                return nBytes;
            }

            // blob doesn't already exist, add it, and move it from the temp dir to its actual place in the blobstore

            add(blob);
            // move it from the temp file to the blobs dir. don't do this before add(blob), because full_filename will not be set up correctly until the blob object can be lookedup
            String destination = dir + File.separatorChar + full_filename(blob);
            Files.move (tmpPath, new File(destination).toPath());
        } catch (IOException ioe) {
            // we couldn't copy the stream to the data store, so undo everything
            Util.print_exception("IO Error copying blob to blobstore", ioe, log);
            remove(blob);
            Util.ASSERT(!this.contains(blob));
            throw ioe;
        } catch (NoSuchAlgorithmException nsae) {
            // we couldn't copy the stream to the data store, so undo everything
            remove(blob);
            Util.ASSERT(!this.contains(blob));
            throw new RuntimeException(nsae);
        }

        Util.ASSERT(this.contains(blob));

        // packing needs to be done more efficiently (batch mode or incremental)
        if ((uniqueBlobs.size() % 100) == 0)
            pack();

        return nBytes;
    }

    public void add(Blob b, URL u) {
        synchronized (this) {
            if (this.contains(b)) {
                log.info("Item is already present: " + b);
                return;
            }

            add(b);
            Util.ASSERT(this.contains(b));
        }
    }

    private synchronized void pack_to_stream(ObjectOutputStream oos) throws IOException
    {
        oos.writeObject(uniqueBlobs);
        oos.writeObject(id_map);
        oos.writeObject(views);
        oos.writeInt(next_data_id);
        oos.flush();
    }

    private synchronized void unpack_from_stream(ObjectInputStream ois) throws IOException, ClassNotFoundException
    {
        uniqueBlobs = (Set<Blob>) ois.readObject();
        id_map = (Map<Blob, Integer>) ois.readObject();
        views = (Map<Blob, Map<String,Object>>) ois.readObject();
        next_data_id = ois.readInt();
    }

    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append ("Data store with " + uniqueBlobs.size() + " unique blobs");
        //int count = 0;
        //for (Data d : unique_datas)
        //    sb.append (count++ + ". " + d + "\n");
        return sb.toString();
    }

    /** returns the index of the given data item in this store */
    int index(Blob b)
    {
        Integer i = id_map.get(b);
        if (i == null)
            return -1;
        else
            return i.intValue();
    }

    public synchronized boolean contains (Blob b) {
        return uniqueBlobs.contains(b);
    }

    /** return the view for data d with the given key */
    private synchronized boolean hasView(Blob b, String view)
    {
        Map<String,Object> map = views.get(b);
        if (map == null)
            return false;
        return map.get(view) != null;
    }

    private synchronized void add(Blob b)
    {
       //  Util.ASSERT (!this.contains(b));

        if (uniqueBlobs.contains(b))
            return;
        uniqueBlobs.add(b);
        id_map.put (b, next_data_id);
        views.put (b, new LinkedHashMap<String,Object>());
        next_data_id++;
    }

    /** remove a piece of data, has to be the last one added. */
    private synchronized void remove(Blob b)
    {
        Util.ASSERT (this.contains(b));
        uniqueBlobs.remove(b);
        id_map.remove(b); // leaves a hole in id_map, but that's ok
        views.remove (b);
        next_data_id--;
    }

    /** add o with the supplied key to the map of views for object d */
    private synchronized void addView(Blob b, String key, Object o)
    {
        Util.ASSERT (this.contains(b));
        views.get(b).put(key, o);
    }

    /** return the view for data d with the given key */
    private synchronized Object getView(Blob b, String view)
    {
        Map<String,Object> map = views.get(b);
        if (map == null)
            return null;
        return map.get(view);
    }
    /**
     * add a view to the primary data with the given key. the data is stored with the given filename in the store
     */
    private synchronized void addView(Blob primary_data, String filename, String view, InputStream is) throws IOException {
        // for file data store, the object stored for the View is the file name
        addView(primary_data, view, filename);
        Util.copy_stream_to_file(is, dir + File.separatorChar + full_filename(primary_data, filename));
    }

    public InputStream getInputStream(Blob b) throws IOException {
        URL u = urlMap.get(b);
        if (u == null)
            return new FileInputStream(dir + File.separatorChar + full_filename(b));
        else
            return u.openStream();
    }

    private String get_cache_URL(String filename) {
        String nullURL = "file://null";
        if (filename == null)
            return nullURL;
        try {
            // important: this can be tricky, esp. on windows (and has led to bugs), so rely on the platform's toURI.toURL()
            // e.g. see http://stackoverflow.com/questions/9942033/java-urlfile-doesnt-work-on-windows-xp
            return new File(dir + File.separator + filename).toURI().toURL().toString();
        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            Util.print_exception(e, log);
            log.warn("ERROR trying to get URL for filename: " + filename + "  under " + dir);
            return nullURL;
        }
    }

    public String get_URL(Blob b) {
        URL u = urlMap.get(b);
        if (u != null)
            return u.toString();
        else
            return get_cache_URL(full_filename(b));
    }

    public String getRelativeURL(Blob b) {
        URL u = urlMap.get(b);
        if (u != null)
            return u.toString();
        else
            return full_filename(b);
    }

    public String getViewURL(Blob b, String key) {
        String filename = (String) getView(b, key);
        if (filename == null)
            return null;
        else
            return get_cache_URL(full_filename(b, filename));
    }

    public byte[] getDataBytes(Blob b) throws IOException {
        URL u = urlMap.get(b);
        if (u == null) {
            String filename = dir + File.separatorChar + full_filename(b);
            return Util.getBytesFromFile(filename);
        } else {
            return Util.getBytesFromStream(u.openStream());
        }
    }

    public byte[] getViewData(Blob b, String key) throws IOException {
        String filename = (String) getView(b, key);
        return Util.getBytesFromFile(dir + File.separatorChar + full_filename(b, filename));
    }

    /**
     * generates thumbnail for the given image and adds it as a "tn" supplement
     */
    public void generate_thumbnail(Blob b) throws IOException {
        if (this.hasView(b, "tn")) {
            log.info("Already have thumbnail for blob " + b);
            return;
        }

        String filename = "tn." + b.filename;
        String tmp_filename = TMP_DIR + File.separatorChar + filename;
        String tnFilename = null;
        boolean noThumb = false;
        String MOGRIFY = "/opt/local/bin/mogrify";
        if (Util.is_image_filename(b.filename) && new File(MOGRIFY).exists()) {
            // mogrify will update tmp_filename in place, creating a thumbnail
            tnFilename = tmp_filename;
            createBlobCopy(b, tmp_filename);
            try {
                Util.run_command(new String[]{MOGRIFY, "-geometry", "160x120", tmp_filename});
            } catch (Exception e) {
                log.warn("mogrify failed: " + e.getMessage() + "\n" + Util.stackTrace(e));
                noThumb = true;
            }
        } else if (!NO_CONVERT_PDFS && Util.is_pdf_filename(b.filename)) {
//    	tnFilename = tmp_filename + ".jpg";
//    	// [0] converts only page 0
//    	// only works on mac, after port install imagemagick
//    	Util.run_command(new String[] {"/opt/local/bin/convert", tmp_filename + "[0]", tnFilename});
//    	Util.run_command(new String[] {"/opt/local/bin/mogrify", "-geometry", "160x120", tnFilename});
            try {
                // pdfToImage will create x1.png from x.pdf
                tnFilename = tmp_filename.substring(0, tmp_filename.length() - ".pdf".length()); // strip the ".pdf"
                tnFilename += "1.png";
                String[] args = new String[]{"-imageType", "png", "-startPage", "1", "-endPage", "1", tmp_filename};
                // disabling pdfbox because it interferes with tika parsers
               // org.apache.pdfbox.PDFToImage.main(args);
                log.info("Saving PDF thumbnail to " + tnFilename);
                filename = filename + ".png"; // make sure the suffix for the thumbnail is named with a .png suffix in the cache
            } catch (Throwable e) {
                // make sure to catch Throwable and not just Exception because the PDF can throw a
                // java.lang.NoClassDefFoundError: org/bouncycastle/jce/provider/BouncyCastleProvider for password protected PDFs
                log.warn("PDF to image got exception: " + e.getMessage() + "\n" + Util.stackTrace(e));
                tnFilename = null;
                noThumb = true;
            }
        } else {
            noThumb = true;
            // log.info ("No thumbnail for attachment file named: " + b.filename);
        }

        if (!noThumb) {
            // add thumbnail to data store
            if (tnFilename != null)
                this.addView(b, filename, "tn", new FileInputStream(tnFilename));

            log.info("Generating thumbnail for data with tn filename: " + tnFilename);

            // best effort to delete the intermediate files, dont worry too much if we fail.
            if (!new File(tmp_filename).delete())
                log.warn("REAL WARNING: Unable to delete file: " + tmp_filename);
            if (tnFilename != null && !(tmp_filename == tnFilename) && !new File(tnFilename).delete())
                log.warn("REAL WARNING: Unable to delete file: " + tnFilename);
        }
    }

    /* copy blob to filePath */
    public String createBlobCopy(Blob b, String filePath) throws IOException {
        // create a copy of the image first in filePath
        InputStream is = null;
        String url = this.get_URL(b);
        url = url.replace("%", "%25");

        try {
            URL u = new URL(url);
            if ("file".equalsIgnoreCase(u.getProtocol())) {
                File f = new File (dir + File.separator + full_filename(b));
                is = new FileInputStream (f);
            } else {
                is = new URL(url).openStream();
            }
        } catch (MalformedURLException e) {
            Util.report_exception_and_rethrow(e, log);
        }

        Util.copy_stream_to_file(is, filePath);
        return url;
    }

}
