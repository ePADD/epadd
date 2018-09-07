package edu.stanford.muse.ner.EntityExtractionManager;

public class PersonEntityExtractor {


    /*This method takes a set of facts (with CIC encapsulated in them), and extract entities from them.
        IT then fills the type of CIC's back into the set of facts and return them.
     */
    public DocFacts extractEntities(DocFacts input){

        //Step 1. Dump facts from input in some files
        input.dumpFacts();
        //Step 2. Load the logic rule files for this extractor in swipl
        ////2.1 - Init JPL

        ////2.2 -


        ////2.3 -



        //Step 3. Get the set of resolved CIC's

        //Step 4. Fill these types back into the DocFacts (by creating either a separate copy or in the same input object)

        //Step 5. Return the modified DocFacts with these CIC's marked as appropriate types (and may be by adding new CIC's if needed)
        return null;
    }
}
