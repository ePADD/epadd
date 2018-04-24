package edu.stanford.muse.AnnotationManager;


import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.CSVWriter;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class AnnotationManager{

    private static Log log = LogFactory.getLog(AnnotationManager.class);

    //mapping from object id (string) to annotation (String).

    //moving forward we can think of internalizing strings so that storage is minimized.
    Map<String,String> docToAnnotation;


    public AnnotationManager (){
        docToAnnotation = new LinkedHashMap<>();
    }


    //set same annotation for a set of ids
    //the invariant is: if annotation is not empty then that info is in the map.. if annotation is empty
    //then that info is not in the map (if and only if relation)
    public void setAnnotationToAll(Set<String> docids, String annotation){
        if(Util.nullOrEmpty(annotation)){
            //remove all docids from docToAnnotation map
            docToAnnotation.keySet().removeAll(docids);
        }else{
            //put docid, annotation in the map
            docids.forEach(docid->docToAnnotation.put(docid,annotation));
        }
    }


    //get annotation for a set of ids
    //if not found in map return empty..
    public String getAnnotation(String docid){
        if(docToAnnotation.containsKey(docid)){
            return docToAnnotation.get(docid);
        }else
            return "";
    }


    //write annotation manager in human readable format

    public void writeObjectToStream(String filepath, Map<String,String> docidToSignature){
        try{
            FileWriter fw = new FileWriter(filepath);
            CSVWriter csvwriter = new CSVWriter(fw, ',', '"',' ',"\n");

            // write the header line: "DocID,annotation".
            List<String> line = new ArrayList<>();
            line.add ("DocID");
            line.add ("annotation");
            csvwriter.writeNext(line.toArray(new String[line.size()]));

            // write the records
            for(String docid: docToAnnotation.keySet()){
                String annotation =  docToAnnotation.get(docid); {
                    line = new ArrayList<>();
                    line.add(docid);
                    line.add(annotation);
                    String sig = docidToSignature.getOrDefault(docid,"ERROR! No document found in archive for this unique id");
                    String d = sig.replace("\n","").replace("\"","").replace(","," ");
                    //get the signature of this doc from docidToSignature map
                    //line.add(d);
                    csvwriter.writeNext(line.toArray(new String[line.size()]));
                }
            }
            csvwriter.close();
            fw.close();
        } catch (IOException e) {
            JSPHelper.log.warn("Unable to write docid to annotation map in csv file");
            return;
        }
    }

    //read annotation manager from a human readable file

    public static AnnotationManager readObjectFromStream(String filepath){
        File annotationfile = new File(filepath);
        AnnotationManager annotationManager = new AnnotationManager();
        if(annotationfile.exists()){
            //read the annotations and assign them to a document in archive (based on unique id)
            try{
                FileReader fr = new FileReader(annotationfile);
                CSVReader csvreader = new CSVReader(fr, ',', '"', ' ');

                // read line by line, except the first line which is header
                String[] record = null;
                record = csvreader.readNext();//skip the first line.
                while ((record = csvreader.readNext()) != null) {
                    String docid = record[0];
                    String annotation = record[1];//skip record[2] for the time being
                    annotationManager.docToAnnotation.put(docid,annotation);
                }

                csvreader.close();
                fr.close();
            } catch (IOException e) {
                log.warn("Unable to read docid to label map from csv file");

            }
        }

        return annotationManager;

    }

}
