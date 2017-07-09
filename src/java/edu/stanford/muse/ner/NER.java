package edu.stanford.muse.ner;

import edu.stanford.muse.email.StatusProvider;
import edu.stanford.muse.exceptions.CancelledException;
import edu.stanford.muse.ie.KillPhrases;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.Document;
import edu.stanford.muse.index.Indexer;
import edu.stanford.muse.ner.featuregen.FeatureUtils;
import edu.stanford.muse.ner.model.DummyNERModel;
import edu.stanford.muse.ner.model.NERModel;
import edu.stanford.muse.ner.model.NEType;
import edu.stanford.muse.util.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.FieldType;
import org.apache.lucene.document.StoredField;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This is the only class dependent on ePADD in this package and has all the interfacing functionality
 *
 * TODO: trainAndrecognise and train methods should take proper options argument and there should be a class that represents training options*/
public class NER implements StatusProvider {
    public static Log		    log					= LogFactory.getLog(NER.class);
    //names and names_original should include all the names in the title
    public static String		NAMES				= "names", NAMES_ORIGINAL = "names_original";
    public static String		NAMES_TITLE			= "en_names_title";

    String						status;
    double						pctComplete			= 0;
    boolean						cancelled			= false;
    Archive						archive				= null;
    NERModel               nerModel;
    //in seconds
    long						time				= -1, eta = -1;
    static FieldType			ft;
    int[]						pcts				= new int[] { 16, 32, 50, 100 };
    StatusProvider statusProvider =  null;

    public static class NERStats {
        //non-repeating number of instances of each type
        public Map<Short, Integer>		counts;
        public Map<Short, Set<String>>	all;

        public NERStats() {
            counts = new LinkedHashMap<>();
            all = new LinkedHashMap<>();
        }

        //a map of entity-type key and value list of entities
        public void update(Span[] names) {
            Arrays.asList(names).forEach(sp->{
                short ct = NEType.getTypeForCode(sp.type).getCode();
                if(!all.containsKey(sp.type))
                    all.put(sp.type, new LinkedHashSet<>());
                all.get(sp.type).add(sp.text);
                if(!all.containsKey(ct))
                    all.put(ct, new LinkedHashSet<>());
                all.get(ct).add(sp.text);
            });
            all.entrySet().forEach(e->counts.put(e.getKey(), e.getValue().size()));
        }

        @Override
        public String toString() {
            String str = "";
            for (Short t : counts.keySet())
                str += "Type: " + t + ":" + counts.get(t) + "\n";
            return str;
        }
    }

    public static class NEROptions {
        public boolean addressbook = true, dbpedia = true, segmentation = true;
        public String prefix = "";
        public String wfsName = "WordFeatures.ser", modelName = "svm.model";
        public String evaluatorName = "ePADD NER complete";
        public String dumpFldr = null;

        public NEROptions setAddressBook(boolean val) {
            this.addressbook = val;
            return this;
        }

        public NEROptions setDBpedia(boolean val) {
            this.dbpedia = val;
            return this;
        }

        public NEROptions setSegmentation(boolean val) {
            this.segmentation = val;
            return this;
        }

        public NEROptions setPrefix(String prefix){
            this.prefix = prefix;
            return this;
        }

        public NEROptions setName(String name){
            this.evaluatorName = name;
            return this;
        }

        public NEROptions setDumpFldr(String name){
            this.dumpFldr = name;
            return this;
        }
    }

    public NERStats	stats;

    static {
        ft = new FieldType();
        ft.setStored(true);
        ft.setIndexed(true);
        ft.freeze();
    }

    public NER(Archive archive, NERModel nerModel) {
        this.archive = archive;
        this.nerModel = nerModel;
        time = 0;
        eta = 10 * 60;
        stats = new NERStats();
    }

    private static void storeSerialized(org.apache.lucene.document.Document doc, String fieldName, Object obj)
    {
        FieldType storeOnly_ft = new FieldType();
        storeOnly_ft.setStored(true);
        storeOnly_ft.freeze();
        try {
            ByteArrayOutputStream bs = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bs);
            oos.writeObject(obj);
            oos.close();
            bs.close();
            doc.removeField(fieldName);
            doc.add(new Field(fieldName, bs.toByteArray(), storeOnly_ft));
        } catch (IOException e) {
            log.warn("Failed to serialize field: "+fieldName);
            e.printStackTrace();
        }
    }

    /* body = true => in message body, false => in subject */
    public static Span[] getNames(org.apache.lucene.document.Document doc, boolean body) {
        if (doc == null)
            return new Span[0];

        String fieldName;
        if(body)
            fieldName = NAMES;
        else
            fieldName = NAMES_TITLE;
        String val = doc.get(fieldName);

        if (val == null) {
            log.warn ("Names NOT extracted in document");
            return new Span[0];
        }

        String[] plainSpans = val.split(Indexer.NAMES_FIELD_DELIMITER);
        // remove kill phrases here itself
        List<Span> spans = Arrays.asList(plainSpans).stream().map(Span::parse).filter(s -> s != null && !KillPhrases.isKillPhrase(s.getText())).collect(Collectors.toList());

        return spans.toArray(new Span[spans.size()]);
    }

    /* body = true => in message body, false => in subject */
    public static Span[] getNames(Document doc, boolean body, Archive archive) throws IOException{
        org.apache.lucene.document.Document ldoc = archive.getLuceneDoc(doc.getUniqueId());
        return getNames(ldoc, body);
    }

    //main method trains the model, recognizes the entities and updates the doc.
    public void recognizeArchive() throws CancelledException, IOException {
        time = 0;
        archive.openForRead();
        archive.setupForWrite();

        if (cancelled) {
            status = "Cancelling...";
            throw new CancelledException();
        }

        List<Document> docs = archive.getAllDocs();

        if (cancelled) {
            status = "Cancelling...";
            throw new CancelledException();
        }

        int di = 0, ds = docs.size();
        int ps = 0, ls = 0, os = 0;

        long totalTime = 0, updateTime = 0, recTime = 0, duTime = 0, snoTime = 0;
        for (Document doc : docs) {
            long st1 = System.currentTimeMillis();
            long st = System.currentTimeMillis();
            org.apache.lucene.document.Document ldoc = archive.getLuceneDoc(doc.getUniqueId());
            //pass the lucene doc instead of muse doc, else a major performance penalty
            //do not recognise names in original content and content separately
            //Its possible to improve the performance further by using linear kernel
            // instead of RBF kernel and classifier instead of a regression model
            // (the confidence scores of regression model can be useful in segmentation)
            String originalContent = archive.getContents(ldoc, true);
            String content = archive.getContents(ldoc, false);
            String title = archive.getTitle(ldoc);
            //original content is substring of content;

            Span[] names = nerModel.find(content);
            Span[] namesT = nerModel.find(title);
            recTime += System.currentTimeMillis() - st;
            st = System.currentTimeMillis();

            stats.update(names);
            stats.update(namesT);
            updateTime += System.currentTimeMillis() - st;
            st = System.currentTimeMillis();

            //!!!!!!SEVERE!!!!!!!!!!
            //TODO: an entity name is stored in NAMES, NAMES_ORIGINAL, nameoffsets, and one or more of EPER, ELOC, EORG fields, that is a lot of redundancy
            //!!!!!!SEVERE!!!!!!!!!!
//			storeSerialized(ldoc, NAMES_OFFSETS, mapAndOffsets.second);
//            storeSerialized(ldoc, TITLE_NAMES_OFFSETS, mapAndOffsetsTitle.second);
//            storeSerialized(ldoc, FINE_ENTITIES, mapAndOffsets.getFirst());
//            storeSerialized(ldoc, TITLE_FINE_ENTITIES, mapAndOffsets.getSecond());

            Map<Short, Integer> counts = new LinkedHashMap<>();
            Map<Short, Integer> countsT = new LinkedHashMap<>();
            Arrays.asList(names).stream().map(sp-> NEType.getCoarseType(sp.type).getCode()).forEach(s->counts.put(s,counts.getOrDefault(s,0)+1));
            Arrays.asList(namesT).stream().map(sp-> NEType.getCoarseType(sp.type).getCode()).forEach(s->countsT.put(s,countsT.getOrDefault(s,0)+1));
            ps += counts.getOrDefault(NEType.Type.PERSON.getCode(), 0) + countsT.getOrDefault(NEType.Type.PERSON.getCode(), 0);
            ls += counts.getOrDefault(NEType.Type.PLACE.getCode(), 0) + countsT.getOrDefault(NEType.Type.PLACE.getCode(), 0);
            os += counts.getOrDefault(NEType.Type.ORGANISATION.getCode(), 0) + countsT.getOrDefault(NEType.Type.ORGANISATION.getCode(), 0);

            snoTime += System.currentTimeMillis() - st;
            st = System.currentTimeMillis();

            ldoc.removeField(NAMES);ldoc.removeField(NAMES_TITLE);

            ldoc.add(new StoredField(NAMES,
                    Util.join(Arrays.asList(names).stream().map(Span::parsablePrint).collect(Collectors.toSet()), Indexer.NAMES_FIELD_DELIMITER)));
            ldoc.add(new StoredField(NAMES_TITLE,
                    Util.join(Arrays.asList(namesT).stream().map(Span::parsablePrint).collect(Collectors.toSet()), Indexer.NAMES_FIELD_DELIMITER)));

            int ocs = originalContent.length();
            List<String> namesOriginal = Arrays.asList(names).stream().filter(sp->sp.end<ocs).map(Span::parsablePrint).collect(Collectors.toList());

            ldoc.add(new StoredField(NAMES_ORIGINAL, Util.join(namesOriginal, Indexer.NAMES_FIELD_DELIMITER)));
            //log.info("Found: "+names.size()+" total names and "+names_original.size()+" in original");

            //TODO: Sometimes, updating can lead to deleted docs and keeping these deleted docs can bring down the search performance
            //Could building a new index be faster?
            archive.updateDocument(ldoc);
            duTime += System.currentTimeMillis() - st;
            di++;

            totalTime += System.currentTimeMillis() - st1;
            pctComplete = 30 + ((double)di/(double)ds) * 70;
            double ems = (double) (totalTime * (ds-di)) / (double) (di*1000);
            status = "Recognized entities in " + Util.commatize(di) + " of " + Util.commatize(ds) + " emails ";
            //Util.approximateTimeLeft((long)ems/1000);
            eta = (long)ems;

            if(di%100 == 0)
                log.info(status);
            time += System.currentTimeMillis() - st;

            if (cancelled) {
                status = "Cancelling...";
                throw new CancelledException();
            }
        }

        log.info("Trained and recognised entities in " + di + " docs in " + totalTime + "ms" + "\nPerson: " + ps + "\nOrgs:" + os + "\nLocs:" + ls);
        archive.close();
        //prepare to read again.
        archive.openForRead();
    }

    //arrange offsets such that the end offsets are in increasing order and if there are any overlapping offsets, the bigger of them should appear first
    //makes sure the redaction is proper.
    public static void arrangeOffsets(List<Triple<String,Integer,Integer>> offsets) {
        Collections.sort(offsets, new Comparator<Triple<String, Integer, Integer>>() {
            @Override
            public int compare(Triple<String, Integer, Integer> t1, Triple<String, Integer, Integer> t2) {
                if (!t1.getSecond().equals(t2.getSecond()))
                    return t1.getSecond()-t2.getSecond();
                else
                    return t2.getThird()-t1.getThird();
            }
        });
    }

    @Override
    public String getStatusMessage() {
        if(statusProvider!=null)
            return statusProvider.getStatusMessage();

        return JSONUtils.getStatusJSON(status, (int) pctComplete, time, eta);
    }

    @Override
    public void cancel() {
        cancelled = true;
        if(statusProvider!=null)
            statusProvider.cancel();
    }

    @Override
    public boolean isCancelled() {
        return cancelled || statusProvider.isCancelled();
    }

//	public static void main1(String[] args) {
//		try {
//			String userDir = System.getProperty("user.home") + File.separator + "epadd-appraisal" + File.separator + "user";
//            Archive archive = SimpleSessions.readArchiveIfPresent(userDir);
//            NER ner = new NER(archive, null);
//            System.err.println("Loading model...");
//            long start = System.currentTimeMillis();
//            NERModel model = ner.trainModel(false);
//            System.err.println("Trained model in: " + (System.currentTimeMillis() - start));
//            System.err.println("Done loading model");
//            String[] pers = new String[]{"Senator Jim Scott", "Rep. Bill Andrews"};
//            String[] locs = new String[]{"Florida", "Plantation"};
//            String[] orgs = new String[]{"Broward Republican Executive Committee", "National Education Association"};
//            String text = "First I would like to tell you who I am. I am a lifelong Republican and have served on the Broward Republican Executive Committee since 1991. I have followed education issues in Florida since I moved here in 1973. All four of my children went to public schools here in Plantation. I continued to study education issues when I worked for Senator Jim Scott for six years, and more recently as I worked for Rep. Bill Andrews for the past eight years.\n" +
//                    "On the amendment, I would like to join any effort to get it repealed. Second, if the amendment is going to be implemented, I believe that decisions about how money is spent should be taken out of the hands of the school boards. I know the trend has been to provide more local control, however, there has been little or no accountability for school boards that fritter away money on consultants, shoddy construction work, and promoting the agenda of the National Education Association and the local teachers’ unions. Third, while the teachers’ union is publicly making “nice” with you and other Republican legislators, they continue to undermine education reform measures, and because school board members rely heavily on the unions to get elected and re-elected, they pretty much call the shots on local policies. ";
//            Pair<Map<Short,List<String>>, List<Triple<String, Integer, Integer>>> ret = model.find(text);
//            boolean testPass = true;
//            for(Short type: ret.getFirst().keySet()) {
//                System.err.print("Type: " + type);
//                for (String str : ret.getFirst().get(type))
//                    System.err.print(":::" + str + ":::");
//                System.err.println();
//            }
//        } catch (Exception e) {
//			e.printStackTrace();
//		}
//	}

    public static void main(String[] args){
        String val = "";
        String[] plainSpans = val.split(Indexer.NAMES_FIELD_DELIMITER);
        List<Span> spans = Arrays.asList(plainSpans).stream().map(Span::parse).filter(s->s!=null).collect(Collectors.toList());
        System.out.println(spans);
    }
}
