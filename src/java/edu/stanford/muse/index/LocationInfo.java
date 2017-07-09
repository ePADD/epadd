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

import edu.stanford.muse.util.Util;

public class LocationInfo {
	public String locationName, lat, longi;
	public LocationInfo (String locationName, String lat, String longi)
	{
		this.locationName = locationName;
		this.lat = lat;
		this.longi = longi;
	}
	
	public String toString()
	{
		return Util.fieldsToString(this);
	}
}
