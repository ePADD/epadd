package edu.stanford.muse.ner.EntityExtractionManager;

import edu.stanford.muse.ner.model.NEType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jpl7.*;

import java.io.File;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

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
        initPrologAndLoadRules_Facts(input);
        //Step 3. Get the set of resolved CIC's obtained by using the rules.
        Variable CIC = new Variable("CIC");
        Variable STRTP = new Variable("STARTP");
        Variable ENDP = new Variable("ENDP");
        Variable MID = new Variable("MID");
        Variable REASON = new Variable("REASON");
        Variable CID = new Variable("CID");

        Set<String> seenCIC = new LinkedHashSet<>();
        {
            Query tq = new Query("resolvedCIC", new Term[]{CIC, STRTP, ENDP, MID, new Atom("person"), REASON, CID});
            tq.allSolutions();
            while (tq.hasMoreElements()) {
                HashMap binding = (HashMap) tq.nextElement();
                Term cic = (Term) binding.get(CIC.name);
                Term mid = (Term) binding.get(MID.name);
                Term entityid = (Term) binding.get(CID.name);
                if(!seenCIC.contains(cic.name())){
                    log.info("Resolved the type of " + cic.toString() + " to PERSON, id = " + entityid.intValue()+" in message -"+mid.name());
                    //Set the types of these CIC's (as inferred) in the input docfacts object.
                    input.updateResolvedType(cic.name(), NEType.Type.PERSON, entityid.intValue());
                    seenCIC.add(cic.name());
                }

            }
        }
        seenCIC.clear();
        //dump the docfacts again, this time setting uniquCICfacts as false.
        input.dumpFacts(false);
        //again load JPL after closing, load fact files and  rules..
        initPrologAndLoadRules_Facts(input);
        //get the set of combinedCIC's and mergedCICs. Remove mergedCICs from input docFact and add combinedCIC's if not already present.
        Variable NXTCIC = new Variable("NXTCIC");
        Variable INBETW = new Variable("INBETW");

        {
            Query tq = new Query("mergedCIC", new Term[]{CIC, STRTP, ENDP, NXTCIC, INBETW, MID});

            while (tq.hasMoreElements()) {
                HashMap binding = (HashMap) tq.nextElement();
                Term cic = (Term) binding.get(CIC.name);
                Term start = (Term) binding.get(STRTP.name);
                Term end = (Term) binding.get(ENDP.name);
                Term nextcic = (Term) binding.get(NXTCIC.name);
                Term inbetw = (Term) binding.get(INBETW.name);
                Term mid = (Term) binding.get(MID.name);
                //remove all merged CICs
                if(!seenCIC.contains(cic.name())){
                    seenCIC.add(cic.name());
                    log.info("CIC - Person names "+cic.name()+" and "+nextcic.name()+" were merged together (LastName, FirstName) in message - "+mid.name());
                    input.removeCIC(cic.name(),start.intValue(),end.intValue(),nextcic.name(),inbetw.name(),mid.name());

                }
            }
        }
        seenCIC.clear();
        //get combinedCICs and add them to the list of CIC's along with their inferred type and id.
        {
            Query tq = new Query("combinedCIC", new Term[]{CIC, STRTP, ENDP, NXTCIC, MID,INBETW, CID});

            while (tq.hasMoreElements()) {
                HashMap binding = (HashMap) tq.nextElement();
                Term cic = (Term) binding.get(CIC.name);
                Term start = (Term) binding.get(STRTP.name);
                Term end = (Term) binding.get(ENDP.name);
                Term nextcic = (Term) binding.get(NXTCIC.name);
                Term inbetw = (Term) binding.get(INBETW.name);
                Term mid = (Term) binding.get(MID.name);
                Term cid = (Term) binding.get(CID.name);
                //remove all merged CICs
                if(!seenCIC.contains(cic.name())) {
                    seenCIC.add(cic.name());
                    log.info("Combined CIC, Person,  added - "+cic.name()+" in message- "+mid.name());
                    input.updateResolvedType(cic.name(),start.intValue(),end.intValue(),nextcic.name(),inbetw.name(),NEType.Type.PERSON,mid.name(),cid.intValue());
                }
                 //input.removeCIC(cic.toString(),start.intValue(),end.intValue(),nextcic.toString(),inbetw.toString(),mid.toString());
            }
        }

        //unload files from prolog.
        unload_prolog_files(input);
        return input;
    }

}
