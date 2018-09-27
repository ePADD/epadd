package edu.stanford.muse.ner.EntityExtractionManager;

import alice.tuprolog.InvalidTheoryException;
import alice.tuprolog.Prolog;
import alice.tuprolog.Theory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Set;

public abstract class EntityExtractor {

    abstract Set<String> getLogicRuleFiles();

    Prolog initPrologAndLoadRules_Facts(DocFacts input) {
        Prolog engine = new Prolog();
        try {
            //load fact files..
            Set<String> getFactsPLFileNames = input.getFactsPLFileNames();
            for (String fname : getFactsPLFileNames) {

                engine.addTheory(new Theory(new FileInputStream(fname)));
            }
            //load Person Entity extraction rules file.
            for (String fname : getLogicRuleFiles()) {
                engine.addTheory(new Theory(new FileInputStream(fname)));
            }
        } catch (InvalidTheoryException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return engine;
    }

    void unload_prolog_files(Prolog engine, DocFacts input) {
        engine.clearTheory();
    }
}
