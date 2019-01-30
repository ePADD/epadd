package edu.stanford.muse.ie;


/**
 * Created by vihari on 26/12/15.
 */
interface Hierarchy {

    int getNumLevels();
    String getName(int level);
    String getValue(int level, Object o);
}