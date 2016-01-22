package edu.stanford.epadd.util;

import java.io.InputStream;

import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;

public class NLPUtils {
	public static SentenceDetectorME	sentenceDetector;
	static {
		InputStream SentStream = edu.stanford.muse.Config.getResourceAsStream("models/en-sent.bin");
		SentenceModel model = null;
		try {
			model = new SentenceModel(SentStream);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("Exception in init'ing sentence model");
		}
		sentenceDetector = new SentenceDetectorME(model);
	}

	public static String[] SentenceTokenizer(String text) {
		return sentenceDetector.sentDetect(text);
	}
}
