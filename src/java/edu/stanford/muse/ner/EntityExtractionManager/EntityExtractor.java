package edu.stanford.muse.ner.EntityExtractionManager;


import java.io.File;
import java.util.Set;

public abstract class EntityExtractor {

    abstract  Set<String> getLogicRuleFiles();

    void initPrologAndLoadRules_Facts(DocFacts input) {
    }

    void unload_prolog_files(DocFacts input) {


    }
}
