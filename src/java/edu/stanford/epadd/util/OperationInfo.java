package edu.stanford.epadd.util;


import com.google.common.collect.Multimap;
import edu.stanford.muse.email.StaticStatusProvider;
import edu.stanford.muse.email.StatusProvider;
import org.json.JSONObject;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * This class contains the metadata about the current operation that is in progress.
 * Mainly used for the bookkeeping of
 */
public abstract class OperationInfo {
    //to denote the status of this operation
    StatusProvider statusProvider;
    //to denote the threadID that is running this operation.
    ExecutorService executorID;
    //Result object in case this operation got over.
    JSONObject resultJSON;
    //operationID - unique to every operation object.
    String opID;
    //name of the JSP API that was invoked
    String actionName;
    //map of passed parameters
    Multimap<String, String> parametersMap;

    public OperationInfo(String actionName, String opID, Multimap<String,String> parametersMap){
        this.actionName = actionName;
        this.opID = opID;
        this.parametersMap = parametersMap;
        this.resultJSON = new JSONObject();

    }

    public void setStatusProvider(StatusProvider sp){
        this.statusProvider = sp;
    }

    public StatusProvider getStatusProvider(){
        return statusProvider;
    }


    public JSONObject getResultJSON(){
        if(resultJSON==null || resultJSON.length()==0) //we will check for non-readiness of result by checking if it is empty or not.
            return null;
        return resultJSON;
    }

    public Multimap<String, String> getParametersMap(){
        return parametersMap;
    }
    public void run(){
        //execute onStart method on a new thread and put that thread ID in this class variable (threadID)
        executorID = Executors.newSingleThreadExecutor();
        executorID.submit(() -> {
            onStart(resultJSON);
        });
    }

    public void cancel(){
        //kill the thread corresponding to threadID and execute onCancel method.
        executorID.shutdownNow();
        //set status provider telling that the operation is being cancelled.
        setStatusProvider(new StaticStatusProvider("Canelling the operation..."));
        onCancel();
        //after operation is cancelled set appropriate JSON object as result to be sent to the client and also set resType as done.
        resultJSON.put("status", 0);
        resultJSON.put("cancelled", true);
        resultJSON.put("responseText", "Operation cancelled by the user");
        setStatusProvider(new StaticStatusProvider("Operation cancelled by the user"));
    }
    public abstract void onStart(JSONObject resultJSON);

    public abstract void onCancel();
}
