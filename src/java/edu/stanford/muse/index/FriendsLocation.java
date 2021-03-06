package edu.stanford.muse.index;

import java.util.ArrayList;
import java.util.List;

import edu.stanford.muse.util.Pair;

/** test q. for interview
 * @exclude*/

public class FriendsLocation {

	private static final int N = 10;

	/** finds best locations for input.size() friends to meet */
	private static Pair<Integer, Integer> findBestLocation(List<Pair<Integer, Integer>> input)	
	{
		List<Integer> xs = new ArrayList<>(), ys = new ArrayList<>();
		for (Pair<Integer,Integer> p: input)
		{
			xs.add(p.getFirst());
			ys.add(p.getSecond());			
		}
		return new Pair<>(findMedian(xs), findMedian(ys));
	}
	
	/** returns median of the input */
	private static int findMedian(List<Integer> input)
	{
		int k = input.size();
		int counts[] = new int[N];
		for (int x: input)
			counts[x]++;
		
		// count up to N/2
		int cumulative_count = 0;
		for (int i = 0; i < counts.length; i++)
		{
			cumulative_count += counts[i];
			if (cumulative_count > k/2)
				return i;
		}
		
		throw new RuntimeException();
		// should not reach here
	}
	
	public static void main (String args[])
	{
		List<Pair<Integer, Integer>> input = new ArrayList<>();
		input.add(new Pair<>(1, 1));
		input.add(new Pair<>(2, 3));
		input.add(new Pair<>(3, 5));
		input.add(new Pair<>(4, 0));
		Pair<Integer, Integer> result = findBestLocation(input);
		System.out.println ("The best place to meet is: " + result.getFirst() + ", " + result.getSecond());
	}
}
