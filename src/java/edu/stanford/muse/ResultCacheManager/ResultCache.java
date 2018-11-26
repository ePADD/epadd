package edu.stanford.muse.ResultCacheManager;

import edu.stanford.muse.AddressBookManager.AddressBook;
import edu.stanford.muse.ie.variants.EntityBook;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.index.Lexicon;
import edu.stanford.muse.ner.Entity;
import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Util;
import org.json.JSONArray;

import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

//This class stores the result of important getter methods. Such memoization helps in retrieving the results of getter
//faster. Especially in an application like ePADD where majority of the operations include getting the data (instead of setting it).
//However, one important thing needs to be taken care of in the design of such a cache manager. That is invalidation of the getter
//result. Therfore for each cached result we also maintain a list of methods which will invalidate the cache and hence the getter result
//needs to be updated again.
public class ResultCache {

    Map<String,JSONArray> correspondentCount = new LinkedHashMap<>();
    Map<String,Map<String,JSONArray>> lexiconCount = new LinkedHashMap<>();

    /*
    After the modification of AddressBook. Which method gets called?

        After ArchiveReaderWriter::saveAddressBook is called. [after user clicks on save]
        After ArchiveReaderWriter::readAddressBook is called. [after user initializes the addressbook]
     */
    public void cacheCorrespondentListing(String archiveID){
        //get archive
        Archive archive =ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        //remove already stored if any..
        correspondentCount.remove(archiveID);
        correspondentCount.put(archiveID, AddressBook.getCountsAsJson((Collection)archive.getAllDocs(),false,archiveID));
    }
    /*
    Called from inside AddressBook::getCountsAsJSON. No need to recalculate the json array hence the need of a different API.
     */
    public void cacheCorrespondentListing(String archiveID,JSONArray result){
        //get archive
        Archive archive =ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        correspondentCount.put(archiveID, result);
    }

    public JSONArray getEntitiesInfoJSON( String archiveID, Short requestedtype){
        Archive archive =ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        EntityBook ebook = archive.getEntityBookManager().getEntityBookForType(requestedtype);
        JSONArray resultArray = ebook.getInfoAsJSON();
        return resultArray;
    }


    public JSONArray getCorrespondentListing(String archiveID){
        return correspondentCount.get(archiveID);
    }

    /*
    Called after initialization of an archive.
     */
    public void cacheLexiconListing(String archiveID){
        //get archive
        Archive archive =ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        for(String lexicon: archive.getAvailableLexicons()){
            cacheLexiconListing(lexicon,archiveID);
        }
    }
    /*
    Called when a lexicon is saved or a new one is uploaded
     */
    public void cacheLexiconListing(String lexicon, String archiveID){
        //get archive
        Archive archive =ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        Map<String,JSONArray> tmp = lexiconCount.get(archiveID);
        if(tmp==null){
            tmp = new LinkedHashMap<>();

        }
        //remove  already stored info from tmp if any.
        tmp.remove(lexicon);
        tmp.put(lexicon, Lexicon.getCountsAsJSON(lexicon,archive,false));
        lexiconCount.put(archiveID,tmp);
    }
    /*
    Called when a lexicon is removed
     */
    public void removeCacheLexiconListing(String lexicon, String archiveID){
        //get archive
        Archive archive =ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        Map<String,JSONArray> tmp = lexiconCount.get(archiveID);
        if(tmp!=null){
            tmp.remove(lexicon);

        }

    }
    /*
    Called inside Lexicon::getCountsAsJSON method. The result is already created so no need to recalculate it. Therefore a different API.
     */
    public void cacheLexiconListing(String lexicon, String archiveID,JSONArray result){
        //get archive
        Archive archive =ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        Map<String,JSONArray> tmp = lexiconCount.get(archiveID);
        if(tmp==null){
            tmp = new LinkedHashMap<>();

        }
        tmp.put(lexicon, result);
        lexiconCount.put(archiveID,tmp);
    }
    public JSONArray getLexiconListing(String lexicon, String archiveID){
        Map<String,JSONArray> tmp = lexiconCount.get(archiveID);
        if(tmp==null)
            return null;
        return tmp.get(lexicon);
    }

    /*public void cacheEntitiesListing(String archiveID) {
        //for now just do computation of entityTypeToDocMap used in Advanced search. Later we will use this to get information about entity listing as well.
        Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
        //archive.computeEntityTypeToDocMap();

    }*/
}
