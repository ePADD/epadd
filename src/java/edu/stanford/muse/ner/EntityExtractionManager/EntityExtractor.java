package edu.stanford.muse.ner.EntityExtractionManager;

import org.jpl7.Atom;
import org.jpl7.JPL;
import org.jpl7.Query;

import java.io.File;
import java.util.Set;

public abstract class EntityExtractor {

    abstract  Set<String> getLogicRuleFiles();

    void initPrologAndLoadRules_Facts(DocFacts input) {
        boolean res = JPL.init();
        if (!res) {
            //means JPL is already initialized. remove loaded files

            unload_prolog_files(input);
        }
        //load fact files..
        Set<String> getFactsPLFileNames = input.getFactsPLFileNames();
        Query consultQueryFacts = null;
        for (String fname : getFactsPLFileNames) {
            consultQueryFacts = new Query("consult", new Atom(fname));
            consultQueryFacts.allSolutions();
        }
        //load Person Entity extraction rules file.
        for (String fname : getLogicRuleFiles()) {
            consultQueryFacts = new Query("consult", new Atom(fname));
            consultQueryFacts.allSolutions();
        }
    }

    void unload_prolog_files(DocFacts input) {
        Set<String> getFactsPLFileNames = input.getFactsPLFileNames();
        Query consultQueryFacts=null;
        for(String fname: getFactsPLFileNames) {
        consultQueryFacts = new Query("unload_file", new Atom(fname));
        consultQueryFacts.allSolutions();
        }
        for(String fname: getLogicRuleFiles()) {
            consultQueryFacts = new Query("unload_file", new Atom(fname));
            consultQueryFacts.allSolutions();
        }


    }
}
