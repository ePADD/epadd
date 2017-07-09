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
package edu.stanford.muse.util;
import java.util.*;

class UnionFindBox<T> extends UnionFindObject {

	private T payload;
	public int classNum;
	
	public UnionFindBox(T t)
	{
		payload = t;
	}
	
	/** returns #of distinct equiv. classes */
	public static <T> int assignClassNumbers(Collection<UnionFindBox<T>> c)
	{
		// unifications are done, now assign cluster #s to the DisplayTerm object
		int runningClassNum = 0;

		// assign class nums for elements that have themselves as reps
		for (UnionFindBox<T> u : c)
		{
			// if rep is itself, it's a new class
			if (u.find() == u)
				u.classNum = runningClassNum++;
		}
		
		// assign class nums for elements that have reps other than themselves
		for (UnionFindBox<T> u : c)
		{
			UnionFindBox<T> rep =  (UnionFindBox<T>) u.find();
			u.classNum = rep.classNum;
		}

		return runningClassNum;
	}
}
