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
package edu.stanford.muse.exceptions;

/** exception when a mbox folder is not present or not readable */
public class MboxFolderNotReadableException extends Exception {
	private String displayMessage;
	public MboxFolderNotReadableException(String displayMessage)
	{
		this.displayMessage = displayMessage;
	}

	public String getMessage()
	{
		return this.displayMessage;
	}
}
