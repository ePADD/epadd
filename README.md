**ePADD (Email: Process Appraise, Discover, Deliver)** 

https://library.stanford.edu/projects/epadd

**Introduction**

ePADD is a software package developed by Stanford University's Special Collections & University Archives that supports archival processes around the appraisal, ingest, processing, discovery, and delivery of email archives.

The software is comprised of four modules:

Appraisal: Allows donors, dealers, and curators to easily gather and review email archives prior to transferring those files to an archival repository.

Processing: Provides archivists with the means to arrange and describe email archives.

Discovery: Provides the tools for repositories to remotely share a redacted view of their email archives with users through a web server discovery environment.

Delivery: Enables archival repositories to provide moderated full-text access to unrestricted email archives within a reading room environment.

**System Requirements**

OS: Windows 7 SP1 / 8.1, Mac OS X  10.9 / 10.10
Memory:  4096 MB RAM (2048 MB RAM allocated to the application by default)
Browser:  Chrome 41/42, Firefox 38/39 
Windows installations: Java Runtime Environment 7u55 or later required. 

**Installation** 

ePADD has been tested on and optimized for Windows 7 SP1 / 8.1 and  Mac OS X  10.9 / 10.10. Please follow the instructions below for your operating system.

*Installing ePADD on Windows*

Please download the latest ePADD distribution *.exe file from https://github.com/epadd. You will need to have Java 1.7 or later installed on your machine for ePADD to work properly.
When you run ePADD for the first time, a directory for the Appraisal Module is created to store working files. When ePADD starts up, it checks this directory and relies upon it to resume earlier work.  If the software does not locate this directory, ePADD will create it.  The ePADD Appraisal Module directory is located at c:\users\<username>\epadd-appraisal. 
In order for functionality for authority reconciliation to function correctly, you must also separately download the reconciliation features (epadd-settings.zip), accessible via https://github.com/epadd. Once downloaded, unzip this file into your user directory (c:\users\<username>\).
Note: This directory houses many customizable files discussed further elsewhere in the user guide, and also includes a file, “config.PROPERTIES,” which can be edited to update the contact information for your repository’s Administrator in case of system errors. 
Depending on your network permissions, you may be asked to allow ePADD access to your internet connection. ePADD requires an internet connection to download email from an email account using the IMAP protocol.
Upon running ePADD, the application icon  will appear in the Windows Taskbar. Right-click on this icon at any point to open an ePADD window or to quit ePADD.

*Installing ePADD on OSX*

Please download the latest ePADD distribution *.dmg file from https://github.com/epadd.
When you run ePADD for the first time, a directory for the Appraisal Module is created to store working files. When ePADD starts up, it checks this directory and relies upon it to resume earlier work.  If the software does not locate this directory, ePADD will create it.  The ePADD Appraisal Module directory is located at Macintosh HD/Users/<username>/epadd-appraisal.
In order for functionality related to authority reconciliation to function correctly, you must also separately download the reconciliation features (epadd-settings.zip), accessible via https://github.com/epadd. Once downloaded, unzip this file into your user directory (Macintosh HD\Users\<username>\). 
This directory houses many customizable files discussed further elsewhere in the user guide, and also includes a file, “config.properties,” which can be edited to update the contact information for your repository’s Administrator in case of system errors. 
Depending on upon your network permissions, you may be asked to allow ePADD access to your internet connection. ePADD requires an internet connection to download email from an email account using the IMAP protocol.
In Mac OSX, the application icon  will appear in the OSX Finder Toolbar. Right-click on this icon at any point to open an ePADD window or to quit ePADD.


**ePADD Documentation, Help, and other Information**

More information about the software and related developments, including links to the full installation and user guide, and a link to the community forums, can be found via our website.

**License(s)**

The ePADD logo, project documentation (including installation and user guide), and other non-software products of the ePADD team are subject to the Creative Commons Attribution 2.0 Generic license (CC By 2.0).
Unless otherwise indicated, software items in this repository are distributed under the terms of the Apache License 2.0.

**Credits**

*Research and Development:*
Sudheendra Hangal, Ashoka University and Amuse Labs;
Vihari Piratla, Amuse Labs;
Sit Manovit, iXora Inc.;
Peter Chan, Stanford University Libraries;
Glynn Edwards, Stanford University Libraries;
Josh Schneider, Stanford University Libraries

*Design:*
Saumya Sarangi, Lollypop Design;
Mandeep RJ, Lollypop Design

*Initial specifications (requirements and wireframes):*
Daniel Hartwig, Stanford University Libraries;
Daniel Jarvis, Hoover Institute, Stanford;
Lisa Miller, Hoover Institute, Stanford;
Aimee Morgan, formerly Stanford University Libraries;
Laura O'Hara, formerly SLAC National Accelerator Laboratory

*Testing and Collaboration:*
Donald Mennerich, New York University Libraries;
Susan Thomas, Bodleian Library, Oxford University;
Riccardo Ferrante and Lynda Schmitz Fuhrig, Smithsonian Institution Archives;
Terry Catapano, Stephen Davis, and Dina Sokolova, Columbia University

*Advisors:*
Jeremy Leighton John, British Library;
Monica S. Lam, Stanford University;
Phillip R. Malone, Stanford University;
Pam Maples, Stanford University;
Meg McAleer, Library of Congress;
Chris Prom, University of Illinois;
Ben Shneiderman, University of Maryland;
Jeff Ubois, Macarthur Foundation

*Funding:*
National Historical Publications & Records Commission;
Payton J. Treat Fund from the Stanford University Libraries ;
U.S. National Science Foundation (for Muse);
Mobisocial Laboratory at Stanford University (for Muse);

*Software:*

Under Apache License 2.0:
Apache Commons (fileupload, lang3, io, httpclient, cli, codec), tika, opennlp, tomcat, maven, ant © Apache Software Foundation
Muse © Mobisocial Laboratory, Stanford University 
Google Gson © Google
OpenCSV © Glen Smith and others 
Java SE Platform Products © Oracle - Oracle BCL
Launch4j © Grzegorz Kowal BSD 3-clause license and MIT license 
Font Awesome © Dave Gandy MIT license, SIL OFL 1.1
Jsoup 

Cooliris Image wall (Special thanks to Soujanya Bhumkar, Austin Shoemaker and team for making the software available to us)
Copyright (c) 2015 Yahoo, Inc. This software is provided 'as-is', without any express or implied warranty. In no event will the authors be held liable for any damages arising from the use of this software. Permission is granted to anyone to use this software for any purpose, including commercial applications, and to alter it and redistribute it freely, subject to the following restrictions: 1. The origin of this software must not be misrepresented; you must not claim that you wrote the original software. If you use this software in a product, an acknowledgement in the product documentation would be appreciated but is not required. 2. Altered source versions must be plainly marked as such, and must not be misrepresented as being the original software. 3. This notice may not be removed or altered from any source distribution.

LIBSVM 
Copyright (c) 2000-2014 Chih-Chung Chang and Chih-Jen Lin All rights reserved. Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 3. Neither name of copyright holders nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Mstor
Copyright (c) 2007, Ben Fortuna All rights reserved. Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of Ben Fortuna nor the names of any other contributors may be used to endorse or promote products derived from this software without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
