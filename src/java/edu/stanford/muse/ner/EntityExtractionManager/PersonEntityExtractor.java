package edu.stanford.muse.ner.EntityExtractionManager;

import alice.tuprolog.*;
import edu.stanford.muse.ner.model.NEType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

public class PersonEntityExtractor extends EntityExtractor {

    static Log log = LogFactory.getLog(PersonEntityExtractor.class);

    private static final String nPersonEntityExtractionRulesFile="PersonExtractionRules.pl";

    Set<String> getLogicRuleFiles(){
        Set<String> tmp = new LinkedHashSet<>();
        tmp.add(System.getProperty("user.home")+File.separator+nPersonEntityExtractionRulesFile);
        return tmp;
    }
    /*This method takes a set of facts (with CIC encapsulated in them), and extract entities from them.
        IT then fills the type of CIC's back into the set of facts and return them.
     */
    public DocFacts extractEntities(DocFacts input){

        //Step 1. Dump facts from input in some files
        input.dumpFacts(true);
        //Step 2. Load the logic rule files for this extractor in swipl
        ////2.1 - Init JPL and load facts/rules
        Prolog engine = initPrologAndLoadRules_Facts(input);
        //Step 3. Get the set of resolved CIC's obtained by using the rules.

        try {
            engine.loadLibrary("edu.stanford.muse.ner.EntityExtractionManager.PLLibrary.StringLibrary_PL");
        } catch (InvalidLibraryException e) {
            e.printStackTrace();
        }
        Set<String> seenCIC = new LinkedHashSet<>();
        {
            try {
                SolveInfo result = engine.solve("resolvedCIC(CIC,STRTP,ENDP,MID,\"person\",REASON,CID).");
                while (result.isSuccess()) {
                    List<Var> vars = result.getBindingVars();
                        String cic = result.getVarValue("CIC").toString();
                        String entityid = result.getVarValue("CID").toString();
                        if(!seenCIC.contains(cic)) {
                            seenCIC.add(cic);
                            input.updateResolvedType(cic, NEType.Type.PERSON, Integer.parseInt(entityid));
                        }
                        if (engine.hasOpenAlternatives())
                            result = engine.solveNext();
                        else
                            break;
                }
            } catch (MalformedGoalException e) {
                e.printStackTrace();
            } catch (NoSolutionException e) {
                e.printStackTrace();
            } catch (NoMoreSolutionException e) {
                e.printStackTrace();
            }

        }

        seenCIC.clear();
        //dump the docfacts again, this time setting uniquCICfacts as false.
        input.dumpFacts(false);
        engine.clearTheory();
        engine = null;
        //again load JPL after closing, load fact files and  rules..
        engine = initPrologAndLoadRules_Facts(input);
        //get the set of combinedCIC's and mergedCICs. Remove mergedCICs from input docFact and add combinedCIC's if not already present.

        {
            try {
                SolveInfo result = engine.solve("mergedCIC(CIC,STRTP,ENDP,NEXTCIC,INBETW,MID).");
                while (result.isSuccess()) {
                    List<Var> vars = result.getBindingVars();
                    String cic = result.getVarValue("CIC").toString();
                    String nextcic = result.getVarValue("NEXTCIC").toString();
                    String mid = result.getVarValue("MID").toString();
                    String inbetw = result.getVarValue("INBETW").toString();
                    int startp = Integer.parseInt(result.getVarValue("STRTP").toString());
                    int endp = Integer.parseInt(result.getVarValue("ENDP").toString());
                    if(!seenCIC.contains(cic)){
                        seenCIC.add(cic);
                        log.info("CIC - Person names "+cic+" and "+nextcic+" were merged together (LastName, FirstName) in message - "+mid);
                        input.removeCIC(cic,startp,endp,nextcic,inbetw,mid);

                    }
                    if (engine.hasOpenAlternatives())
                        result = engine.solveNext();
                    else
                        break;
                }
            } catch (MalformedGoalException e) {
                e.printStackTrace();
            } catch (NoSolutionException e) {
                e.printStackTrace();
            } catch (NoMoreSolutionException e) {
                e.printStackTrace();
            }

        }
        seenCIC.clear();
        //get combinedCICs and add them to the list of CIC's along with their inferred type and id.
        {

            try {
                SolveInfo result = engine.solve("combinedCIC(CIC, STRTP, ENDP, NXTCIC, MID,INBETW, CID).");
                while (result.isSuccess()) {
                    List<Var> vars = result.getBindingVars();
                    String cic = result.getVarValue("CIC").toString();
                    String nextcic = result.getVarValue("NEXTCIC").toString();
                    String mid = result.getVarValue("MID").toString();
                    int cid = Integer.parseInt(result.getVarValue("CID").toString());
                    String inbetw = result.getVarValue("INBETW").toString();
                    int startp = Integer.parseInt(result.getVarValue("STRTP").toString());
                    int endp = Integer.parseInt(result.getVarValue("ENDP").toString());
                    if(!seenCIC.contains(cic)) {
                        seenCIC.add(cic);
                        log.info("Combined CIC, Person,  added - "+cic+" in message- "+mid);
                        input.updateResolvedType(cic,startp,endp,nextcic,inbetw,NEType.Type.PERSON,mid,cid);
                    }
                    if (engine.hasOpenAlternatives())
                        result = engine.solveNext();
                    else
                        break;
                }
            } catch (MalformedGoalException e) {
                e.printStackTrace();
            } catch (NoSolutionException e) {
                e.printStackTrace();
            } catch (NoMoreSolutionException e) {
                e.printStackTrace();
            }

        }

        //unload files from prolog.
        unload_prolog_files(engine,input);
        return input;
    }

    public static void main(String args[]){

        Prolog prolog = new Prolog();
        try {
            prolog.loadLibrary("edu.stanford.muse.ner.EntityExtractionManager.PLLibrary.StringLibrary_PL");
        } catch (InvalidLibraryException e) {
            e.printStackTrace();
        }

    }
}
