**ePADD (Email: Process Appraise, Discover, Deliver)** 

https://openpreservation.org/tools/epadd/

**Introduction**

ePADD is a software package that supports archival processes around the appraisal, ingest, processing, discovery, and delivery of email archives.
ePADD was originally developed by Stanford University's Special Collections & University Archives and is currently hosted and maintained by the [Open Preservation Foundation](https://openpreservation.org/).

The software is comprised of four modules:

Appraisal: Allows donors, dealers, and curators to easily gather and review email archives prior to transferring those files to an archival repository.

Processing: Provides archivists with the means to arrange and describe email archives.

Discovery: Provides the tools for repositories to remotely share a redacted view of their email archives with users through a web server discovery environment. (Note that this module is downloaded separately).

Delivery: Enables archival repositories to provide moderated full-text access to unrestricted email archives within a reading room environment.

**System Requirements**

64-bit Windows 10/11, macOS or Ubuntu 24.04 or later, at least 8 GB of RAM, and a current version of Chrome or Firefox. The installers include Java; `epadd.exe` and the standalone jar need Java 11 or later. See [System Requirements](docs/manual/README.md#21-system-requirements) in the manual for details, including how much memory ePADD uses.

**Installation**

Download ePADD from the [releases page](https://github.com/ePADD/epadd/releases). The "Which file do I need?" table at the top of each release names the right file for your computer.

* **Windows:** double-click the `.msi` file (`windows-x86_64`, or `windows-arm64` for Arm processors such as Snapdragon). No administrator rights are needed. [More](docs/manual/README.md#221-installing-epadd-on-windows)
* **macOS:** open the `.dmg` file (`macos-arm64` for Apple silicon, `macos-x86_64` for Intel) and drag ePADD into Applications. [More](docs/manual/README.md#222-installing-epadd-on-macos)
* **Ubuntu:** `sudo apt install ./epadd-<version>-linux-x86_64.deb` [More](docs/manual/README.md#223-installing-epadd-on-linux)
* **Any system with Java 11 or later, without installing:** `java -jar epadd-<version>-standalone.jar`

If Windows or macOS warns about an unrecognised developer, the installation sections of the manual linked above explain how to continue. The files starting with `epadd-discovery-` are the [Discovery Module](docs/manual/README.md#5-discovery-module), which is installed in the same way.

The [ePADD Installation and User Guide](docs/manual/README.md) covers installation, settings and all features in full.

**ePADD Documentation, Help, and other Information**

More information about the software and related developments, including links to the full installation and user guide, as well as a link to the community forums, can be found via [our website] (https://openpreservation.org/tools/epadd/).

**License(s)**

The ePADD logo, project documentation (including installation and user guide), and other non-software products of the ePADD team are subject to the Creative Commons Attribution 2.0 Generic license (CC By 2.0).

Unless otherwise indicated, software items in this repository are distributed under the terms of the Apache License, Version 2.0 (the "License"); you may not use these files except in compliance with the License. You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.

*Funding:*
- Email Archives : Building Capacity and Community, Andrew W. Mellon Foundation (ePADD Phase 4);
- Andrew W. Mellon Foundation (ePADD Phase 3);
- David C. Weber Librarian's Research Fund from the Stanford University Libraries (ePADD Phase 3);
- Institute for Museum and Library Services (ePADD Phase 2);
- National Historical Publications & Records Commission (ePADD Phase 1);
- Payton J. Treat Fund from the Stanford University Libraries (ePADD Phase 1);
- U.S. National Science Foundation (Muse);
- Mobisocial Laboratory at Stanford University (Muse)

*Software:*

Under Apache License 2.0:
- Apache Commons (fileupload, lang3, io, httpclient, cli, codec), tika, opennlp, tomcat, maven, ant © Apache Software Foundation
- Muse © Mobisocial Laboratory, Stanford University 
- Google Gson © Google
- OpenCSV © Glen Smith and others 
- Java SE Platform Products © Oracle - Oracle BCL
- Launch4j © Grzegorz Kowal BSD 3-clause license and MIT license 
- Font Awesome © Dave Gandy MIT license, SIL OFL 1.1
- Jquery-autocomplete © Tomas Kirda under MIY-style license.
- Jsoup 

LIBSVM 
Copyright (c) 2000-2014 Chih-Chung Chang and Chih-Jen Lin All rights reserved. Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: 1. Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. 2. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. 3. Neither name of copyright holders nor the names of its contributors may be used to endorse or promote products derived from this software without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

Mstor
Copyright (c) 2007, Ben Fortuna All rights reserved. Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met: Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer. Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution. Neither the name of Ben Fortuna nor the names of any other contributors may be used to endorse or promote products derived from this software without specific prior written permission. THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

**Credits**

*Research and Development:*
- Sudheendra Hangal, Ashoka University and Amuse Labs;
- Chinmay Narayan, Amuse Labs;
- Vihari Piratla, Amuse Labs;
- Sit Manovit, iXora Inc.;
- Peter Chan, Stanford University Libraries;
- Sally DeBauche, Stanford University Libraries;
- Glynn Edwards, Stanford University Libraries;
- Josh Schneider, Stanford University Libraries

*Design:*
- Saumya Sarangi, Lollypop Design;
- Mandeep RJ, Lollypop Design

*Advisory Board and Collaboration (Phase 3):*
- Stephen Abrams, Harvard University;
- Patricia Patterson, Harvard University;
- Ian Gifford, University of Manchester;
- Jessica Smith, University of Manchester;
- Jochen Farwer, University of Manchester

*Testing and Collaboration (Phase 2):*
- Elvia Arroyo-Ramirez, University of California, Irvine;
- Laura Uglean Jackson, University of California, Irvine;
- Skip Kendall, Harvard University;
- Margo Padilla, Metropolitan New York Library Council (METRO);
- Christopher Prom, University of Illinois at Urbana-Champaign;
- Audra Eagle Yun, University of California, Irvine

*Advisory Board (Phase 2):*
- Sherri Berger, California Digital Library;
- Andrew Byers, Duke University;
- Jackie Dooley, OCLC Research;
- Mike Giarlo, Stanford University;
- Marie Hicks, Illinois Institute of Technology;
- Peter Hirtle, Cornell University;
- Lise Jaillart, Loughborough University;
- Jeremy Leighton John, British Library;
- Cal Lee, University of North Carolina, Chapel Hill;
- Evelyn McLellan, Artefactual;
- Maria Matienzo, Digital Public Library of America;
- T. Christian Miller, ProPublica;
- Jessica Moran, National Library of New Zealand;
- David Rosenthal, Stanford University;
- Marc A. Smith, Social Media Research Foundation;
- Terry Winograd, Stanford University;
- Kam Woods, University of North Carolina, Chapel Hill

*Testing and Collaboration (Phase 1):*
- Donald Mennerich, New York University Libraries;
- Susan Thomas, Bodleian Library, Oxford University;
- Riccardo Ferrante and Lynda Schmitz Fuhrig, Smithsonian Institution Archives;
- Terry Catapano, Stephen Davis, and Dina Sokolova, Columbia University

*Advisory Board (Phase 1):*
- Jeremy Leighton John, British Library;
- Monica S. Lam, Stanford University;
- Phillip R. Malone, Stanford University;
- Pam Maples, Stanford University;
- Meg McAleer, Library of Congress;
- Chris Prom, University of Illinois;
- Ben Shneiderman, University of Maryland;
- Jeff Ubois, Macarthur Foundation

*Initial specifications (requirements and wireframes):*
- Daniel Hartwig, Stanford University Libraries;
- Daniel Jarvis, Hoover Institute, Stanford;
- Lisa Miller, Hoover Institute, Stanford;
- Aimee Morgan, formerly Stanford University Libraries;
- Laura O'Hara, formerly SLAC National Accelerator Laboratory

