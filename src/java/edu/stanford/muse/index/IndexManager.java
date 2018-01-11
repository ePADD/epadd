/*
 Copyright (C) 2012 The Stanford MobiSocial Laboratory

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package edu.stanford.muse.index;

import edu.stanford.muse.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/** this class for debugging (?) */
public class IndexManager {

	private String rootDir = "";

	public void saveFullTextIndex(String datasetName, Archive driver) throws IOException
	{
		String file = rootDir + File.separatorChar + datasetName + ".ft-index";
		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
		oos.writeObject(driver);
		oos.close();
	}

	public void saveIndex(String datasetName, Collection<Document> docs) throws IOException
	{
		String file = rootDir + File.separatorChar + datasetName + ".ft-index";
		ObjectOutputStream oos = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)));
		oos.writeObject(docs);
		oos.close();
	}

	public Archive getFullTextIndex (String datasetName)
	{
		Archive driver = null;
		String file = rootDir + File.separatorChar + datasetName + ".index";
		try {
			ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			driver = (Archive) ois.readObject();
			ois.close();
		} catch (Exception ioe) {
			System.out.println ("Dataset index file does not exist at : " + file);
		}
		return driver;
	}

	public Collection<Document> getIndex (String datasetName)
	{
		String file = rootDir + File.separatorChar + datasetName + ".index";
		Collection<Document> docs = null;
		try {
			ObjectInputStream ois = new ObjectInputStream(new GZIPInputStream(new FileInputStream(file)));
			docs = (Collection<Document>) ois.readObject();
			ois.close();
		} catch (Exception ioe) {
			System.out.println ("Dataset index file does not exist at : " + file);
		}
		return docs;
	}

	private List<String> getAllFilesWithSuffix(String suffix)
	{
		File files[] = new File(rootDir).listFiles();
		List<String> list = new ArrayList<>();
		for (File f: files)
		{
			if (f.getName().endsWith(suffix))
				list.add(f.getName().substring(0, f.getName().length()-suffix.length()));
		}
		return list;
	}

	public Pair<List<String>, List<String>> getAllDataSets()
	{
		List<String> ftDatasets = getAllFilesWithSuffix(".ft-index");
		List<String> datasets = getAllFilesWithSuffix(".index");
		return new Pair<>(datasets, ftDatasets);
	}
}
