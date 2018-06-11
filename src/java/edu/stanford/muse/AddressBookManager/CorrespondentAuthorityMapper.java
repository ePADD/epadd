package edu.stanford.muse.AddressBookManager;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import edu.stanford.muse.Config;
import edu.stanford.muse.AuthorityMapper.AuthorityMapper;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import static edu.stanford.muse.ie.FASTIndexer.FIELD_NAME_FAST_ID;
import static edu.stanford.muse.ie.variants.EntityBook.canonicalize;

/**
 * Created by chinmay on 18/10/17.
 */
public class CorrespondentAuthorityMapper extends AuthorityMapper implements   java.io.Serializable  {

    public static Log log					= LogFactory.getLog(CorrespondentAuthorityMapper.class);
    public static String CANDIDATE_AUTHORITIES_FILE_NAME = "CandidateCorrespondentAuthorities.csv";
    public static String CONFIRMED_AUTHORITIES_FILE_NAME= "ConfirmedCorrespondentAuthorities.csv";
    private final static long serialVersionUID = 1L;

    /* creates a mapper. first checks if it already exists in the archive dir. otherwise creates a new one, and initializes it for the archive (might take a while to generate candidates) */
    public static CorrespondentAuthorityMapper createCorrespondentAuthorityMapper (Archive archive) throws IOException, ParseException, ClassNotFoundException {

        String filename = archive.baseDir + File.separator + Config.AUTHORITIES_FILENAME;
        File authMapperFile = new File (filename);

        CorrespondentAuthorityMapper cmapper = new CorrespondentAuthorityMapper();
        try {
            if (authMapperFile.exists() && authMapperFile.canRead()) {
                cmapper = (CorrespondentAuthorityMapper) Util.readObjectFromFile(filename);
            }
        } catch (Exception e) {
            Util.print_exception ("Error reading authority mapper file: " + filename, e, log);
        }

        if (cmapper== null)
            cmapper= new CorrespondentAuthorityMapper();

        // get ready for queries
        cmapper.openFastIndex ();

        // compute candidates if we don't have them yet. may need some way to force recomputation in the future, even if already computed.
        if (cmapper.cnameToFastIdCandidates.isEmpty() || cmapper.cnameToCount.isEmpty()) {
            // this can take a while.... might need to make a progress bar available for large archives
            cmapper.setupCandidatesAndCounts(archive);
        }
        return cmapper;
    }

    /** this should be called during creation time, or any time the cnameToFastIdCandidates has to be recomputed */
    public void setupCandidatesAndCounts(Archive archive) throws IOException, ParseException {
        AddressBook ab = archive.getAddressBook();

        List<Contact> contacts = ab.allContacts();
        for (Contact c : contacts) {
            try {
                Set<String> names = c.getNames();
                if (Util.nullOrEmpty(names))
                    continue;

                String contactName = c.pickBestName();
                String cname = canonicalize(contactName);
                List<String> cnameTokens = Util.tokenize(cname);
                if (cnameTokens.size() < 2)
                    continue; // only match when 2 or more words are present in the name

                if (cnameToAuthority.get(cname) == null) {
                    for (String name : names) {
                        List<String> nameTokens = Util.tokenize(name);
                        if (nameTokens.size() < 2)
                            continue; // only match when 2 or more words are present in the name

                        List<Document> hits = lookupNameInFastIndex(name);
                        if (hits.size() > 20)
                            log.warn ("Warning: many (" + hits.size() + ") hits for authority name=" + name + " (associated with contact " + contactName + ")");
                        for (Document d : hits) {
                            Long fastId = Long.parseLong(d.get(FIELD_NAME_FAST_ID));
                            cnameToFastIdCandidates.put(cname, fastId);
                        }
                    }
                }
            } catch (Exception e) {
                Util.print_exception("Error parsing contact for authorities: " + c, e, log);
            }
        }

        List<Pair<Contact, Integer>> pairs = ab.sortedContactsAndCounts((Collection) archive.getAllDocs());
        for (Pair<Contact, Integer> p : pairs) {
            Contact c = p.getFirst();
            String name = c.pickBestName();
            String cname = canonicalize(name);
            if (Util.nullOrEmpty(cname))
                continue;

            cnameToCount.put(cname, p.getSecond());
        }
    }
    /** small class meant to convey temp. results to the frontend. Not serialized. */
    public static class AuthorityInfo {
        public boolean isConfirmed;
        public int nMessages;
        public String name, tooltip, url, errorMessage;
        public AuthorityMapper.AuthorityRecord confirmedAuthority;
        public List<AuthorityMapper.AuthorityRecord> candidates;
    }

    /** returns an authorityInfo object representing info needed by the front-end. Use only for rendering the authorities table. */
    public AuthorityInfo getCorrespondentAuthorityInfo (String archiveID, AddressBook ab, String name) throws IOException, ParseException {
        String cname = canonicalize (name);
        AuthorityInfo result = new AuthorityInfo();
        result.isConfirmed = false;
        result.name = name;

        //////Following snippet was added to handle the case when cnameToCount map was not initialized (from createCorrespondentAuthorityMapper method) because
        //it was read from two csv files (candidateCorrespondentAuthorities.csv and confirmedCorrespondentAuthorities.csv).
        if(cnameToCount==null || cnameToCount.size()==0){
            Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);

            List<Pair<Contact, Integer>> pairs = ab.sortedContactsAndCounts((Collection) archive.getAllDocs());
            for (Pair<Contact, Integer> p : pairs) {
                Contact c = p.getFirst();
                String lname = c.pickBestName();
                String lcname = canonicalize(lname);
                if (Util.nullOrEmpty(lcname))
                    continue;
                cnameToCount.put(lcname, p.getSecond());
            }
        }
        /////////////////////////////////////////////////////////////
        Integer nMessages = (cnameToCount != null) ? cnameToCount.get(cname) : null;
        result.nMessages = (nMessages == null) ? 0 : nMessages;

        String tooltip = "";
        Collection<Contact> contacts = ab.lookupByName(name);
        if (contacts != null)
            for (Contact contact: contacts) {
                tooltip += contact.toTooltip();
            }
        else {
            result.errorMessage = "Name not in address book: " + name;
            return result;
        }

        result.url = "browse?archiveID=" + archiveID + "&adv-search=on&correspondentTo=on&correspondentFrom=on&correspondentCc=on&correspondentBcc=on&correspondent=" + name;
        result.tooltip = tooltip;
        result.nMessages = nMessages==null?0:nMessages;

        AuthorityMapper.AuthorityRecord authRecord = cnameToAuthority.get(cname);
        if (authRecord != null) {
            result.isConfirmed = true;
            result.confirmedAuthority = authRecord;
        }

        List<AuthorityMapper.AuthorityRecord> candidates = new ArrayList<>();
        Collection<Long> fastIds = cnameToFastIdCandidates.get(cname);
       if (fastIds != null)
            for (Long id : fastIds)
                candidates.add (getAuthRecordForFASTId(id));

        result.candidates = candidates;
        return result;
    }

    /*
    Code for writing correspondent authority mapper in a human readable format
    Two files will be stored corresponding to an authority mapper. Going forward the same will hold for EntityAuthorityMapper as well.
    File names will be;
    1. confirmedAuthorityRecords.csv, and
    2. candidateAuthorityRecords.csv
    confirmedAuthorityRecords.csv will be same as the one exported by 'getAuthoritiesAsCSV' method in AuthorityMapper.java
    candidateAuthorityRecords.csv will be csv version of cnameToFastIdCandidates multimap data structure.
     */
    public void writeObjectToStream(String dirname){
        // writing cnameToFastIdCandidates multimap to csv format
        try{
            FileWriter fw = new FileWriter(dirname+ File.separator+CANDIDATE_AUTHORITIES_FILE_NAME);
            CSVWriter csvwriter = new CSVWriter(fw, ',', '"', '\n');

            // write the header line: "DocID,LabelID ".
            List<String> line = new ArrayList<>();
            line.add ("Canonical Name");
            line.add ("Candidate FastID");
            csvwriter.writeNext(line.toArray(new String[line.size()]));

            // write the records
            for(String cname: cnameToFastIdCandidates.keySet()){
                for(Long fastid: cnameToFastIdCandidates.get(cname)) {
                    line = new ArrayList<>();
                    line.add(cname);
                    line.add(Long.toString(fastid));
                    csvwriter.writeNext(line.toArray(new String[line.size()]));
                }
            }
            csvwriter.close();
            fw.close();
        } catch (IOException e) {
            log.warn("Unable to write fastIDcandidates of correspondents in csv file");
            return;
        }

        //writing confirmed correspondent authorities to a csv file.Here we take advantage of an already implemented method (getAuthoritiesAsCSV) that is
        //used to export the authorities in a csv file format.
        String pathToFile = dirname + File.separator + CONFIRMED_AUTHORITIES_FILE_NAME;

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(pathToFile, "UTF-8");
            String csv = getAuthoritiesAsCSV();
            pw.println(csv);
            pw.close();
        } catch(Exception e){
            Util.print_exception ("Error exporting authorities", e, log);
            e.printStackTrace();
        }
    }

    /*
    Reading authority records from csv files and setting up this object.
     */
    public static CorrespondentAuthorityMapper readObjectFromStream(String dirname) {
        //read candidate authorities from csv file and fill in cnameToFastIdCandidates multimap
        CorrespondentAuthorityMapper cauthorityMapper = new CorrespondentAuthorityMapper();
        try{
            FileReader fr = new FileReader(dirname+ File.separator+CANDIDATE_AUTHORITIES_FILE_NAME);
            CSVReader csvreader = new CSVReader(fr, ',', '"', '\n');

            // read line by line, except the first line which is header
            String[] record = null;
            record = csvreader.readNext();//skip the first line.
            while ((record = csvreader.readNext()) != null) {

                long fastid = 0;
                try{
                    fastid = Long.parseLong(record[1].trim());
                }catch(NumberFormatException e){
                    fastid=-1;
                }
                if(fastid==-1)//means fastid was not parseable from string.. skip it
                    continue;
                else
                    cauthorityMapper.cnameToFastIdCandidates.put(record[0],fastid);
            }
            csvreader.close();
            fr.close();
        } catch (IOException e) {
            log.warn("Unable to read candidateAuthorities from csv file");

        }

        //read confirmed authorities from csv file and fill in cnameToAuthority map
        try{
            FileReader fr = new FileReader(dirname+ File.separator+CONFIRMED_AUTHORITIES_FILE_NAME);
            CSVReader csvreader = new CSVReader(fr, ',', '"', '\n');

            // read line by line, except the first line which is header
            String[] record = null;
            record = csvreader.readNext();//skip the first line.
            while ((record = csvreader.readNext()) != null) {
                if(record.length<=1)
                    continue;//to handle the case when an empty line is present in the csv file.
                AuthorityRecord ar = new AuthorityRecord();
                String canonname =  record[0].trim();
                ar.preferredLabel = record[1];
                try {
                    ar.fastId = Long.parseLong(record[2].trim());
                    ar.viafId = record[3].trim();
                    ar.wikipediaId = record[4].trim();
                    ar.lcshId = record[5].trim();
                    ar.lcnafId = record[6].trim();
                    ar.localId = record[7].trim();
                    ar.extent = record[8].trim();
                    String ismanual = record[9].trim();
                    if (ismanual.toLowerCase().equals("y"))
                        ar.isManuallyAssigned =true;
                    else if(ismanual.toLowerCase().equals("n"))
                        ar.isManuallyAssigned = false;
                    else {
                        log.warn("Invalid character found for isManuallyAssigned field in the correspondent confirmed authority csv file");
                        continue;
                    }
                } catch (NumberFormatException e) {
                    log.warn("Invalid integer found in the correspondent confirmed authority csv file");
                    continue;//because some string to number formatting failed.
                }
                cauthorityMapper.cnameToAuthority.put(canonname,ar);
            }
            csvreader.close();
            fr.close();
        } catch (IOException e) {
            log.warn("Unable to read confirmed authorities from csv file");

        }
        //fill in cnameToCount map (specific to the fact that it is a correspondent authority mapper


        return  cauthorityMapper;
    }


}
