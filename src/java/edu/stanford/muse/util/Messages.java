package edu.stanford.muse.util;

import java.text.MessageFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {

	public static  String getMessage(String bundleName, String key) {
		return getMessage(Locale.getDefault(), bundleName, key, null);
	}

	public static  String getMessage(String bundleName, String key, Object[] args) {
		return getMessage(Locale.getDefault(), bundleName, key, args);
	}

	public static String getMessage(Locale locale, String bundleName, String key, Object[] args) {

		if (args == null)
			args = new Object[0]; // null is the same as no arg
			
		System.out.println("currentLocale = " + locale.toString());

		ResourceBundle messages = ResourceBundle.getBundle(bundleName, locale);

		MessageFormat formatter = new MessageFormat("");
		formatter.setLocale(locale);

		formatter.applyPattern(messages.getString(key));
		return formatter.format(args);
	}

	static public void main(String[] args) {
		System.out.println(getMessage("messages", "template",  new Object[]{ "First Param", "Second Param", 7, new Date()}));
		System.out.println(getMessage(Locale.GERMANY, "Messages", "template", new Object[]{ "Mars", " Param1", 7, new Date()}));
	}
} 
