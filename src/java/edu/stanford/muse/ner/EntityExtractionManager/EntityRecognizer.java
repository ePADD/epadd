package edu.stanford.muse.ner.EntityExtractionManager;


import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.index.SearchResult;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.ner.tokenize.CICTokenizer;
import edu.stanford.muse.ner.tokenize.Tokenizer;
import edu.stanford.muse.util.NLPUtils;
import edu.stanford.muse.util.Span;
import edu.stanford.muse.util.Triple;
import edu.stanford.muse.util.Util;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;


import javax.print.Doc;
import java.io.IOException;
import java.util.*;

public class EntityRecognizer {

    static Log log = LogFactory.getLog(EntityRecognizer.class);

    Map<String,Set<CICFact>> nExtractedAndResolved = new LinkedHashMap<>();
    Map<CICFact, NEType.Type> nResolvedCICs = new LinkedHashMap<>();

    /*
    Entry point to start extraction and recognition of all content (content and title) from all docs of archive.
     */

    public void extractAndRecognize(Archive archive){
        //Step 1. Decide on how many messages to look in one chunk. We can either look at all messages of one thread id
        //or all messages in a given month or year. The assumption is that because of the locality of reference it makes
        //more sense to look at closely spaced messages to get the same entity referenced multiple times.

        //Collection<Set<Document>> clusters = getClustersByThreadIDs(archive);

        Collection<Set<Document>> clusters = getClustersChronologicallyFixedSize(archive,500);

        //Step 2. Iterate over all chunks and for each chunk of documents
        clusters.forEach(cluster->{
            //Step 3. Generate local fact files and global fact files (if needed)
            // dumpCICFacts, dumpMetaFacts, dumpContacts, dumpResolvedSet[This is initialized to empty and will keep on
            //getting populated when passing through different extractors]
            DocFacts dfacts = new DocFacts(cluster,archive);
            cluster.forEach(doc->{
               // EmailDocument ed=(EmailDocument)doc;
                dfacts.extractAndStoreCIC(doc);

                //Step 3.3 - get Correspondent ids, threadid, date for this document and add them to dfacts
                dfacts.extractAndStoreMetaData(doc);
            });

            //Use already resolved CICs (stored in map nExtractedAndResolved) to mark the CIC's in dfacts as resolved.
            dfacts.markAsResolved(nResolvedCICs);


            //Now docFacts is ready with all the facts for the documents present in this cluster.
            //Step 4. Invoke Different extractors like PersonEntityExtractor, PlaceEntityExtractors etc. which in turn will
            //use different logic to return an updated docFacts. This docFacts will have resolved types added to CIC's wherever
            //appropriately resolved.
            DocFacts processed = new PersonEntityExtractor().extractEntities(dfacts);
            //Step 5. If needed enhance the set of facts based on the return value and invoke the next Extractor

            ///Invoke other entity extractor with different types
//            OtherEntityExtractor otherEntityExtractor = new OtherEntityExtractor();
//            for(NEType.Type type : NEType.getAllTypes()){
//                if(!type.getDisplayName().equals("Person")){//for now do it only for non person types types.
//                    processed = otherEntityExtractor.extractEntities(processed,type);
//                }
//            }
            //at the end add resolved CIC's in the map.

            Map<String, Set<CICFact>> resolved  = processed.getResolvedCICs();
            nExtractedAndResolved.putAll(resolved);
            //add all resolved CIC's in the store
            resolved.forEach((mid,setcic)->setcic.forEach(cic->nResolvedCICs.put(cic,cic.nType)));
        });
 }


    /*
    Method to cluster documents by threadids so that all messages of one threadid are clustered together.
     */

    private Collection<Set<Document>> getClustersByThreadIDs(Archive archive){

            Map<Long,Set<Document>> threadClusters= new LinkedHashMap<>();
            for (Document ed : archive.getAllDocs()) {
                long tid = ((EmailDocument)ed).threadID;
                Set<Document> docset=threadClusters.get(tid);
                if(docset==null) {
                    docset = new LinkedHashSet<>();
                    threadClusters.put(tid, docset);
                }
                docset.add(ed);
            }
            return threadClusters.values();

    }

    /*
    Method to cluster documents by Months so that all messages of one month are clustered together.
     */
    private Collection<Set<Document>> getClustersChronologicallyFixedSize(Archive archive,int chunksize){

        Collection<Set<Document>> clusters= new LinkedHashSet<>();

        //get the set of documents in chronological order and then divide it in chnuksize.
        List<Document> resultDocsList = archive.getAllDocs();
        Collections.sort(resultDocsList);

        int count =0;
        Set<Document> currentcluster = new LinkedHashSet<>();
        for(Document doc: resultDocsList){
            if((count+1)%chunksize==0){
                //means one cluster is done..save it in clusters collection.. reinitialize set current cluster as empty and reset count to 0.
                clusters.add(currentcluster);
                currentcluster=new LinkedHashSet<>();
                count=0;
            }else{
                //else put this doc in current cluster
                currentcluster.add(doc);
                count++;
            }
        }
        return clusters;
    }




    /*
    Returns a list of entites extracted from the content part of the given message
     */
    public Span[] findFromContent(String messageID){

        Set<CICFact> resolved = nExtractedAndResolved.get(messageID);
        List<Span> spans = new ArrayList<>();

        if(resolved!=null){
            resolved.forEach(fact->{
                Span chunk = new Span(fact.nCIC, (int)fact.nStart, (int)fact.nEnd);
                chunk.setType(fact.nType.getCode(),1);//put dummy score 1 for now
                spans.add(chunk);
            });

        }
        return spans.toArray(new Span[spans.size()]);

    }

    /*
    Returns a list of entities extracted from the title part of the given message
     */
    public Span[] findFromTitle(String messageID){

        return null;
    }



}
