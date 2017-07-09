package edu.stanford.muse.ie;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


/**
 * Created by vihari on 26/12/15.
 */
public interface Hierarchy {

    int getNumLevels();
    String getName(int level);
    String getValue(int level, Object o);
}