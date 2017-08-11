package edu.stanford.muse.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.ArrayList;
import java.util.List;

/** utility class to help keep track of the time */
public class TimeKeeper {
    public static Log log = LogFactory.getLog(TimeKeeper.class);

	private static final int MAX = 100;
	
	// Thread local variable containing each thread's ID
     private static final ThreadLocal<List<Long>> times = new ThreadLocal<List<Long>>() {
             @Override 
             protected List<Long> initialValue() {
            	 return new ArrayList<Long>();
         }
     };

     public static void snap() {
    	 List<Long> threadLocalTimes = times.get();
    	 // just be defensive, in case this got called in a loop... it will run away and take an unbounded amount of memory. if it goes above MAX, just clear and throw away the values.
    	 if (threadLocalTimes.size() == MAX)
    	 {
    		 log.warn ("TIMEKEEPER FATAL ERROR! threadLocalTimes size is going beyond allowed maximum of " + MAX + ". This probably means that Timekeeper.snap is being called inside a loop!");
    		 threadLocalTimes.clear();
    	 }
    	 threadLocalTimes.add(System.currentTimeMillis());
     }
     
     public static String since() { return since(1); }
     
     /** n is the number of snaps to go back */
     public static String since(int n)
     {
    	 List<Long> threadLocalTimes = times.get();
    	 long now = System.currentTimeMillis();
    	 int idx = threadLocalTimes.size() - n;
    	 if (idx < 0 || idx > threadLocalTimes.size()-1)
    		 return "???";
    	 
    	 long past = threadLocalTimes.get(idx);
    	 long millis = now - past;
    	 if (millis < 1000)
    		 return Util.commatize(millis) + "ms";
    	 else
    		 return Util.commatize(millis/1000) + " seconds";
     }     

     static class TestTimeKeeper extends Thread {
    	 int id;
    	 TestTimeKeeper(int id) { this.id = id; }
    	 public void run() {
    		 TimeKeeper.snap ();
    		 try {
    			 Thread.sleep (3000);
    			 TimeKeeper.snap ();
    			 Thread.sleep (2000);
    			 System.out.println ("Thread " + id + ": Time since 1st snap is " + TimeKeeper.since(2) + ", since 2nd snap is " + TimeKeeper.since(1));
    			 System.out.println ("Checking error checking: " + TimeKeeper.since(-2) + " or " + TimeKeeper.since(3));
    		 } catch (InterruptedException e) {
    			 // TODO Auto-generated catch block
    			 e.printStackTrace();
    		 }
    	 }
     }
     
     public static void main (String args[]) throws InterruptedException
     {
    	 new TestTimeKeeper(1).start();
    	 Thread.sleep (2000);
    	 new TestTimeKeeper(2).start();

    	 // check error check
    	 for (int i = 0; i < 1000; i++)
    		 TimeKeeper.snap();
     }
}
