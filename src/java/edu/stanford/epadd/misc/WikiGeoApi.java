package edu.stanford.epadd.misc;

import java.util.*;

import com.google.gson.Gson;

import edu.stanford.muse.util.Pair;
import edu.stanford.muse.util.Triple;

/**
 * Descriptor class for data from Wikipedia Geo api.
 */
public class WikiGeoApi {
	static class Thumbnail {
		String	source;
		Integer	width, height;

		public Thumbnail() {
		}
	}

	static class Coordinate {
		Double	lat, lon;

		public Coordinate() {
		}
	}

	// of the type
	static class Hit {
		String				pageid;		// 2926943
		String				ns;
		String				title;
		Thumbnail			thumbnail;
		String				page_image;
		List<Coordinate>	coordinates;

		public Hit() {
		}
	}

	static class Query {
		Map<String, Hit>	pages;

		public Query() {
		}
	}

	/** No idea what this field is doing and hence named some. */
	static class Some {
		Integer	coordinates;

		public Some() {
		}
	}

	/*
	 * The response json looks like this:
	 * {"query":{"pages":{"29307656":{"pageid":29307656,"ns":0,"title":"Adan Dam"
	 * ,"thumbnail":{"source":
	 * "http://upload.wikimedia.org/wikipedia/commons/thumb/1/14/India_Maharashtra_location_map.svg/180px-India_Maharashtra_location_map.svg.png"
	 * ,
	 * "width":180,"height":139},"pageimage":"India_Maharashtra_location_map.svg"
	 * ,
	 * "coordinates":[{"lat":20.4215,"lon":77.5631,"primary":"","globe":"earth"}
	 * ]}}},"limits":{"coordinates":500}}
	 */
	Query	query;
	Some	limits;

	public static Double calculateDistance(Pair p1, Pair p2) {
		Double lon2 = (Double) p2.getSecond(), lat2 = (Double) p2.getFirst();
		Double lon1 = (Double) p1.getSecond(), lat1 = (Double) p1.getFirst();

		double R = 6373;
		double dlon = lon2 - lon1, dlat = lat2 - lat1;
		double a = Math.pow(Math.sin(dlat / 2.0), 2) + Math.cos(lat1) * Math.cos(lat2) * Math.pow((Math.sin(dlon / 2.0)), 2), c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double d = R * c;
		return d;
	}

	/**
	 * Returns list of Triple<latitude,longitude,Title of the hits>
	 */
	public static List<Pair<String, Double>> getInfo(WikiGeoApi wa, String lats, String lons) {
		Double lat = 0.0, lon = 0.0;
		try {
			lat = Double.parseDouble(lats);
			lon = Double.parseDouble(lons);
		} catch (Exception e) {
			System.err.println("Exception while parsing: " + lat + ", " + lon);
			e.printStackTrace();
		}
		List<Pair<String, Double>> pairs = new ArrayList<Pair<String, Double>>();
		if (wa != null && wa.query != null && wa.query.pages != null && wa.query.pages != null) {
			Map<String, Hit> hits = wa.query.pages;
			for (Hit h : hits.values()) {
				List<Coordinate> cos = h.coordinates;
				if (cos.size() > 0) {
					double dist = calculateDistance(new Pair<Double, Double>(cos.get(0).lat, cos.get(0).lon), new Pair<Double, Double>(lat, lon));
					pairs.add(new Pair<String, Double>(h.title, dist));
				}
			}
		}
		return pairs;
	}

	public static void main(String[] args) {
		String test = "{\"query\":{\"pages\":{\"1978685\":{\"pageid\":1978685,\"ns\":0,\"title\":\"Buffalo Bayou\",\"thumbnail\":{\"source\":\"http://upload.wikimedia.org/wikipedia/en/thumb/8/86/Buffalo_Bayou_as_it_passes_by_Houston%27s_Memorial_Park%2C_May_2014.jpg/180px-Buffalo_Bayou_as_it_passes_by_Houston%27s_Memorial_Park%2C_May_2014.jpg\",\"width\":180,\"height\":134},\"pageimage\":\"Buffalo_Bayou_as_it_passes_by_Houston's_Memorial_Park,_May_2014.jpg\",\"coordinates\":[{\"lat\":29.7638,\"lon\":-95.0816,\"primary\":\"\",\"globe\":\"earth\"}]},\"17131101\":{\"pageid\":17131101,\"ns\":0,\"title\":\"Lynchburg Ferry\",\"coordinates\":[{\"lat\":29.7635,\"lon\":-95.08,\"primary\":\"\",\"globe\":\"earth\"}]}}},\"limits\":{\"coordinates\":500}}";
		Gson gson = new Gson();
		WikiGeoApi wg = gson.fromJson(test, WikiGeoApi.class);
		Map<String, Hit> hits = wg.query.pages;
		if (hits != null) {
			for (Hit h : hits.values())
				System.err.println(h.title);
		}
		System.err.println(wg.limits.coordinates);
	}
}
