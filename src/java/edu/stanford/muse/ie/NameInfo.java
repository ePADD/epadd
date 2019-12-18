package edu.stanford.muse.ie;

import edu.stanford.muse.AddressBookManager.Contact;
import edu.stanford.muse.util.Util;

import java.io.Serializable;
import java.util.Date;
import java.util.Map;

public class NameInfo implements Comparable<NameInfo>, Serializable { 
	private final static long serialVersionUID = 1L;

	public final String title /* with _ in place of space */;
	public String snippet;
	public String type = "notype";
	public float score;
	public String word /* no spaces */, originalTerm /* with spaces, capitalization etc */;
	public int times;
	public Date firstDate, lastDate = null;
	public Map<Contact, Integer> peopleToCount;
	public Map<String, Integer> sentimentCatToCount; // for each sentiment, how many docs with this name reflect that sentiment

	public NameInfo(String t) { this.title = t; } 
	public int compareTo(NameInfo other)
	{
		return other.times - this.times;
	}
	
	private String cleanTitle() { return title.replaceAll("_", " ");
	
	}
	
	private String firstDatetoString () {
		return Util.formatDate(firstDate);
	
	}
	private String lastDatetoString () {
		return Util.formatDate(lastDate);
	}
	public String toString()
	{
		return score/1000 + "k " + cleanTitle() + " (" + type + ") # " + snippet;
	}
	
	public String toString(boolean b)
	{
		return cleanTitle() + " (" + Util.pluralize(times,  "time") + ") score: " + score + " # " + snippet;
	}

	public String toString(boolean b, boolean b2)
	{
		return cleanTitle() + " (" + Util.pluralize(times,  "time") + ")";
	}
	
	public String toHTML(boolean withWPLink)
	{
		// update this to include first/last date, sentiments and count, and link to a browse page with term="..." and sentiment="..."

		String cleanTitle = cleanTitle(); 
		// title has to be escaped because it may have things like & embedded. e.g. saw Texas A&M university'
		String result = "<a target=\"_blank\" href=\"browse?term=%22" + Util.URLEncode(cleanTitle) + "%22\">" + cleanTitle + "<a>"  + " (" + Util.pluralize(times,  "time") + ")<br>";
		/*
		result += "<span style = 'color:orange'>First Date: " + firstDatetoString() + "<br>Last Date: " + lastDatetoString() + "<br><span style = 'color:green'>";
		if (sentimentCatToCount != null){
			for (String sentiment :sentimentCatToCount.keySet()){
				String sentimentlink = "browse?sentiment=" + sentiment+"&term=%22" + java.net.URLEncoder.encode(cleanTitle) + "%22\">" + sentiment + "<a>";
				result += "<span style = 'color:aqua'><a target=\"new\" href=\"" + sentimentlink + " = " + sentimentCatToCount.get(sentiment) + ", ";
			}
		}
		result += "<br><span style = 'color:red'>";
		try{
			if (peopleToCount != null){
			for (Contact ct :peopleToCount.keySet()){

					result += ct.pickBestName() + " = " + peopleToCount.get(ct) + ", "; //problem is with pickBestName
				}
			}
		}
		catch (Exception e){
			String exceptionString = ("EXCEPTION");
			result += exceptionString;
		}
		result += "<br><span style = 'color:blue'>";
		*/
		if (withWPLink) {
			String wpLink = "http://en.wikipedia.org/wiki/" + cleanTitle.replaceAll(" ", "_"); // wp needs _ instead of spaces, though spaces work also
			result += " <a target=\"new\" href=\"" + wpLink + "\"><img style=\"position:relative;top:3px\" src=\"images/wikipedia.png\"/></a>";
		}
		return result;
	}
}
