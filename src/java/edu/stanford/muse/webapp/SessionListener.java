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
package edu.stanford.muse.webapp;

import java.util.Date;

import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import edu.stanford.muse.index.ArchiveReaderWriter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import edu.stanford.muse.index.Archive;
import edu.stanford.muse.util.Util;

public class SessionListener implements HttpSessionListener {
    public static Log log = LogFactory.getLog(JSPHelper.class);
    static {
    	log.info ("Initializing class edu.stanford.muse.webapp.SessionListener");
    }
    private int sessionCount = 0;

	public void sessionCreated(HttpSessionEvent event) {
		synchronized (this) {
			sessionCount++;
			log.info("Session Created: " + event.getSession().getId() + " at " + new Date());
			log.info("Current number of sessions: " + sessionCount);
		}
	}

	public void sessionDestroyed(HttpSessionEvent event) {
		log.info("Destroying session: " + event.getSession().getId() + " at " + new Date());
		HttpSession session = event.getSession();

		if (ModeConfig.isDiscoveryMode())
			log.info ("Not saving archive on session destroy because we're in discovery mode");
		else {
			// save the archive before quitting the session, so the annotations, flags, etc. can be saved
			Archive archive = (Archive) session.getAttribute("archive");
			if (archive != null)
				try {
					ArchiveReaderWriter.saveArchive(archive, Archive.Save_Archive_Mode.INCREMENTAL_UPDATE);
				} catch (Exception e) {
					Util.print_exception(e, log);
					return;
				}
		}
		synchronized (this) {
			sessionCount--;
			log.info("Current number of sessions: " + sessionCount);
		}
	}
}