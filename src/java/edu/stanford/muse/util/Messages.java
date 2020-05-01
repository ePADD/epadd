package edu.stanford.muse.util;

import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.webapp.ModeConfig;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;


public class Messages {

	//static Locale local= new Locale("fr", "FR");


	public static String test(String archiveID, String bundleName, String key){
		Archive ar= ArchiveReaderWriter.getArchiveForArchiveID(archiveID);
	/*	Locale local= Locale.forLanguageTag(ar.collectionMetadata.ingestionLocaleTag); */

	Locale local;


	if(ModeConfig.isDiscoveryMode()) {
		if (ar != null)
			local = Locale.forLanguageTag(ar.collectionMetadata.ingestionLocaleTag);
		else
			local = Locale.getDefault();
	}
	else
		local=Locale.getDefault();

	/*
	if(ar!=null && (ModeConfig.isAppraisalMode() || ModeConfig.isDiscoveryMode() ) )
		local= Locale.forLanguageTag(ar.collectionMetadata.ingestionLocaleTag);
	else
		local= Locale.getDefault();
	 */

		return test(local, bundleName, key, null);

	}

	public static String test(Locale l, String bundleName, String key, Object []args){
		if (args == null)
			args = new Object[0]; // null is the same as no arg

		//Locale ingest= new Locale("France", "French");

		System.out.println("currentLocale = " + l.toString());

		ResourceBundle messages = ResourceBundle.getBundle(bundleName, l);

		MessageFormat formatter = new MessageFormat("");
		formatter.setLocale(l);

		formatter.applyPattern(messages.getString(key));
		return formatter.format(args);
	}

/*	public static  String getMessagee(String arch, String bundleName, String key) {
		Archive ar= ArchiveReaderWriter.getArchiveForArchiveID(arch);
		Locale ingest= new Locale(ar.collectionMetadata.ingestionLocaleTag);
		return getMessage(ingest, bundleName, key, null); //Locale.getDefault()
	}

	private static  String getMessagee(String arch, String bundleName, String key, Object[] args) {
		Archive ar= ArchiveReaderWriter.getArchiveForArchiveID(arch);
		Locale ingest= new Locale(ar.collectionMetadata.ingestionLocaleTag);
		return getMessage(ingest, bundleName, key, args); //Locale.getDefault()
	}
*/
	public static  String getMessage(String bundleName, String key) {
		return getMessage(Locale.getDefault(), bundleName, key, null); //Locale.getDefault()
	}

	private static  String getMessage(String bundleName, String key, Object[] args) {
		return getMessage(Locale.getDefault(), bundleName, key, args); //Locale.getDefault()
	}

	private static String getMessage(Locale locale, String bundleName, String key, Object[] args) {

		if (args == null)
			args = new Object[0]; // null is the same as no arg

		//Locale ingest= new Locale("France", "French");

		System.out.println("currentLocale 23 = " + locale.toString());

		ResourceBundle messages = ResourceBundle.getBundle(bundleName, locale);

		MessageFormat formatter = new MessageFormat("");
		formatter.setLocale(locale);

		formatter.applyPattern(messages.getString(key));
		return formatter.format(args);
	}

	static public void main(String[] args) {
		//System.out.println(getMessage("messages","browse-top.title-labels"));
		System.out.println(getMessage("messages", "template",  new Object[]{ "First Param", "Second Param", 7, new Date()}));
		//System.out.println(getMessage(Locale.GERMANY, "Messages", "template", new Object[]{ "Mars", " Param1", 7, new Date()}));
	}
} 
