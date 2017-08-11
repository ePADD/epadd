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
package edu.stanford.muse.email;

import edu.stanford.muse.util.JSONUtils;

public class StaticStatusProvider implements StatusProvider {
    private static final long serialVersionUID = 1L;

	private String message = "No message";
	private boolean isCancelled = false;

	public StaticStatusProvider(String mesg)
	{
		this.message = mesg;
	}

	public String getStatusMessage() {
		return JSONUtils.getStatusJSON(this.message);
	}

	public void cancel() {isCancelled = true; }
	public boolean isCancelled() { return isCancelled; }
}
