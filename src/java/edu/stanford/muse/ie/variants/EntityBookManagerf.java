package edu.stanford.muse.ie.variants;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

/*
Since v7 we keep one entitybook per entity. Earlier we had one entity book to keep the information about all entity types.
With this change, we now introduce this class that contains information about different entitybooks, one per entity type.
 */
public class EntityBookManager {
    //we don't need to serialize this class as the date here will be made persistent in form of human readable files.
    public static long serialVersionUID = 1L;
    public static Log log = LogFactory.getLog(EntityBookManager.class);

    //variable to hold mapping of different entity books, one per entity type.
    private Map<Short,EntityBook> mTypeToEntityBook = new LinkedHashMap<>();

    /*
    Method to read different entity books from files and fill this object. This object is then returned to the caller.
     */
    public static EntityBookManager readObjectFromStream(BufferedReader in) throws IOException {



    }

    /*
    Method for saving this
     */


}
