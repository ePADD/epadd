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


package edu.stanford.muse.wpmine;

import java.io.Serializable;

public class Pair<N, V> implements Serializable {
	private N first;
	private V second;

	public Pair(N name, V value) {
		this.first = name;
		this.second = value;
	}

	public void setFirst(N first) {
		this.first = first;
	}

	public N getFirst() {
		return first;
	}

	public void setSecond(V second) {
		this.second = second;
	}

	public V getSecond() {
		return second;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Pair) {
			Pair pair = (Pair) obj;
			return (this.getFirst().equals(pair.getFirst()) && this.getSecond().equals(pair.getSecond()));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.getFirst().hashCode() ^ this.getSecond().hashCode();
	}

	public String toString()
	{
		return "PAIR<" + this.getFirst() + " -- " + this.getSecond()+ ">";
	}

}
