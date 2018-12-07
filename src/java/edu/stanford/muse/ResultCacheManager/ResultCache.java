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

    Map<String,Map<String,JSONArray>> lexiconCount = new LinkedHashMap<>();



}
