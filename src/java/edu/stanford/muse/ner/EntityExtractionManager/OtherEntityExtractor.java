package edu.stanford.muse.ner.EntityExtractionManager;

import alice.tuprolog.*;
import edu.stanford.muse.ner.model.NEType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.util.*;

public class OtherEntityExtractor extends EntityExtractor {

    static Log log = LogFactory.getLog(OtherEntityExtractor.class);


    private static final String nOtherEntityExtractionRulesFile="OtherEntitiesExtractionRules.pl";
    private static final String nDBPediaFactFile = "modelFactsTruncated.pl";
    Set<String> getLogicRuleFiles(){
        Set<String> tmp = new LinkedHashSet<>();
        tmp.add(System.getProperty("user.home")+File.separator+nOtherEntityExtractionRulesFile);
        tmp.add(System.getProperty("user.home")+File.separator+nDBPediaFactFile);
        return tmp;
    }
    /*This method takes a set of facts (with CIC encapsulated in them), and extract entities from them.
        IT then fills the type of CIC's back into the set of facts and return them.
     */
    public DocFacts extractEntities(DocFacts input, NEType.Type type){

        //Step 1. Dump facts from input in some files
        input.dumpFacts(true);
        //Step 2. Load the logic rule files for this extractor in swipl
        ////2.1 - Init JPL and load facts/rules
        Prolog engine = initPrologAndLoadRules_Facts(input);
        //Step 3. Get the set of resolved CIC's obtained by using the rules.

        log.info("================================"+type.getDisplayName()+"================================");
        Set<String> seenSet = new LinkedHashSet<>();
        Set<String> seenCIC = new LinkedHashSet<>();

        {
            //get list of all unresolved CIC's from input docFacts.
            Set<CICFact> unresolvedCICs= input.getUnResolvedCICs();

            //Iterate over all and fire a query to check if X is of given type.
            unresolvedCICs.forEach(cic->{
                //NOTE: An atom string can not start from upper case character. This needs to be taken care in the prolog rules as well especially when our facts contain strings
                //starting from capital letter.
                if(!seenSet.contains(cic.nCIC)) {
                    try {
                        SolveInfo result = engine.solve("resolvedCIC(CIC,STRTP,ENDP,MID,\""+type.getDisplayName().toLowerCase()+"\",REASON,EID).");

                        while (result.isSuccess()) {
                            List<Var> vars = result.getBindingVars();
                            String cicn = result.getVarValue("CIC").toString();
                            String entityid = result.getVarValue("EID").toString();
                            if(!seenCIC.contains(cic)) {
                                seenCIC.add(cicn);
                                input.updateResolvedType(cicn, NEType.Type.PERSON, Integer.parseInt(entityid));
                            }
                            break;//get only one solution for now.

                        }
                    } catch (MalformedGoalException e) {
                        e.printStackTrace();
                    } catch (NoSolutionException e) {
                        e.printStackTrace();
                    }
                }

            });
        }
        seenSet.clear();
        /***** Merging of entities together-- Update as and when more rules are found--------*/
        /*
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
                input.removeCIC(cic.name(),start.intValue(),end.intValue(),nextcic.name(),inbetw.name(),mid.name());
            }
        }

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
                input.updateResolvedType(cic.name(),start.intValue(),end.intValue(),nextcic.name(),inbetw.name(),NEType.Type.PERSON,mid.name(),cid.intValue());
                //input.removeCIC(cic.toString(),start.intValue(),end.intValue(),nextcic.toString(),inbetw.toString(),mid.toString());
            }
        }
        */
        //unload rule files
        unload_prolog_files(engine,input);
        return input;
    }


}
