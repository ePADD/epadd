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

import au.com.bytecode.opencsv.CSVReader;
import com.google.common.collect.Multimap;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
//import org.apache.commons.logging.Log;
//import org.apache.commons.logging.LogFactory;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import org.xml.sax.ContentHandler;

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

    public final Set<Blob> uniqueBlobs = new LinkedHashSet<>();

    private final transient Map<Blob,Integer> dupBlobCount = new LinkedHashMap<>();//map to maintain the count of duplicate blobs so that it can be reported in the data report
    // mapping of each data item to a data id
    private final Map<Blob, Integer> id_map = new LinkedHashMap<>();
    private final Map<Blob, URL> urlMap = new LinkedHashMap<>(); // -- seems this is not really used
    // data id's are just assigned sequentially starting from 0
    private int next_data_id = 0;

    // mapping of each data to its views
    private final Map<Blob, Map<String,Object>> views = new LinkedHashMap<>();
    //map of original file name and (cleanedupname, normalizedname) generated from Amatica normalization
    //transient because this can always be built from the normalziation info file (csv) present in session directory of the archive.
    private transient  Map<String,Pair<String,String>> normalizationMap = new LinkedHashMap<>();
    private transient  HashMap<String,String> executablePathMap = new LinkedHashMap();

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

    private static final Logger log =  LogManager.getLogger(BlobStore.class);
    private final static long serialVersionUID = 1L;

    private String dir; // base dir where this data store keeps it files
    //private static final String META_DATA_FILENAME = ".MetaData"; // filename where meta data is kept-- NO Longer used because the data stored in this file is also stored in default.archive.v2 serialized file.

    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");

    //remove final later if we want this to be user configurable
//pdfbox sometimes crashes the VM mysteriously on Macs
    private final static boolean NO_CONVERT_PDFS = System.getProperty("pdf.thumbnails") == null;

    /**
     * @param dir directory where this store keeps its data
     */
    public BlobStore(String dir) {
        log.info("Opening file repository in " + dir);
        this.dir = dir;
        File dir_file = new File(dir);
        dir_file.getParentFile().mkdir();//just to make sure that data folder is present
        if (!dir_file.exists())
            dir_file.mkdirs();
        else {
            // could also lock the dir to make sure no other data store object uses this dir inadvertently
          /*  File f = new File(this.dir + File.separatorChar +  META_DATA_FILENAME);
            if (f.exists()) {
                unpack();
                log.info("File repository in " + dir + " has " + uniqueBlobs.size() + " entries");
            }*/
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
    public BlobStore createCopy(String path, Collection<Blob> blobs) {
        BlobStore fbs = new BlobStore(path); // no owner field
        for (Blob b : blobs) {
            try {
                // path can be something like E:\/Users/xyz
                // get_URL_Normalized(b) can return something like file:E:\/Users\hangal\ePADD archive of hangal@gmail.com\blobs
                String urlString = get_URL_Normalized(b); // .replaceAll("\\\\", "/");
                // urlString can be something like file:E://Users/hangal/ePADD archive of hangal@gmail.com/blobs
                // very importantly, the E:/Users/... needs to be made E://
                //	    urlString = urlString.replaceAll(":/", "://"); //
                URL url = new URL(urlString); // replaceAll expects a regex, so it needs to get a \\, so we need to write \\\\ !
                fbs.add(b, url.openStream()); // get_URL_Normalized returns things with \ sometimes
            /* this code is causing trouble, so drop these views. openstreams are not serializable, dunno why they are kept with the views
			for (String view: getViews(b))
				fbs.addView(b, view, new URL(getViewURL(b, view)).openStream());
				*/
            } catch (Exception e) {
                Util.print_exception(e, log);
            }
        }
        //fbs.pack();
        return fbs;
    }

    /*
    This method returns number of renamed files obtained after normalization (from archivmatica)
     */
    public int getCleanedFilesCount(){
        int count=0;
        if(normalizationMap==null)
            return count;
        for(String s: normalizationMap.keySet()){
            if(!s.equals(normalizationMap.get(s).first))//if original name and cleanedup name not same then the file was renamed.
                count++;
        }
        return count;
    }

    /*
    This method returns number of normalized files obtained after normalization (from archivmatica)
     */

    public int getNormalizedFilesCount(){
        int count=0;
        if(normalizationMap==null)
            return count;
        for(String s: normalizationMap.keySet()){
            if(!normalizationMap.get(s).first.equals(normalizationMap.get(s).second))//if cleanedup name and normalized name not same then the file was normalized.
                count++;
        }
        return count;
    }




    /**
     * directory where this store keeps it data
     */
    public String getDir() {
        return dir;
    }

    /*private synchronized void unpack() throws IOException {
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
    }*/

    /*public synchronized void pack() throws IOException {
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
    }*/

    /**
     * full filename for data
     */
    public String full_filename_normalized(Blob b) {
        return full_filename_normalized(b, true);
    }

    /**
     * full filename for data
     */
    public String full_filename_normalized(Blob b, boolean useIndex) {
        return full_filename_normalized(b, b.filename,useIndex);
    }

    public boolean isCleaned(Blob b){
        //if cleanedup.notequals(normalized) then normalization happened. Download original file (cleanedupfileURL)
        //origina.notequals(normalized) then only name cleanup happened.(originalfilename)
        //so the attributes are either only originalfilename or cleanedupfileURL or both.
        String cleanedupname = full_filename_cleanedup(b);
          boolean isCleanedName = !cleanedupname.equals(full_filename_original(b));
        return isCleanedName;

    }

    public boolean isNormalized(Blob b){
        //if cleanedup.notequals(normalized) then normalization happened. Download original file (cleanedupfileURL)
        //origina.notequals(normalized) then only name cleanup happened.(originalfilename)
        //so the attributes are either only originalfilename or cleanedupfileURL or both.
        String cleanedupname = full_filename_cleanedup(b);
        String normalizedname=full_filename_normalized(b);
         boolean isNormalized = !cleanedupname.equals(normalizedname);
         return isNormalized;

    }


    /**
     * full filename for an arbitrary file associated with d
     */
    private String full_filename_normalized(Blob b, String fname,boolean useIndex) {
        int idx = index(b);
        String actualname = (idx < 0)? fname: idx + "." + fname;
        String modifiedname = null;
        //now check if normalizationinfo map has a mapping for this file or not. If it has then return the cleanedup/normalized name instead of the
        //actual name of the blob.
        if(normalizationMap==null || !normalizationMap.containsKey(actualname)){
            modifiedname =  actualname;
        }else{
            modifiedname = new String(normalizationMap.get(actualname).second);//
        }
        //if idx>=0 and useIndex is false then remove idx from the file name and return it.
        if(idx>=0 && !useIndex){
            modifiedname = modifiedname.substring(Integer.toString(idx).length()+1);//remove the initial part equal to the length of idx + 1 character (for .)
        }
        return modifiedname;
        //else simply return the modifiedname;

    }

    public String full_filename_cleanedup(Blob b) {
        return full_filename_cleanedup(b, true);
    }

    private String full_filename_cleanedup(Blob b, boolean useIndex) {
        return full_filename_cleanedup(b, b.filename,useIndex);
    }
    /**
     * full filename for an arbitrary file associated with b.
     */
    private String full_filename_cleanedup(Blob b, String fname, boolean useIndex) {

        int idx = index(b);
        String actualname = (idx < 0)? fname: idx + "." + fname;
        String modifiedname;
        //now check if normalizationinfo map has a mapping for this file or not. If it has then return the cleanedup/normalized name instead of the
        //actual name of the blob.
        if(normalizationMap==null || !normalizationMap.containsKey(actualname)){
            modifiedname = actualname;
        }else{
            modifiedname = new String(normalizationMap.get(actualname).first);//second denote if it was cleaned/normalized or both?
        }
        //if idx>=0 and useIndex is false then remove idx from the file name and return it.
        if(idx>=0 && !useIndex){
            modifiedname = modifiedname.substring(Integer.toString(idx).length()+1);//remove the initial part equal to the length of idx + 1 character (for .)
        }
        return modifiedname;
        //else simply return the modifiedname;
    }

    public String full_filename_original(Blob b) {
        return full_filename_original(b, true);
    }

    public String full_filename_original(Blob b, boolean useIndex) {
        return full_filename_original(b, b.filename,useIndex );
    }

    private String full_filename_original(Blob b, String fname, boolean useIndex) {
        if(useIndex) {
            int idx = index(b);
            String actualname = (idx < 0) ? fname : idx + "." + fname;
            return actualname;
        }else
            return fname;
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
            log.info(" adding file to blob store = " + get_URL_Normalized(blob));
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
                //blob already exists.. record it in the data report.
                int curcount = dupBlobCount.getOrDefault(blob,0);
                dupBlobCount.put(blob,curcount+1);
                return nBytes;
            }

            // blob doesn't already exist, add it, and move it from the temp dir to its actual place in the blobstore

            add(blob);
            // move it from the temp file to the blobs dir. don't do this before add(blob), because full_filename_normalized will not be set up correctly until the blob object can be lookedup
            String destination = dir + File.separatorChar + full_filename_normalized(blob);
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
      /*  if ((uniqueBlobs.size() % 100) == 0)
            pack();*/

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

    /*private synchronized void pack_to_stream(ObjectOutputStream oos) throws IOException
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
    }*/

    public String toString()
    {
        //int count = 0;
        //for (Data d : unique_datas)
        //    sb.append (count++ + ". " + d + "\n");
        return ("Data store with " + uniqueBlobs.size() + " unique blobs");
    }

    /** returns the index of the given data item in this store */
    public int index(Blob b)
    {
        Integer i = id_map.get(b);
        if (i == null)
            return -1;
        else
            return i;
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
        views.put (b, new LinkedHashMap<>());
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
        Util.copy_stream_to_file(is, dir + File.separatorChar + filename);
    }

    private InputStream getInputStream(Blob b) throws IOException {
        URL u = urlMap.get(b);
        if (u == null)
            return new FileInputStream(dir + File.separatorChar + full_filename_normalized(b));
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

    public String get_URL_Normalized(Blob b) {
        URL u = urlMap.get(b);
        if (u != null)
            return u.toString();
        else
            return get_cache_URL(full_filename_normalized(b));
    }

    public String get_URL_Cleanedup(Blob b) {
        URL u = urlMap.get(b);
        if (u != null)
            return u.toString();
        else
            return get_cache_URL(full_filename_cleanedup(b));
    }


    public String getRelativeURL(Blob b) {
        URL u = urlMap.get(b);
        if (u != null)
            return u.toString();
        else
            return full_filename_normalized(b);
    }

    public String getViewURL(Blob b, String key) {
        String filename = (String) getView(b, key);
        if (filename == null)
            return null;
        else
            return get_cache_URL(filename);//full_filename_normalized(b));
    }

    public byte[] getDataBytes(Blob b) throws IOException {
        URL u = urlMap.get(b);
        if (u == null) {
            String filename = dir + File.separatorChar + full_filename_normalized(b);
            return Util.getBytesFromFile(filename);
        } else {
            return Util.getBytesFromStream(u.openStream());
        }
    }

    public byte[] getViewData(Blob b, String key) throws IOException {
        String filename = (String) getView(b, key);
        return Util.getBytesFromFile(dir + File.separatorChar +  full_filename_normalized(b));
    }


    public boolean is_image(Blob blob)
    {
        return Util.is_image_filename (get_URL_Normalized(blob));
    }


    public Pair<String, String> getContent(Blob blob)
    {
        Metadata metadata = new Metadata();
        StringBuilder metadataBuffer = new StringBuilder();
        ContentHandler handler = new BodyContentHandler(-1); // no character limit
        InputStream stream = null;
        boolean failed = false;

        Parser parser = new AutoDetectParser();
        ParseContext context = new ParseContext();
        try {
            stream = getInputStream(blob);

            try {
                // skip mp3 files, tika has trouble with it and hangs
                // skip zip files too. tika has trouble with some zip formats as well.
                String fname = get_URL_Normalized(blob);
                if (!Util.nullOrEmpty(fname) && !fname.toLowerCase().endsWith(".mp3") &&  !fname.toLowerCase().endsWith(".zip"))
                    parser.parse(stream, handler, metadata, context);

                String[] names = metadata.names();
                //Arrays.sort(names);
                for (String name : names) {
                    // some metadata tags are problematic and result in large hex strings... ignore them. (caused memory problems with Henry's archive)
                    // https://github.com/openplanets/SPRUCE/blob/master/TikaFileIdentifier/python/config.py
                    // we've seen at least unknown tags: (0x8649) (0x935c) (0x02bc)... better to drop them all
                    String lname = name.toLowerCase();
                    if (lname.startsWith("unknown tag") || lname.startsWith("intel color profile"))
                    {
                        log.info ("Warning: dropping metadata tag: " + name + " for blob: " + fname);
                        continue;
                    }
                    metadataBuffer.append(": ");
                    metadataBuffer.append(metadata.get(name));
                    metadataBuffer.append("\n");
                }
            } catch (Exception e) {
                log.warn("Tika is unable to extract content of blob " + this + ":" + Util.stackTrace(e));
                // often happens for psd files, known tika issue:
                // http://mail-archives.apache.org/mod_mbox/tika-dev/201210.mbox/%3Calpine.DEB.2.00.1210111525530.7309@urchin.earth.li%3E
                failed = true;
            } finally {
                try { stream.close(); } catch (Exception e) { failed = true; }
            }

        } catch (IOException e) {
            log.warn("Unable to access content of blob " + get_URL_Normalized(blob) + ":" + Util.stackTrace(e));
            failed = true;
        }

        if (failed){
            blob.processedSuccessfully = false;
            return null;
        }
        else{
            blob.processedSuccessfully = true;
            return new Pair<>(metadataBuffer.toString(), handler.toString());
        }


    }



    /**
     * generates thumbnail for the given image and adds it as a "tn" supplement
     * On successful generation it returns true.
     */
    public boolean generate_thumbnail(Blob b) throws IOException {
        if (this.hasView(b, "tn")) {
            log.info("Already have thumbnail for blob " + b);
            return false;
        }

        String filename = full_filename_normalized(b,false);

        String tmp_filename = TMP_DIR + File.separatorChar + filename;
        String pdfFilePath=null;
        String tnFilename = null;
        boolean noThumb = false;
        String libreoffice = this.executablePathMap.get("soffice");//"/Applications/LibreOffice.app/Contents/MacOS/soffice";
        String convert = this.executablePathMap.get("convert");//"/usr/local/bin/convert";
       // if(either is null return false)
        if(Util.nullOrEmpty(libreoffice) || Util.nullOrEmpty(convert) || !new File(libreoffice).exists() || !new File(convert).exists())
            return false;
        //For now we are only generating thumnbnails for non image attachments using libreoffice writer.
        if ( Util.is_doc_filename(filename) && new File(libreoffice).exists()) { //exclude pdf files too..
            //First generate pdf file using libreoffice writer.
  //          tnFilename = tmp_filename;
            createBlobCopy(b, tmp_filename); //create a copy of the blob  in temp directory.
            try {
                if(!Util.is_pdf_filename(filename)) {
                    //Following command will generate the pdf file with the same name (only extension changed to pdf).
                    String[] pdfcmd = new String[]{libreoffice, "--headless", "--convert-to", "pdf", "--outdir", TMP_DIR+File.separatorChar, tmp_filename};
                   Util.run_command(pdfcmd);
                   //check if pdf file was created or not.
                    if(!new File(tmp_filename).exists()){
                        log.warn("Could not generate pdf. Command "+pdfcmd.toString()+" failed\n");
                        noThumb=true;
                    }
                }
                //Split into basefile name and extension.
                Pair<String,String> nameExtension = Util.splitIntoFileBaseAndExtension(filename);
                pdfFilePath = TMP_DIR + File.separator + nameExtension.first+".pdf"; //This file must have been generated by libreoffice command above.
                String outputImagePath = TMP_DIR + File.separator + nameExtension.first+".png";//This is the output file from the convert command below.
                //now use pdfToImage to convert this pdf file to image file and assign the generated file name to tnFilename variable.
                //Using some density to make image readable. desnsity 100 didn't give good image resolution. More desnity also mean more time to create image.
                String[] convertcmd = new String[]{convert,"-density", "100", pdfFilePath+"[0]", outputImagePath};
                Util.run_command(new String[]{convert,"-density", "100", pdfFilePath+"[0]", outputImagePath}); //[0] is to convert only first page of the pdf.
                //outputImagePath will be the path of thumbnail.
                tnFilename = outputImagePath;
                if(!new File(tnFilename).exists())
                {
                    log.warn("Could not generated thumbnail. Command "+convertcmd.toString()+" failed\n");
                    //means something wrong happened during conversion. Can not generate thumbnail.
                    noThumb=true;
                }
            } catch (Exception e) {
                log.warn("libreoffice failed: " + e.getMessage() + "\n" + Util.stackTrace(e));
                noThumb = true;
            }
        }  else {
            noThumb = true;
            // log.info ("No thumbnail for attachment file named: " + b.filename);
        }

        if (!noThumb) {
            // add thumbnail to data store
            if (tnFilename != null) {
                //Name that should be used to store the thumbnail. Make it same as the basename of the file with 'png' suffix and 'tn' prefix.
                String tnFnameInStore = "tn"+Util.splitIntoFileBaseAndExtension(full_filename_original(b)).first+".png";
                this.addView(b, tnFnameInStore, "tn", new FileInputStream(tnFilename));
                log.info("Generating thumbnail for data with tn filename: " + tnFilename);
            }else{
                log.info("No thumbnail will be generated for "+filename+"\n");
            }

            // best effort to delete the intermediate files, dont worry too much if we fail.
            if (!new File(tmp_filename).delete())
                log.warn("REAL WARNING: Unable to delete file: " + tmp_filename); //tmp_filename is the original file copied to temp directory.
            if (pdfFilePath!=null && !new File(pdfFilePath).delete())
                log.warn("REAL WARNING: Unable to delete intermediate pdf file: " + pdfFilePath);
            return true;
        }
        else
            return false;
    }

    /* copy blob to filePath */
    public String createBlobCopy(Blob b, String filePath) throws IOException {
        // create a copy of the image first in filePath
        InputStream is = null;
        String url = this.get_URL_Normalized(b);
        url = url.replace("%", "%25");

        try {
            URL u = new URL(url);
            if ("file".equalsIgnoreCase(u.getProtocol())) {
                File f = new File (dir + File.separator +  full_filename_normalized(b));
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

    public void setNormalizationMap(String blobNormalizationMapPath) {

        if(normalizationMap==null)
            normalizationMap=new LinkedHashMap<>();
        //read the normalization info file and put it in a map
        try{
            FileReader fr = new FileReader(blobNormalizationMapPath);
            CSVReader csvreader = new CSVReader(fr, ',', '"');

            // read line by line, except the first line which is header
            String[] record = null;
            record = csvreader.readNext();//skip the first line.
            while ((record = csvreader.readNext()) != null) {
                String filename = record[0];
                String cleanedupname = record[1];
                String normalizedname = record[2];
                this.normalizationMap.put(filename,new Pair<String,String>(cleanedupname,normalizedname));
            }

            csvreader.close();
            fr.close();
        } catch (IOException e) {
            log.warn("Unable to read docid to label map from csv file");

        }

    }

    public Map<String,Pair<String,String>> getNormalizationMap() {
        return normalizationMap;
    }

    public void setNormalizationMap(Map<String, Pair<String, String>> normalizationMap) {
        this.normalizationMap=normalizationMap;
    }

    public boolean isNormalized() {
        return normalizationMap!=null && normalizationMap.size()>0;
    }

    public Map<Blob, Integer> getDupBlobCount() {
        if(dupBlobCount==null)
            return new LinkedHashMap<>();
        return dupBlobCount;
    }

    public void setExecutablePath(String progname, String progpath) {
        if(this.executablePathMap==null)
            this.executablePathMap=new LinkedHashMap<>();
        this.executablePathMap.put(progname,progpath);
    }

    /*public void writeNormalizationMap(String filename){
        try{
            FileWriter fw = new FileWriter(filename);
            CSVWriter csvWriter = new CSVWriter(fw, ',', '"');
            List<String> line = new ArrayList<>();
            line.add ("OriginalFile");
            line.add ("CleanedupName");
            line.add ("NormalizedName");

            csvWriter.writeNext(line.toArray(new String[line.size()]));

            for(String orig: normalizationMap.keySet()){
                String cleanedupname = normalizationMap.get(orig).first;
                String normalizedname = normalizationMap.get(orig).second;
                line = new ArrayList<>();
                line.add (orig);
                line.add (cleanedupname);
                line.add (normalizedname);
                csvWriter.writeNext(line.toArray(new String[line.size()]));
            }
        csvWriter.close();
        fw.close();
        } catch (IOException e) {
            log.warn("Unable to writer normalization info file");

        }

    }
*/
}
