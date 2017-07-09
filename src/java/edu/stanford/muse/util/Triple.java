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
import java.io.Serializable;

public class Triple<C1,C2,C3> implements Serializable  {

	public C1 first;
	public C2 second;
	public C3 third;

	public Triple(C1 c1, C2 c2, C3 c3) {
		this.first = c1;
		this.second = c2;
		this.third = c3;
	}

	public void setFirst(C1 first) {
		this.first = first;
	}

	public C1 getFirst() {
		return first;
	}

	public C1 first() { return first; }
	public C2 second() { return second; }
	public C3 third() { return third; }
	
	public void setSecond(C2 second) {
		this.second = second;
	}

	public C2 getSecond() {
		return second;
	}

	public void setThird(C3 third) {
		this.third = third;
	}

	public C3 getThird() {
		return third;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof Triple) {
			Triple<C1,C2,C3> t = (Triple) obj;
			return (this.getFirst().equals(t.getFirst()) && 
					this.getSecond().equals(t.getSecond()) && 
					this.getThird().equals(t.getThird()));
		}
		return false;
	}

	@Override
	public int hashCode() {
		return this.getFirst().hashCode() ^ this.getSecond().hashCode() ^ this.getThird().hashCode();
	}
	
	public String toString()
	{
		return "Triple<" + this.getFirst() + " -- " + this.getSecond()+ " -- " + this.getThird() + ">";
	}
}
