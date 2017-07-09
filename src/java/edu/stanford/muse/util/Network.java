package edu.stanford.muse.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class Network {
    public static String getContentFromURL(URL url) {
        try {
            // get URL content
            URLConnection conn = url.openConnection();

            // open the stream and put it into BufferedReader
            BufferedReader br = new BufferedReader(new InputStreamReader(
                    conn.getInputStream()));

            String html = "";
            String inputLine = "";
            while ((inputLine = br.readLine()) != null) {
                html += inputLine;
            }
            br.close();
            return html;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
