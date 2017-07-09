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

import java.io.Serializable;

class ImapPopConnectionOptions implements Serializable {

	private static final long serialVersionUID = 1L;
	public String protocol, server, userName;
	public transient String password;
	public int port;
	
	/** sometimes we will wipe passwords. in that case the password string is set to this special string so we'll know that it has been wiped. */
	private static final String WIPED_PASSWORD = "WIPED.PASSWORD";
	
	public ImapPopConnectionOptions(String protocol, String server, int port, String userName, String password)
	{
		this.protocol = protocol;
		this.server = server;
		this.port = port;
		this.userName = userName;
		this.password = password;
	}
	
	public void wipePasswords() { 
		//this.password = WIPED_PASSWORD;
	}
	
	public String toString()
	{
		return "protocol=\"" + this.protocol + "\" " +
		"server=\"" + this.server + "\" " +
		"port=\"" + this.port + "\" " +
		"userName=\"" + this.userName + "\" " + 
		(WIPED_PASSWORD.equals(password) ? "password not stored" : "");
	}
}
