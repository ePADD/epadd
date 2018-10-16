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

/** mainly for epadd modes. Needs to be simplified, and rolled into Config. */
public class ModeConfig
{
	public static Mode mode = Mode.APPRAISAL;

	// default: appraisal mode.
	// server_mode: not currently used in epadd
	public enum Mode {
		SERVER_MODE, APPRAISAL, PROCESSING, DELIVERY, DISCOVERY
	}

	// TODO: probably need more dimensions. e.g., read-only vs. read-write, full
	// archive vs. restricted archive, server-hosted vs desktop

	static {
		// note: this will be overridden by epadd.properties
		if (System.getProperty("muse.mode.public") != null || System.getProperty("epadd.mode.discovery") != null)
			mode = Mode.DISCOVERY;
		else if (System.getProperty("muse.mode.server") != null)
			mode = Mode.SERVER_MODE;
		else if (System.getProperty("muse.mode.admin") != null || System.getProperty("epadd.mode.processing") != null)
			mode = Mode.PROCESSING;
		else if (System.getProperty("epadd.mode.delivery") != null)
			mode = Mode.DELIVERY;
	}


	public static boolean isPublicMode() { return mode == Mode.DISCOVERY; }
	public static boolean isDiscoveryMode() { return mode == Mode.DISCOVERY; }

	public static boolean isAppraisalMode() { return mode == Mode.APPRAISAL; }
	public static boolean isProcessingMode() { return mode == Mode.PROCESSING; }
	public static boolean isDeliveryMode() {return mode == Mode.DELIVERY;}

	public static boolean isServerMode()
	{
		return mode == Mode.SERVER_MODE;
	}
	public static boolean isMultiUser()
	{
		return isPublicMode() || isServerMode();
	}

	public static String getModeForDisplay() {
		String mode="";
		if(ModeConfig.isAppraisalMode())
			mode="Appraisal";
		else if(ModeConfig.isProcessingMode())
			mode="Processing";
		else if(ModeConfig.isDeliveryMode())
			mode="Delivery";
		else if(ModeConfig.isDiscoveryMode())
			mode="Discovery";
		return mode;
	}
}