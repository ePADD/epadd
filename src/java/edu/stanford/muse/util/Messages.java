package edu.stanford.muse.util;

import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.webapp.ModeConfig;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;


public class Messages {

	private static final Logger log =  LogManager.getLogger(Messages.class);

	//static Locale local= new Locale("fr", "FR");


	/**
	 *
	 * @param archiveID archive ID from which data of the archive can be accessed
	 * @param bundleName the bundle name that is used (Here- Messages and Help)
	 * @param key the key present in the properties file to access its value
	 * @return return the string corresponding to the bundleName and key
	 */
	public static String getMessage(String archiveID, String bundleName, String key)
	{
		Archive ar= ArchiveReaderWriter.getArchiveForArchiveID(archiveID);


	Locale locale;

	/* Use collection specifice language/locale only in the discovery mode - for now. For other modes just use system's default locale*/
	if(ModeConfig.isDiscoveryMode()) {
		if (ar != null)
			locale = Locale.forLanguageTag(ar.collectionMetadata.getIngestionLocaleTag());
		else /*For the case when archive isn't loaded yet in the discovery mode. In that case continue to use system's default locale*/
			locale = Locale.getDefault();
	}
	else
		locale = Locale.getDefault();
		return getMessage(locale, bundleName, key, null);

	}

	/**
	 *
	 * @param locale Locale to be used to get the value.
	 * @param bundleName
	 * @param key  the key for which the value needs to be returned.
	 * @param args the arguments to replace in the value obtained for the key. Not used and tested for now.
	 * @return
	 */
	public static String getMessage(Locale locale, String bundleName, String key, Object []args){
		if (args == null)
			args = new Object[0]; // null is the same as no arg

		//Locale ingest= new Locale("France", "French");

		//log.info("currentLocale = " + locale.toString());		//Used to know current locale
		ResourceBundle messages = ResourceBundle.getBundle(bundleName, locale);

		MessageFormat formatter = new MessageFormat("");
		formatter.setLocale(locale);

		formatter.applyPattern(messages.getString(key));
		return formatter.format(args);
	}



	static public void main(String[] args) {
		//System.out.println(getMessage("messages","browse-top.title-labels"));
		System.out.println(getMessage(Locale.getDefault(),"messages", "browse-top.title-labels",null));
		//System.out.println(getMessage(Locale.GERMANY, "Messages", "template", new Object[]{ "Mars", " Param1", 7, new Date()}));
	}
}
