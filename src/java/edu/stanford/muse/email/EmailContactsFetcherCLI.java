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

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;

/** Usage: EmailContactsFetcherCLI [-T protocol] [-H host] [-p port] [-U userName] [-i identity]");
*/
public class EmailContactsFetcherCLI {

static String protocol = "imaps";
static String host = "imap.gmail.com";
static int port = -1;
static String user = "hangal";
static String password = null;
static String identityName = null;
static String alternateEmailAddrs = null;
static String folder = "[Gmail]/Starred";
static int N_THREADS = 1; // max # of threads used to access mailbox

@SuppressWarnings("unused")
private static void parseOptions(String[] args)
{
    int optind;
    for (optind = 0; optind < args.length; optind++) {
    if (args[optind].equals("-T")) {
	protocol = args[++optind];
    } else if (args[optind].equals("-a")) {
	alternateEmailAddrs = args[++optind];
    } else if (args[optind].equals("-t")) {
	try { N_THREADS = Integer.parseInt(args[++optind]); } catch (NumberFormatException nfe) { /* */ }
    } else if (args[optind].equals("-H")) {
	host = args[++optind];
    } else if (args[optind].equals("-U")) {
	user = args[++optind];
    } else if (args[optind].equals("-P")) {
	password = args[++optind];
    } else if (args[optind].equals("-f")) {
	folder = args[++optind];
    } else if (args[optind].equals("-i")) {
	identityName = args[++optind];
    } else if (args[optind].equals("-p")) {
	port = Integer.parseInt(args[++optind]);
    } else if (args[optind].equals("--")) {
	optind++;
	break;
    } else if (args[optind].startsWith("-")) {
	System.out.println(
"Usage: EmailContactsFetcherCLI [-T protocol] [-H host] [-p port] [-U userName] [-i identity] [-t #threads] -a [alternate email addresses]");
	System.out.println(
"\t[-P password] [-f mailbox] [msgnum] [-v] [-D] [-s] [-S] [-a]");
	System.out.println(
"or     EmailContactsFetcherCLI -m [-v] [-D] [-s] [-S] [-f msg-file]");
	System.exit(1);
    } else {
	break;
    }
    }

    // read password from .pass file if not supplied on cmd-line
    if (password == null && !"mstor".equals(protocol))
    {
        try {
            LineNumberReader lnr = new LineNumberReader(new FileReader(".pass"));
            password = lnr.readLine();
            lnr.close();
        } catch (IOException ioe) {
        	System.out.println ("IOException while reading password " + ioe);
        	throw new RuntimeException(ioe);
        }
    }
}
}
