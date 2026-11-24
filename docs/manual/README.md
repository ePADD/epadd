# ePADD Installation and User Guide

*Version 11.1.4 · November 2026*

**ePADD** is a software application that supports archival processes around the appraisal, ingest, processing, discovery, and delivery of email archives. It is open source and licensed under an Apache Public License, v2.0.

**ePADD**  is hosted and maintained by the Open Preservation Foundation (OPF) and collaboratively governed by the user community.

Funding for **ePADD** development has previously been provided by the Andrew W. Mellon Foundation, the Institute of Museum & Library Studies (IMLS) National Leadership Grant (NLG) for Libraries and the National Historical Publications and Records Commission (NHPRC).

**Document Revisions**

| Date | Version Number | Document Changes |
| :---- | :---- | :---- |
| 04/21/2014 | .01 | Initial Draft |
| 04/23/2014 | .02 | Guide updated to include Appraisal Module |
| 04/29/2014 | .03 | Guide updated with user comments |
| 09/09/2014 | .04 | Guide updated with module changes |
| 06/30/2015 | 1.0 | Guide updated for all modules |
| 07/10/2015 | 1.0.1 | Guide updated for all modules reflecting UI updates in 1.0.1 release |
| 08/10/2015 | 1.1RC3 | Guide updated for all modules reflecting new search functionality in 1.1 release |
| 02/01/2016 | 1.1 | Guide updated to reflect IMLS partner feedback |
| 07/15/2016 | 1.1 | Guide updated with announcement of 2.0 beta 1 release |
| 08/19/2016 | 2.0 | Guide updated for all modules reflecting new functionality in 2.0 release |
| 11/21/2016 | 2.1 | Guide updated reflecting new configuration file and other minor changes in 2.1 release |
| 02/27/2017 | 3.0 | Guide updated for all modules reflecting new functionality and UI in 3.0 release |
| 5/22/2017 | 3.1 | Guide updated reflecting minor updates in 3.1 release |
| 7/17/2017 | 4.0 | Guide updated for all modules reflecting new functionality in 4.0 release |
| 8/10/2017 | 4.0 | Guide updates with instructions for standing up Discovery Module on web server |
| 8/15/2017 | 4.1 | Guide updated for all modules reflecting minor fixes in 4.1 release |
| 11/13/2017 | 4.1 | Guide updated to reflect latest OS support |
| 1/22/2018 | 5.0 beta 1 | Guide updated for all modules reflecting new functionality in 5.0b1 release |
| 2/28/2018 | 5.0 | Guide updated for all modules reflecting updates in 5.0 release |
| 5/14/2018 | 5.1 | Guide updated for all modules reflecting small fixes in 5.1 release |
| 6/29/2018 | 6.0 beta 1 | Guide updated for all modules |
| 7/31/2018 | 6.0 | Guide updated for all modules reflecting fixes in 6.0 release |
| 9/10/2018 | 6.1 beta 1 | Guide updated for all modules reflecting small fixes in 6.1 beta release |
| 9/27/2018 | 6.1 | Guide updated for all modules reflecting small fixes in 6.1 release |
| 10/29/2018 | 7.0 beta 1 | Guide updated for all modules reflecting major changes in 7.0 release |
| 12/11/2018 | 7.0 beta 2 | Guide updated for all modules  reflecting major changes in 7.0 beta 2 release |
| 1/31/2019 | 7.0 | Guide updated for all modules reflecting major changes in 7.0 final release |
| 7/29/2019 | 7.1 | Guide updated for all modules reflecting changes in 7.1 release |
| 2/20/2020 | 7.2 | Guide updated for all modules reflecting changes in 7.2 release |
| 5/29/2020 | 7.3 beta | Guide updated to reflect major changes in 7.3 beta release |
| 6/29/2020 | 7.3 | Guide updated for all modules reflecting changes in 7.3 release |
| 1/15/2021 | 7.3.5 alpha | Guide updated for the Processing Module to reflect changes in the 7.3.5 alpha release |
| 4/8/2021 | 8 | Guide updated for all modules reflecting changes in 8 release |
| 10/5/2022 | 9 | Guide updated for the Appraisal Module and Processing Module to reflect changes in 9 release |
| 1/24/2023 | 10 | Guide updated for the Appraisal and Processing modules to reflect changes in the 10 alpha release |
| 3/1/2024 | 11 | Guide updated for the Appraisal and Processing modules to reflect changes in 11 release |
| 8/24/2026 | 11.1.3 | Guide updated to reflect changes in 11.1.3 release |
| 10/23/2026 | 11.1.4 | Guide updated to reflect changes in 11.1.4 release and reformatted for GitHub Markdown.&nbsp; |
| 11/24/2026 | 11.1.4 | Installation instructions updated for the Windows (.msi), macOS (.dmg) and Linux (.deb) installers with bundled Java, with direct download links |

&nbsp;

Previous Documentation:

[ePADD 10 User Documentation](https://docs.google.com/document/d/1PWzKyonRdogAChtHOQs-CVjCTMuQi00pLsHZRJateKw/edit)

[ePADD 9 User Documentation](https://docs.google.com/document/d/1Wx5RkvSKmZ0LZXMcK3wpbgWzHMRTigS3W0RNV8yY-Lk/edit)

[ePADD 8 User Documentation](https://docs.google.com/document/d/1CVIpWK5FNs5KWVHgvtWTa7u0tZjUrFrBHq6_6ZJVfEA/edit)

[ePADD 6.1 User Documentation](https://docs.google.com/document/d/1zh265uM0m16vUQhGh7iMMqv7q3A_mlQg-4lET0_diW8/edit?usp=sharing)

[ePADD 5.1 User Documentation](https://docs.google.com/document/d/1ZMuWU0z-IVsk80_lUEYMfVrwfCsS1bp0sjL28GBGcMU/edit)

[ePADD 4.1 user documentation](https://docs.google.com/document/d/1joUmI8yZEOnFzuWaVN1A5gAEA8UawC-UnKycdcuG5Xc)

**Table of Contents**

[**1 	Overview**](#1-overview)

[**2 	Getting Started**](#2-getting-started)

[2.1 		System Requirements](#21-system-requirements)

[2.2 		Installing ePADD](#22-installing-epadd)

[*2.2.1         Installing ePADD on Windows*](#221-installing-epadd-on-windows)

[*2.2.2         Installing ePADD on macOS*](#222-installing-epadd-on-macos)

[*2.2.3         Installing ePADD on Linux*](#223-installing-epadd-on-linux)

[*2.2.4         Compatibility Note*](#224-compatibility-note)

[2.3 		Running ePADD](#23-running-epadd)

[2.4 		Selecting a Module](#24-selecting-a-module)

[2.5 		Navigating ePADD](#25-navigating-epadd)

[**3	Appraisal Module**](#3-appraisal-module)

[3.1 	Importing email](#31-importing-email)

[*3.1.1	Emailchemy Integration*](#311-emailchemy-integration)

[*3.1.2	Sidecar Files*](#312-sidecar-files)

[3.2 	Reviewing Email](#32-reviewing-email)

[*3.2.1         Browsing Email Messages*](#321-browsing-email-messages)

[*3.2.2         Browsing All Messages*](#322-browsing-all-messages)

[*3.2.3         Editing Correspondents*](#323-editing-correspondents)

[*3.2.4         Performing Entity Analysis*](#324-performing-entity-analysis)

[*3.2.5         Performing Lexicon Analysis*](#325-performing-lexicon-analysis)

[*3.2.5.1         Editing a Default Lexicon*](#3251-editing-a-default-lexicon)

[*3.2.5.2         Reviewing Lexicon Analysis Results*](#3252-reviewing-lexicon-analysis-results)

[*3.2.6         Reviewing Regular Expressions*](#326-reviewing-regular-expressions)

[*3.2.7	         Reviewing Attachments*](#327-reviewing-attachments)

[*3.2.7.1         Reviewing Image Attachments*](#3271-reviewing-image-attachments)

[*3.2.7.2         Reviewing All Attachments*](#3272-reviewing-all-attachments)

[*3.2.8         Browsing Email Folders*](#328-browsing-email-folders)

[3.3 	Appraising Email](#33-appraising-email)

[*3.3.1         Reviewing and Creating Collection Metadata*](#331-reviewing-and-creating-collection-metadata)

[*3.3.2         Searching Email Messages*](#332-searching-email-messages)

[*3.3.2.1         Navigating Search Results*](#3321-navigating-search-results)

[*3.3.3    Annotating Email Messages*](#333-annotating-email-messages)

[*3.3.4       Labeling Email Messages*](#334-labeling-email-messages)

[*3.3.4.1         Assigning bulk labels*](#3341-assigning-bulk-labels)

[*3.3.4.2         Creating a new label*](#3342-creating-a-new-label)

[*3.3.4.3         Assigning bulk labels by correspondent*](#3343-assigning-bulk-labels-by-correspondent)

[*3.3.5         Assigning Images to the Collection*](#335-assigning-images-to-the-collection)

[3.4         Exporting Email for Preservation](#34-exporting-email-for-preservation)

[3.5](#35-exporting-message-attachments)         [Exporting Message Attachments](#35-exporting-message-attachments)

[3\.](#36-exporting-email-to-the-processing-module)6         [Exporting Email to the Processing Module](#36-exporting-email-to-the-processing-module)

[**4 	Processing Module**](#4-processing-module)

[4.1	Importing an Email Archive from the Appraisal Module into the Processing Module and Assigning Accession Metadata](#41-importing-an-email-archive-from-the-appraisal-module-into-the-processing-module-and-assigning-accession-metadata)

[4.2	Assigning Collection Metadata](#42-assigning-collection-metadata)

[4.3	Appraisal Revisited](#43-appraisal-revisited)

[4.4	Setting Default Labels](#44-setting-default-labels)

[4.5	Performing Authority Control](#45-performing-authority-control)

[*4.5.1*](#451-reconciling-correspondents-with-authorized-headings)         [*Reconciling Correspondents with Authorized Headings*](#451-reconciling-correspondents-with-authorized-headings)

[4.6 	Exporting Email for Preservation](#46-exporting-email-for-preservation)

[4.7](#47-downloadable-csv-of-search-results) 	Downloadable .CSV of Search Results

4.8	Exporting Message Headers

[4.9 	Exporting Message Attachments](#49-exporting-message-attachments)

[4.10	Exporting Entities](#410-exporting-entities)

[4.11	Exporting Correspondents](#411-exporting-correspondents)

[4.12	Exporting Original Text of Non-restricted Messages as Individual Text Files](#412-exporting-original-text-of-non-restricted-messages-as-individual-text-files)

[4.13 	Exporting Email to the Delivery and Discovery Modules](#413-exporting-email-to-the-delivery-and-discovery-modules)

[**5	Discovery Module**](#5-discovery-module)

[5.1](#51-testing-the-discovery-module) 	[Testing the Discovery Module](#51-testing-the-discovery-module)

[5.2	Mounting the Discovery Module under a Web Server](#52-mounting-the-discovery-module-under-a-web-server)

[5.3	Searching Entities Across Collections](#53-searching-entities-across-collections)

[5.4	Navigating to a Collection](#54-navigating-to-a-collection)

[5.5	Browsing Email](#55-browsing-email)

[5.6	Searching Email](#56-searching-email)

[**6 	Delivery Module**](#6-delivery-module)

[6.1 	Importing the Email Archive into the Delivery Module](#61-importing-the-email-archive-into-the-delivery-module)

[6.2 	Navigating to a Collection](#62-navigating-to-a-collection)

[6.3  	Browsing the Email Archive](#63-browsing-the-email-archive)

[6.4 	Searching the Email Archive](#64-searching-the-email-archive)

[6.5 	Labeling messages](#65-labeling-messages)

[**Appendix**](#appendix-epadd-data-flows)

[ePADD Data Flows](#appendix-epadd-data-flows)

# 1 Overview

Welcome to ePADD, the all-in-one email appraisal, processing, discovery, and delivery solution for donors, archival repositories, and researchers. This installation and user guide will assist the new ePADD user in making the most of the program.

ePADD consists of four modules – Appraisal, Processing, Discovery, and Delivery – which are designed to facilitate the process of working with email archives. The application runs in a user-friendly and familiar web browser interface:

**Appraisal:** Allows creators, dealers, and curators to easily gather and review email archives on a local server (to ensure privacy) prior to transferring those files to an archival repository.

**Processing:** Provides archivists with the means to arrange and describe email archives on a local server.

**Discovery:** Provides the tools for repositories to remotely share a redacted view of email archives with users through a public web server discovery environment.

**Delivery:** Enables archival repositories to provide moderated full-text access to unrestricted email archives within a reading room environment on a local server.

# 2 Getting Started

This section will get you started working with ePADD from installation through navigating the software platform.

## 2.1 System Requirements

**OS:** 64-bit Windows (x86\_64 or Arm), macOS (Apple silicon or Intel), Linux. The Debian package (.deb) requires Ubuntu 24.04 or later; on other Linux distributions use the standalone .jar.

**Memory:** We recommend at least 8 GB of RAM. Large email collections may require more memory, and ePADD generally performs better with more available memory.

The maximum amount of memory available to ePADD depends on how it is started:

* **Installers (.msi on Windows, .dmg on macOS, .deb on Linux):** Up to 75% of the computer's total RAM.  
* **Windows executable (epadd.exe):** Up to 75% of the available memory when ePADD starts, with a minimum configured limit of 4 GB (2 GB for the Discovery Module).  
* **Java (.jar):** Uses the Java Virtual Machine (JVM) default maximum heap size, typically around 25% of the memory available to Java, unless explicitly configured.

These values are limits, not memory reservations. ePADD only uses as much memory as it needs, up to the configured limit. The Windows minimum is a minimum configured limit, not a guarantee that this amount of physical memory is available.

Increasing the memory limit:

If ePADD runs slowly or reports an out-of-memory error:

1. If you use epadd.exe on Windows, close unnecessary applications and restart ePADD. The memory limit is calculated when ePADD starts, so freeing memory beforehand may increase the limit.  
2. If the problem persists, run ePADD using the .jar file and specify a maximum for the RAM using the *\-Xmx* option. This requires a separately installed Java (see **Java** below).

For example, to set the maximum to 12 GB, open a Command Prompt or terminal, navigate to the directory containing the standalone .jar file, and run:

*java \-Xmx12g \-jar epadd-11.1.4-standalone.jar*

Choose a value below the computer's total installed RAM, leaving sufficient memory for the operating system, web browser, and other applications

**Browser:**  Chrome 68 or later, Firefox 59 or later.

**Java:** The installers (.msi, .dmg and .deb) include their own Java runtime (Azul Zulu, Java 17), so Java does not need to be installed separately. The bundled runtime is used only by ePADD and does not change or interfere with any other Java installed on the computer. The exact bundled Java version is listed in the release notes of each release.

The Windows executable (epadd.exe) and the standalone .jar files do not include Java. To use them, a 64-bit Java Runtime Environment, version 11 or later, must be installed on the computer.

## 2.2 Installing ePADD

Please download the latest ePADD distribution files from [https\://github.com/ePADD/epadd/releases/](https://github.com/ePADD/epadd/releases/). Each release offers several files. The release notes contain a table, "Which file do I need?", that links to the right installer for each type of computer. In summary:

| Your computer | File to download |
| --- | --- |
| Windows (most PCs) | [epadd-11.1.4-windows-x86\_64.msi](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-windows-x86_64.msi) |
| Windows on Arm, for example Snapdragon laptops | [epadd-11.1.4-windows-arm64.msi](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-windows-arm64.msi) |
| Mac with Apple silicon (M1 or later) | [epadd-11.1.4-macos-arm64.dmg](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-macos-arm64.dmg) |
| Mac with an Intel processor | [epadd-11.1.4-macos-x86\_64.dmg](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-macos-x86_64.dmg) |
| Ubuntu 24.04 or later | [epadd-11.1.4-linux-x86\_64.deb](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-linux-x86_64.deb) |
| Any system with Java 11 or later, no installation | [epadd-11.1.4-standalone.jar](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-standalone.jar) |

The .msi, .dmg and .deb installers include their own Java runtime, so nothing else needs to be installed first (see Section [2.1: System Requirements](#21-system-requirements)). Each installer is built for one processor architecture, so please make sure to choose the file that matches your computer.

Files whose names begin with *epadd-discovery* are the corresponding installers and files for the Discovery Module (see the notes below). The *.war* files are for server installations.

Follow the instructions below for your operating system.

When upgrading ePADD, it may be necessary to remove the existing epadd-settings folder to ensure that updated configuration and supporting files are installed.&nbsp;

**Before deleting this folder, we recommend making a backup**. The folder may contain log files that can be useful for troubleshooting errors or crashes, as well as customised settings. If you encounter problems with ePADD, retain the log files, as they may help developers or support staff identify the cause.

The epadd-settings folder is located at:

On Windows: C:\\Users\\\<username\>\\epadd-settings  
Mac: /Users/\<username\>/epadd-settings

### 2.2.1 Installing ePADD on Windows

ePADD is available for Windows in two forms:

* **Windows installer (.msi), recommended.** Download [*epadd-11.1.4-windows-x86\_64.msi*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-windows-x86_64.msi), or [*epadd-11.1.4-windows-arm64.msi*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-windows-arm64.msi) for Windows computers with an Arm processor (for example laptops with a Snapdragon processor). If you are unsure, open *Settings \> System \> About* and check the *System type*: "x64-based processor" requires the x86\_64 file, "ARM-based processor" the arm64 file. The installer includes Java, so no separate Java installation is needed.  
* **Windows executable (epadd-\<version\>.exe).** This file runs without installation but does not include Java. **You will need to have a 64-bit Java Runtime Environment version 11 or later installed on your machine to use it.** It runs on both x86\_64 and Arm computers.

**Installing with the .msi installer**

1) Double-click the downloaded .msi file.

2) Follow the installation wizard. You can choose the installation folder or accept the default.

3) When the installation has finished, start ePADD from the *ePADD* folder in the Windows Start menu or from the ePADD shortcut on the desktop.

The installer installs ePADD for the current user only and does not require administrator rights. When you install a newer version with the .msi installer, it replaces the previously installed version. Your data and settings folders (see below) are not affected. To uninstall ePADD, use *Settings \> Apps \> Installed apps*.

If Windows shows a SmartScreen warning about an unrecognised publisher, choose *More info* and then *Run anyway*. The release notes state whether the Windows files of a release are code signed.

When you run ePADD for the first time, a directory for the Appraisal Module is created to store working files. When ePADD starts up, it checks this directory and relies upon it to resume earlier work.  If the software does not locate this directory, ePADD will create it.  The ePADD Appraisal Module directory is located at c:\\users\\\<username\>\\epadd-appraisal.

![][image1]

**Example of the custom configuration file**

Some settings can be configured using a custom configuration file, *epadd.properties*, which must be in plain text and created in the user’s home directory (typically C:\\users\\\<username\>\\). If the file is not present, ePADD will revert to the existing defaults. Editing this file is only necessary if you wish to change the defaults, for instance, if your default directory does not contain enough space.

1) Using a text editor, edit the existing epadd.properties file.

2) Add to it from the following list of properties.

**epadd.settings.dir=** The location of the epadd-settings folder. The default is C:\\users\\\<username\>\\epadd-settings. Note the use of the forward slash in the configuration file.

**epadd.base.dir=** The location of the ePADD module folders (epadd-appraisal, epadd-processing, epadd-discovery, and epadd-delivery). The default is  C:\\users\\\<username\>\\epadd-appraisal, etc. Note the use of the forward slash in the configuration file.

**epadd.mode=** Defines the default module upon opening ePADD. Value should be one of the exact strings, 'appraisal', 'processing', 'discovery', or 'delivery' (without quotes). Match is case insensitive. The default is appraisal.

3\) Save the file.

Depending on your network permissions, you may be asked to allow ePADD access to your internet connection. ePADD requires an internet connection to download email from an email account using the IMAP protocol.

Upon running ePADD, the application icon **![Macintosh HD:private:var:folders:5m:rh\_kt9gx601gl7lw5qyk54gr0000gq:T:TemporaryItems:epadd\_logo.png][image2]** will appear in the Windows Taskbar. If you do not see the icon, it may be hidden from view. Consult ‘Windows help & learning’ \- [https\://support.microsoft.com/en-gb/windows](https://support.microsoft.com/en-gb/windows) \- for more information. Right-click on the ePADD icon at any point to open an ePADD window or to quit ePADD.

**Note:** The Discovery Module is run through a separate distribution, available as an installer ([*epadd-discovery-11.1.4-windows-x86\_64.msi*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-windows-x86_64.msi) or [*epadd-discovery-11.1.4-windows-arm64.msi*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-windows-arm64.msi)) or as [*epadd-discovery-11.1.4-standalone.jar*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-standalone.jar), which requires Java 11 or later.

These files are accessible via [https\://github.com/ePADD/epadd/releases/](https://github.com/ePADD/epadd/releases/). Be careful not to use the standard ePADD application in a discovery environment. The ePADD discovery application should be used in a public discovery environment because it is more secure than the standard ePADD application. For example, it does not allow any modifications to the archive.

ePADD saves all data in folders in the C:\\users\\\<username\>\\ directory, unless you have defined a different location using the epadd.properties file. We recommend as best practice making periodic backups of all ePADD related folders in this directory, which may include: epadd-settings, epadd-appraisal, epadd-processing, epadd-discovery, and epadd-delivery.

**Note:** For information about ePADD's memory requirements and how to adjust the maximum memory available to the application, see Section 2.1: System Requirements.&nbsp;

**Note:** The user can optionally create and modify a named entity kill list to suppress text strings that ePADD has identified as entities from appearing as such within the software. To do so, create a text file titled 'kill.txt' in the ePADD-settings folder. Each line of the document should contain a single named entity. Save the file and restart ePADD. The entities you listed should be suppressed.

### 2.2.2 Installing ePADD on macOS

Please download the latest ePADD disk image (.dmg) from [https\://github.com/ePADD/epadd/releases/](https://github.com/ePADD/epadd/releases/). There are two different disk images for macOS, and you need the one that matches the processor of your Mac:

* [*epadd-11.1.4-macos-arm64.dmg*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-macos-arm64.dmg) for Macs with Apple silicon (M1, M2 or later).  
* [*epadd-11.1.4-macos-x86\_64.dmg*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-macos-x86_64.dmg) for Macs with an Intel processor.

Not sure which Mac you have? Open the Apple menu and choose *About This Mac*. If it shows "Chip" followed by Apple M1, M2 or later, you have Apple silicon. If it shows "Processor" with Intel, you have an Intel Mac.

The disk image includes Java, so no separate Java installation is needed.

**Installing with the .dmg file**

1) Double-click the downloaded .dmg file to open it.

2) Drag the ePADD application into the *Applications* folder.

3) Eject the disk image and start ePADD from the *Applications* folder or from Launchpad.

To upgrade, replace the ePADD application in the *Applications* folder with the new version. Your data and settings folders (see below) are not affected.

If macOS refuses to open ePADD because it cannot verify the developer, open *System Settings \> Privacy & Security*, scroll down to the message about ePADD and choose *Open Anyway*. The release notes state whether the macOS files of a release are signed and notarized.

When you run ePADD for the first time, a directory for the Appraisal Module is created to store working files. When ePADD starts up, it checks this directory and relies upon it to resume earlier work.  If the software does not locate this directory, ePADD will create it.  The ePADD Appraisal Module directory is located at/Users/\<username\>/epadd-appraisal.

**Example of the custom configuration file**

Some settings can be configured using a custom configuration file, *epadd.properties*, which must be plain text and created in the user’s home directory/Users/\<username\>/. If the file is not present, ePADD will revert to the existing defaults. Editing this file is only necessary if you wish to change the defaults, for instance, if your default directory does not contain enough space.

Follow the following steps to edit the configuration file.

1\) Using a text editor, edit the existing epadd.properties file.

2\) Add to it from the following list of properties:

**epadd.settings.dir=** The location of the epadd-settings folder. The default is \<HOME\>/epadd-settings.

**epadd.base.dir=** The location of the ePADD module folders (epadd-appraisal, epadd-processing, epadd-discovery, and epadd-delivery). The default is \<HOME\>/epadd-appraisal, etc.

**epadd.mode=** Defines the default module upon opening ePADD. Value should be one of the exact strings, 'appraisal', 'processing', 'discovery', or 'delivery' (without quotes). Match is case insensitive. The default is appraisal.

3\) Save the file.

Depending upon your network permissions, you may be asked to allow ePADD access to your internet connection. ePADD requires an internet connection to download email from an email account using the IMAP protocol.

In Mac OS, the application icon **![Macintosh HD:private:var:folders:5m:rh\_kt9gx601gl7lw5qyk54gr0000gq:T:TemporaryItems:epadd\_logo.png][image2]** will appear in the OS Finder Toolbar. Right-click on this icon at any point to open an ePADD window or to quit ePADD.  
![E:\\Users\\jschne\\Dropbox\\ePADD\\Wordings and docs\\Screen Shot 2014-04-22 at 2.47.30 PM.png][image3]

**ePADD requires an internet connection to download email using the IMAP protocol**

**Note:** The Discovery Module is run through a separate distribution, available as a disk image ([*epadd-discovery-11.1.4-macos-arm64.dmg*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-macos-arm64.dmg) or [*epadd-discovery-11.1.4-macos-x86\_64.dmg*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-macos-x86_64.dmg)) or as [*epadd-discovery-11.1.4-standalone.jar*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-standalone.jar), which requires Java 11 or later. These files are accessible via [https\://github.com/ePADD/epadd/releases/](https://github.com/ePADD/epadd/releases/).  Be careful not to use the standard ePADD application in a discovery environment. The ePADD discovery application should be used in a public discovery environment because it is more secure than the standard ePADD application. For example, it does not allow any modifications to the archive.

ePADD saves all data in folders in the Macintosh HD/Users/\<username\>/ directory. We recommend as best practice making periodic back-ups of all ePADD related folders in this directory, which may include: epadd-settings, epadd-appraisal, epadd-processing, epadd-discovery, and epadd-delivery.

**Note:** For information about ePADD's memory requirements and how to adjust the maximum memory available to the application, see Section 2.1: System Requirements.

**Note:** The user can optionally create and modify a named entity kill list to suppress text strings that ePADD has identified as entities from appearing as such within the software. To do so, create a text file titled 'kill.txt' in the ePADD-settings folder. Each line of the document should contain a single named entity. Save the file and restart ePADD. The entities you listed should be suppressed.

### 2.2.3 Installing ePADD on Linux

For Ubuntu 24.04 or later, please download the Debian package [*epadd-11.1.4-linux-x86\_64.deb*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-linux-x86_64.deb) from [https\://github.com/ePADD/epadd/releases/](https://github.com/ePADD/epadd/releases/). The package includes Java, so no separate Java installation is needed. It is available for 64-bit x86 computers only.

**Installing with the .deb package**

1) Open a terminal in the directory containing the downloaded file.

2) Install the package (administrator rights are required):

*sudo apt install ./epadd-11.1.4-linux-x86\_64.deb*

3) Start ePADD from the application menu (it is listed under *Office*) or by running */opt/epadd/bin/epadd* in a terminal.

To upgrade, install the package of the newer version in the same way. To uninstall ePADD, run *sudo apt remove epadd*. Your data and settings folders are not affected by either.

The package was built on Ubuntu 24.04 and its dependencies are resolved against that release. On older Ubuntu versions and on other Linux distributions, use [*epadd-11.1.4-standalone.jar*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-11.1.4-standalone.jar) instead, which requires a 64-bit Java Runtime Environment version 11 or later:

*java \-jar epadd-11.1.4-standalone.jar*

ePADD saves all data in folders in the user's home directory (/home/\<username\>/), unless you have defined a different location using the epadd.properties file. The epadd.properties file and its settings work as described for macOS above. We recommend as best practice making periodic backups of all ePADD related folders in this directory, which may include: epadd-settings, epadd-appraisal, epadd-processing, epadd-discovery, and epadd-delivery.

**Note:** The Discovery Module is available as the Debian package [*epadd-discovery-11.1.4-linux-x86\_64.deb*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-linux-x86_64.deb) (installed with *sudo apt install* as above and uninstalled with *sudo apt remove epadd-discovery*) or as [*epadd-discovery-11.1.4-standalone.jar*](https://github.com/ePADD/epadd/releases/download/v11.1.4/epadd-discovery-11.1.4-standalone.jar).

### 2.2.4 Compatibility Note

Important: Releases from 6.0 onward package all email archive files using the BagIt specification, and are not backwards compatible with previous releases earlier to v. 5\. Users should migrate v. 5 ePADD folders using the script available via [https\://github.com/ePADD/epadd/releases](https://github.com/ePADD/epadd/releases).

ePADD 5.0 is not compatible with previous versions.

## 2.3 Running ePADD

If you installed ePADD with an installer, start it from the Start menu or desktop shortcut (Windows), the Applications folder (macOS) or the application menu (Linux). If you use epadd.exe, simply double-click the executable file. The standalone .jar can be started with *java \-jar epadd-11.1.4-standalone.jar*.

## 2.4 Selecting a Module

The first time you run ePADD, it will default to the Appraisal Module. For testing, this may be changed by manually pointing the browser to localhost:9099/epadd/switch-module. However, for production, it is best practice to first close ePADD, and change the default module using the epadd.properties file (see section [2.2: Installing ePADD](#22-installing-epadd) in this guide for more details on configuring ePADD).

Note that an email archive must first be ingested into ePADD and exported through the Appraisal Module before it can be accessed in the Processing Module, and exported once again to the Discovery and Delivery Modules before it can be accessed in those modules.

**![][image4]**

**Selecting a module during testing by navigating to localhost:9099/epadd/switch-module**

The four modules are:

**Appraisal Module** – Provides creators, curators, and archivists with a toolset to load, review, and manage an email archive on a local server prior to accessioning that archive to a repository. The module includes various methods to help a user determine the relevance and importance of email messages, to identify and flag sensitive messages, and to impose restrictions on access. Email can be exported from this module to the Processing Module.

**Processing** **Module** – Enables an archivist to organize and edit an email archive on a local server following the initial appraisal of materials. Includes the ability to annotate messages, to identify and flag sensitive information, to further refine entity extraction, and to approve/assign authority headings. Email processed through this module can be exported to the Discovery and Delivery Modules. (Note that the Discovery Module is downloaded and run separately; see sections [3.6: Exporting Email to the Processing Module](#36-exporting-email-to-the-processing-module) and [5.2: Mounting the Discovery Module Under a Web Server](#52-mounting-the-discovery-module-under-a-web-server) for further information).

**Discovery Module** \- A separate, public web-based discovery environment designed to allow remote users to search email correspondents, headers, and extracted entities, while limiting full-text access to the materials.

**Delivery** **Module** – Provides end users with a suite of sophisticated search functionalities to query and review the full content of unrestricted email messages in a collection. This is a client-facing module that is designed to provide mediated access to the processed email archive on a local server running on a managed workstation.

**Note:** While users can access a Discovery Module from the settings menu, implementing the module from a web server to provide public access requires additional work. Additional information about the Discovery Module can be found in *Section 5*.

**Note:** We have designed ePADD with the expectation that a given workstation will typically run one specific module at a time, though we recognize this will not always be the case. If you are exploring ePADD for development purposes, and planning to run multiple modules on a single workstation, you may navigate between modules by manually navigating to localhost:9099/epadd/switch-module using the web browser address bar. Please note that it is always best practice to close the archive using the ‘Close’ header menu option, and to fully quit ePADD, before beginning work in another module.

Please note that it is always best practice to fully quit ePADD and restart ePADD before beginning work in another module.  You *must* fully quit and restart ePADD in order to switch to another module from either the Discovery or Delivery module.

## 2.5 Navigating ePADD

The navigation bar at the top of the screen will present you with primary menu options aligning with the functionality of a particular module. Selecting one of these primary menu options will bring you to a sub-menu offering the potential to carry out related tasks. ePADD is flexible and does not require that the user navigate through a given module in any particular order. Links to documentation, user forums, and other helpful information can be found via the **Help** dropdown located in the far right of the navigation bar.

**![][image5]**

**Documentation and other information can be accessed from the Help dropdown**

Collections in Processing, Discovery, and Delivery can be closed, and the users return to the collection browse menu, using the ‘Close’ header menu option, providing additional clarity when working with multiple collections. Note: As always, the user can only work with one collection at a given time.

Additionally, users must manually save their work in Appraisal, Processing, and Delivery, using the 'Save' header menu option.

The user can generate a link to a specific message within a given module by selecting the 'Link' option, next to the unique message ID. The generated link appears in the address bar. After loading a collection, pasting the link will take you immediately to that message.

More settings may be available under the **More** option on the Browse screen, including verifying the bag checksum. This function will verify that the checksum of the bag is consistent with ePADD’s expectations. This may be especially helpful to ensure that an email archive transferred from a donor has not been corrupted or otherwise tampered with.

ePADD uses SHA256\_fixity checksums and MD5 checksums. Both are present in the ePADD Appraisal directory:

![][image6]

The use of the SHA256\_fixity checksums is new to ePADD v.10. If an email archive was imported using an earlier version of ePADD, it will not have been subject to the SHA256\_fixity checksum. To apply a SHA256 checksum, click on the Information button next to the email archive title at the head of the ePADD dashboard:

![][image7]

Then, click ‘UPDATE CHECKSUM’:

![][image8]

**We recommend keeping regular backups of your epadd-appraisal, epadd-processing, and/or epadd-delivery folders in case of file corruption.**

You can view more information about the archive by clicking on the information button  adjacent to the email archive title at the top of most pages:

![][image9]

To view additional statistics for the email archive, select the **Reports** option from the Browse menu.

Tips and other helpful information can be accessed on many screens by  
selecting the help drawer button located in the top right corner of a screen:

![][image10]

**An example of a screen which provides helpful information accessed via the help drawer.** ![][image11]

# 3 Appraisal Module

![][image12]

**Appraisal Navigation Bar**

The **Appraisal Module** provides creators, curators and archivists with a robust toolset to load, review and manage the email of an individual (or optionally, a group of individuals) prior to accessioning that mail to a repository. The module includes various tools to help a user determine the relevance and importance of email messages, to identify and flag sensitive messages, and to impose restrictions on access.

## 3.1 Importing email

The Appraisal module is the default module (although this can be adjusted using the epadd.properties file).  When first launched, you will find yourself on the **Import** screen. This interface is where you enter information (name and an associated email address) about the owner of the email account (optional), as well as specify the location and account info, if applicable, of the MBOX files or IMAP email accounts that ePADD will be ingesting for review and potential transfer to an archival repository. You can select multiple accounts, as well as multiple MBOX files, by selecting the *Add another account* or the *Add* anothe*r folder* buttons. Note that many email clients do not allow incoming IMAP connections due to their security settings, before attempting an import using IMAP, verify that your account will allow it.

![][image13]

**ePADD works with MBOX files as well as IMAP email accounts**

Completing the *Name of email source* enables users to explore email by source after import, either by using the new *Folder View*, or by specifying the *Email Source* in Advanced Search. For ingest of email through IMAP, the email account is used as the source name. Please ensure all relevant information has been filled out before pressing the *Continue* button at the bottom of the screen.

**![][image14]**

**ePADD will estimate how long it will take to load the selected email files**

Once one or more email sources have been loaded, ePADD will give you the opportunity to select or deselect individual email folders associated with accounts, as well as specify a date range to review.

![][image15]

**Confirming Selection of Email Sources**

Once you have confirmed the mailboxes you would like to review for transfer, click the *Next* button. Depending on the size of the accounts/folders selected, this next step may take some time as ePADD imports and deduplicates messages and performs a variety of additional steps intended to allow the user rich access to the email archive.

If you receive an error during the import process and need to start again, you can delete a collection from within the Appraisal module. Click on the information button \-      \- which is located to the right of the email archive’s title on the ePADD dashboard:

![][image16]

And then click ‘Delete’, which is located in the bottom left of the ‘Information about this Archive’ dialog box:

![][image17]  
![][image18]

Alternatively, users can also delete the relevant directory manually by removing the epadd-appraisal folder from the user directory. This folder can be found:

On Windows:  C:\\Users\\\<username\>\\epadd-\<module\>\\  
On Mac: /Users/\<username\>/epadd-\<module\>\\

If you delete the directory manually (not from within ePADD as described below), then make sure ePADD is not running; otherwise, you get a *folder in use* error. You can stop ePADD by going in your browser to localhost:9100.

This method \- deleting the directory manually \- can be used at any point if you need or want to start again.

### 3.1.1 Emailchemy Integration

As an optional add-on, ePADD offers users the ability to import various email file formats using Emailchemy, an email conversion tool (this add-on is unavailable in earlier versions of ePADD). Emailchemy also allows users to export emails, through ePADD’s Appraisal and/or Processing modules, in formats other than MBOX, which is ePADD’s standard format. For example, users can export to EML, if that is a preferred preservation format. Click on the following link for a comprehensive list of file formats that Emailchemy reads and writes: [https\://weirdkid.com/emailchemy/](https://weirdkid.com/emailchemy/).[^1]

To use the integrated version of Emailchemy, users must purchase a license from the developers, Weird Kid Software, and create a license dongle, which will enable those features in ePADD. Further information on how to obtain a license is available via the Weird Kid Software website: [https\://weirdkid.com/emailchemy-for-epadd/](https://weirdkid.com/emailchemy-for-epadd/).

The status of the Emailchemy license – whether it has been enabled or not – is noted on the Import screen:

![][image19]

![][image20]

When the Emailchemy license is active, users can point either to a specific non-MBOX file or to a folder containing one or more files:

![][image21]

In the example below, two non-MBOX files (PST files) are converted. The term ‘folder,’ in this context, refers to a non-MBOX file, not the number of folders in that particular file.

![][image22]

ePADD produces a list of folders contained within those non-MBOX files, as in the example below. To see the full file path of each folder, hover the cursor over the folder name. This screen also records the import of any MBOX files.

![][image23]

Following the import, select ‘Folders’ on the Appraisal module dashboard for a list of folders imported from the PST files:

![][image24]

![][image25]

### 3.1.2 Sidecar Files

A sidecar file is a file that is not necessarily part of an email archive, but which contains information that is relevant to it, such as a donor agreement, finding aid or metadata file. Sidecar files are optional and can include any type of documentation considered relevant by the user. ePADD allows users to upload, export, list and delete sidecar files. To upload a sidecar file, go to the Import screen and click on the upload icon:

![][image26]

Sidecar files are stored in:

On Windows: C:\\Users\\\<username\>\\epadd-appraisal\\user\\data\\blobs\\sidecarfiles

On Mac:  User/\<username\>/epadd-appraisal/user/data/blobs/sidecarfiles

To view a list of uploaded sidecar files and, if necessary, delete any of them, click ‘Sidecar File Listing’ on the **Import** screen:

![][image27]

## 3.2 Reviewing Email

The Appraisal Module provides the user with a diverse set of tools for reviewing and assessing email prior to transfer to an archival repository.

As a first step, the user should review the **Reports** option from the Browse menu.

This screen will present information about the email, including duplicate messages deduplicated by ePADD, and any potential errors identified by ePADD, such as missing headers and dates. Messages containing potential errors are listed on this screen by error type.  
![][image28]

![][image29]

The error itself is assigned to the message as a label. To view the set of messages with a particular label, select the Labels option from the Browse screen, then select the label with the associated error type.

![][image30]

Errors may have been generated during creation (in case of older email), transfer, or migration. Ascertaining where the error occurred may require technical expertise or assistance.

With the exception of reviewing the report and potentially investigating possible errors, there is no set order to perform the following steps. In fact, different email archives will benefit from different approaches.

The various tools ePADD provides to assist in email review are described below.

### 3.2.1 Browsing Email Messages

After loading email into ePADD, the program opens the **Browse** screen as the default view. The user can also navigate to the **Browse** screen from the Appraisal Module Menu. From this screen, the user can browse All Messages (which allows users to browse all messages in the collection, one-by-one);by Correspondent; by Person or Other entities; by account folder structure; by Lexicon categories; or by whether messages include Image Attachments, Other Attachments; or contain sensitive information (identified through regular expressions).

### 3.2.2 Browsing All Messages

Users can browse all messages in a collection by clicking on ‘All Messages’, which is located in the banner at the top of the ePADD dashboard:

![][image16]

### 3.2.3 Editing Correspondents

![][image31]

ePADD automatically merges identities for a single correspondent by intelligently analyzing headers. This step helps clarify relationships between email correspondents to allow for higher-level analysis of the email archives.

Users can graph, edit, and export, and re-import the address book using the buttons available from the **Browse Correspondents** screen.

Navigate to the **Edit Correspondents** screen by selecting the Edit Correspondent button  This screen allows the user to view and edit the email address(es) and name(s) associated with each correspondent.  The default view of this screen lists all correspondents by volume of messages exchanged, but the correspondents can also be listed alphabetically using the dropdown selection at the top of the screen. If there is a single archive owner, they should always be the first individual listed.

**Note: As a first step, we recommend downloading the correspondent file before making any edits, as some changes are irreversible.**

![][image32]

**ePADD allows the user to confirm or correct the identities of correspondents that ePADD has resolved using the Edit Correspondents screen**

If names or addresses are incorrectly associated with the wrong individual, you can manually cut and paste them underneath a different entry.

ePADD recognizes header information identifying messages as being sent from a mailing list where that information is available. These messages are differentiated from other correspondents in the Edit Correspondents screen under the heading '-- \[ML\]' Additional mailing list addresses can be manually identified and added under this heading using the Edit Correspondents interface. Users can also define the default display name for a correspondent when editing correspondents. ePADD interprets the first name listed as the default display name. If you would prefer a different name as the default display, simply add it to the top of the list. **Note:  Re-ingesting email will overwrite any user changes. Please be sure to keep track of which addresses have been marked as mailing lists if planning to reingest email.**

When you have completed your work editing correspondents, press *SAVE*.

You can also edit the address book offline, using the export/import functionality available within ePADD.

Select the graph button![][image33]to view a graph of top correspondents. Drag and drop a bounding box over the X axis to zoom in on a time period.

Two or more correspondents can now be selected within the correspondents list in ePADD, and the entries can be combined using a new icon on the correspondents list page.

The entry will now contain all of the email addresses, names and titles contained within the items which were combined.

### Instructions:

In the correspondents tab, search for the entries you wish to merge and select them by clicking on the entry anywhere other than the name/message figure totals

![][image34]  
Select the merge correspondents icon: ![][image35]

![][image36]

Select ‘OK’ on the following warning to confirm the merge:

![][image37]  
Merging correspondents can sometimes take some time, as it combines multiple entries in the address book but also re-compute contact-related ePADD metadata. A progress bar to provide reassurance that the process is continuing has been added:

![][image38]

Once the merge is complete the following message is displayed:

![][image39]

**Now select the ‘save’ option from the top menu, or ePADD will not retain the changes.**

![][image40]

### 

### 3.2.4 Performing Entity Analysis

ePADD identifies, extracts, and manages entities using a custom natural language processing toolkit. The extracted entities include persons, fine-grained entities such as corporations and government agencies, as well as other entity types such as diseases, awards, and events.  
Extracted entities can be reviewed by navigating to **Entities**, accessible under the **Browse** menu option from the navigation bar.

**3.2.4.1 	Reviewing Fine-Grained Types**

To review fine-grained types such as universities and governmental agencies, or other entity types such as diseases and awards, select **Entities** from the Browse menu. You can optionally download a CSV containing all entities and associated metadata (\# of messages, score, date range, and entity type) from this screen.  
![][image41]

**Fine-grained entity types, accessible through the *Entities* option on the browse screen.**

Select an entity type to view all of the entities recognized as belonging to that type.

![][image42]

Select an entity to view the set of all messages including that entity. Users can also download all entities of a particular type and associated metadata (\# of messages, score, inclusive message dates) from this screen; or also choose to edit entities associated with a particular entity type.

**3.2.4.2 	Editing Entities**

Users can edit or merge entities in a given category from the entity browse screen, by selecting the 'Edit entities' option:

![][image43]

The user can merge entities by grouping them together using find, cut, and paste commands. Unmerge entities by separating them using find, cut, and paste commands. The first entity name listed within each group of entity names is the one ePADD will display in all search and browsing results and visualizations. Manually change this display name by moving a new name to the top of this list. Alternatively, you can supply a new entity display name to the top of the list. This supplied entity name does not need to appear in the email archive. The user can view the individual entities that have been merged with a given entity by hovering over that entity from the Entity Types screen.

The user can optionally create and modify a single entity kill list to suppress text strings that ePADD has identified as entities from appearing as such under any entity category. To do so, create a text file titled 'kill.txt' in the ePADD-settings folder. Each line of the document should contain a single named entity. Save the file and restart ePADD. The entities you listed should be suppressed.

### 3.2.5 Performing Lexicon Analysis

ePADD employs lexicon analysis to search email messages for terms associated with personal or restricted information, which might indicate the need for further review. ePADD ships with several default lexicons. The 'Sensitive' lexicon can be used to assist in the identification of email messages with the potential for confidential content.

The user can make persistent changes to any lexicons, or start afresh by building a new lexicon.

The **Lexicon Browse** screen is accessible under the Browse Menu screen from the Appraisal Module navigation bar.

![][image44]

Users can create a new lexicon, import a lexicon from outside ePADD, or view a current lexicon from the Lexicon Browse screen. Click on an existing lexicon to view the included categories of terms and number of hits for each category.  
![][image45]

From a particular lexicon page, you can choose to view a graph of the results, or edit the existing lexicon. Or, you can select a category to view all messages that contain a term included in that category.

#### ***3.2.5.1*** **Editing a Default Lexicon**

![][image46]

The edit lexicon screen allows users to add categories, delete categories, download the lexicon for offline editing or sharing, or delete the lexicon entirely.

Users can also test categories to view the number of hits for each term in the category. This can be helpful in discovering whether a particular term is too broad or narrow to be helpful.

**Please note:** New entries in a category should be separated with the \| (pipe) character. Multi-word phrases should be enclosed in double-quotes. Note that words and phrases with hyphens, such as father-in-law, should be placed in quotes.

When you have finished making changes, select the *Save* button.

Users can export lexicons using the Export Button, to install them for use with other collections or share them with another institution. In this way, these lexicons can be reused across collections and ePADD instances. Default lexicons may be updated in future releases.

The user **must** use the ePADD interface to add, remove, or update lexicons. This is because of how the bag checksum is calculated.

#### 3.2.5.2 Reviewing Lexicon Analysis Results

ePADD allows a user to test the results of any category  in order to ascertain whether a particular term is too general or too specific to be helpful.  
![][image47]  
**Test terms contained in a lexicon category by selecting the test function ![][image48]from the Edit Lexicon screen**

Lexicon analysis results for a whole lexicon may be reviewed via Advanced Search. A user can also view lexicon analysis results directly by clicking category results from the Lexicon Table screen.

### 3.2.6 Reviewing Regular Expressions **![][image49]**

To further aid in identifying potentially confidential materials, ePADD searches the archive for sensitive messages by searching for regular expressions such as social security numbers, and credit card numbers. Regular expressions are a customizable lexicon under the Lexicon Search browse category, selected from the lexicon dropdown menu. Note that since each lexicon category contains just one regular expression, results for each category display under Lexicon Search results. The user can find the form of many common regular expressions online; [https\://regexlib.com/](https://regexlib.com/) may be a helpful resource in this regard.

## 3.2.7 Reviewing Attachments

```
ePADD allows the user to browse and review email attachments. ePADD uses the Apache Tika toolkit to recognize a wide range of document formats. A complete list of formats recognized by Apache Tika can be found [here](http://tika.apache.org/1.4/formats.html).   
```

&nbsp;

#### 3.2.7.1 Reviewing Image Attachments

Select **Image Attachments** from the Appraisal Module menu to navigate to the Image Attachments screen. Image attachments are displayed chronologically by year and are de-duplicated. The user can navigate between years using the left and right arrows at the top right of the screen. The user can switch between a grid and list view using the icons in the top right corner. Selecting the Message View button will open the set of all messages associated with the currently displayed set of attachments. Selecting an image attachment will give the user the option to directly access the related message. The user can also export the current set of attachments as a .zip file by selecting the download icon.

Facets on the left can be used to further refine the set of images displayed by certain criteria including attachment type, attachment size, correspondent, and folder. Note that facets are tied to the messages associated with the currently displayed attachments. If the user selects '.pdf' from the attachment type facet, the resulting set of image attachments will include all attachments associated with messages that contain a .pdf attachment. This distinction means that the user will see attachments with other file types.

**Pro-tip: You might use the file size and file type facets to exclude common non-controversial logos etc.**

![][image50]

#### 3.2.7.2 Reviewing All Attachments

![][image51]

Select **All Attachments** from the Appraisal Module menu at the top of the screen to navigate to the All Attachments screen. This screen will present all attachments associated with the email archive, which can be viewed in a grid or list view, or downloaded and viewed outside of the ePADD program.

As with the Image Attachments view, attachments are displayed chronologically by year and are de-duplicated. The user can refine the set of attachments using the facets. The user can navigate between years using the left and right arrows at the top right of the screen. The user can switch between a grid and list view using the icons in the top right corner. Selecting the Message View button will open the set of all messages associated with the currently displayed set of attachments. Selecting an attachment will give the user the option to directly access the related message. Note that ePADD does not provide native support for viewing non-image attachments.  
![][image52]

![][image53]

### 3.2.8 Browsing Email Folders

### ![][image54]

The user can navigate email messages by folder structure using the Browse Folders interface, accessible from the Browse screen.

![][image55]  
**Browse Folders interface**

If using Gmail, to ensure messages appear in the appropriate folder, please be sure to deselect the non-unique 'All Mail' folder during ingest for email ingested via IMAP.

## 3.3 Appraising Email

Once you have confirmed the identity of correspondents, edited the lexicon to your satisfaction, and reviewed entities and attachments, the next step is using the information you have learned to appraise individual email messages. ePADD provides the user with a faceted search interface from which to annotate individual email messages and make appraisal decisions.

### 3.3.1 Reviewing and Creating Collection Metadata

ePADD allows users to add manually an extensive range of metadata associated with a particular email archive. First, click on the information button \-  \- which is located to the right of the email archive’s title on the ePADD dashboard:

![][image16]

This will open a dialog box entitled ‘Information about this Archive’. Here, click on the edit button \-  \- found next to the heading ‘Metadata’:

![][image56]

This presents you with a variety of metadata options:

![][image57]

On the tabs you can switch between Collection, EAD (Encoded Archival Description) and PREMIS. In the Collection metadata you can add some basic information about the collection, such as Collection Title, short description, and scope and content, as well provide links to online catalog descriptions.

You can do the same with metadata elements associated with EAD by selecting the EAD tab:

![][image58]

And with PREMIS:

![][image59]

You can add or edit PREMIS metadata for an individual file, not just the whole collection, by selecting one of the files listed to the left of the metadata fields. This will present you with two tabs, ‘File,’ where you can add notes about a particular file, and ‘PREMIS,’ where you can add metadata relevant to that particular file:

![][image60]

PREMIS also has an XML schema that has been developed for preservation metadata. The xml file generated by ePADD is found in the folder ‘user’ in the directory:

On Windows:  C:\\Users\\\<username\>\\epadd-\<module\>\\  
On Mac:  /Users/\<username\>/epadd-\<module\>/

The file epaddPremis.xml is located in In the folder ‘data’:

![https\://lh3.googleusercontent.com/vKCVbpm5gEmKb71AVhnUzN2uHS5UENYFShGcS1t5NwBFXpT5wTGlSsLKDfPZJhV-XN4-NGS8z0a6ssf6GfwTe4kQNyONpLuv4xUE5ZU50l19naMWc5pWCa6j2CYNEIxy3WJiwpJB4w4b85Ig5WEhKZplCzhOjw90iKEG0Z0Sc04M9odACS1xcPir][image61]

Here you can see an example of the xml file, which contains the manually added PREMIS metadata as well as some metadata automatically added by ePADD, such as date and time of ingest, number of attachments in the collection:

![https\://lh5.googleusercontent.com/fGZMh-LM-WR-SlEUZ6xK0N5jgK6LCdC3iaAFJs6puSVNmvLaKvYJ1DpQhtW6vNckuWICarXS2pjoJnsC-hHZDj2tqkmCysqT\_eqBUAQYNBGgeVai9uUNGsSf6ZxvirExVzO7nAiD9CZrGQSKr-wtKbVWvqlDO3vr01lkmXJD\_6gPUiTJTXxexN1C][image62]

As well XML, PREMIS metadata is also available in JSON format.

![][image63]

To add a PREMIS event manually, select ‘More’ on the ePADD dashboard:

![][image64]

Here, users can select a PREMIS event type, such as ‘Virus check,’ from the dropdown list, add further information to the free text ‘Details’ field, and enter the date/time of the event.

![][image65]

As well XML, PREMIS metadata is also available in JSON format.

![][image63]

### 3.3.2 Searching Email Messages

![][image66]![][image67]

To search the email archive, navigate to the Search screen from the Appraisal Module Menu.

```
Besides lexicons, accessible via the Browse page, ePADD supports five types of search: simple search, advanced search, multi-entity search, multi-term search, and correspondent list search. 
```

&nbsp;

To use the simple search feature, simply type in your query into the simple search box available on the initial search screen. This will search across message headers (correspondents and subjects) and message text, as well as the text of all document attachments.

Once you have entered your term(s) and made your selection, click the *Search* button. The search string will appear highlighted on the message browse screen.

Advanced Search enables the user to generate complex searches of email text, correspondents, attachments, and any actions previously taken on the messages, as well as a variety of other parameters.

![][image68]

![][image69]![][image70]![][image71]

Search Tips can be accessed by selecting the help button ![][image72] in the top right corner of the *Advanced Search* screen.

ePADD also allows a user to search the email archive for entities contained in a given block of provided text, using *Multi-entity Search*. Any matching terms will be highlighted on the results page, and hyperlinked to corresponding messages.

Likewise, ePADD enables a user to search an email archive for multiple terms using *Multi-term Search*. Type or paste one term per line. Any matching terms will be highlighted on the results page, and hyperlinked to corresponding messages.

Finally, ePADD enables a user to search for a list of email addresses using Correspondent List Search. Type or paste one term per line to search for and apply labels to a set of email addresses.

#### 3.3.2.1 Navigating Search Results

Upon submitting a search query, ePADD opens a new browser window displaying the message browse screen.

![][image73]

The message browse screen displays the full text of email messages as well as a faceted browser which allows the user to further limit the search results. The total number of messages resulting from your search appears above the email message display. The current message appears as a fraction of that display. For instance, if you are appraising message 1 out of 400 results, the display would read 1/400. The forward and back arrows to the left and right of that number allow you to navigate between search results.

Search results can be limited by correspondent, email direction (incoming or outgoing), lexicon category (if relevant), attachment type (if relevant), annotations or the originating mail folder. To limit the results by a particular facet, simply click the term from the left-hand panel.

All messages have been assigned a unique ID, accessible from this page. Select the ID button to copy the ID to your clipboard:

![][image74]

If you ever wish to return to a particular message, you can search for a message ID (or semicolon delimited set of message IDs) using the Advanced Search interface in the Miscellaneous search category.

**![][image75]**

### 3.3.3 Annotating Email Messages

ePADD allows the user to annotate email messages from the message browse screen. These annotations are persistent across ePADD sessions, and can be used to provide supplemental information to assist the archivist in processing the collection, including elaborating on access restrictions or to provide additional information for the benefit of researchers.

To annotate an email message, click the annotation button in the bar above the message headers: . Users can select to apply the annotation to a single message or all messages currently being reviewed. Likewise the user can either append the annotation to current annotations, or overwrite all existing annotations with the new annotation. **Warning: Overwriting annotations cannot be undone\!**  
![][image76]

Once you have added an annotation, a new facet, Annotations, will appear in results on the message browse screen. Additionally, when you navigate to that message or those messages in the future, the annotation will be visible.

![][image77]

### 3.3.4 Labeling Email Messages **![][image78]**

ePADD allows the user to label email messages that should not be exported for transfer to an archival repository, as well as messages that should be transferred with restrictions.

To label a message, navigate to the message or set of messages, then select a label or set of labels from the Labels dropdown.

There are three default labels within ePADD: Do Not Transfer is a default, not editable restriction label that indicates the message should not be transferred to the next module(s).  
Cleared for Release is a default, not editable restriction label that indicates the message that was previously labeled with at least one restriction is now cleared for release and should be  transferred to the next module(s).

Reviewed is a default, editable label that indicates the message being browsed has been reviewed, and can be used to help track one’s progress in reviewing the email archive.

![][image79]

After you have assigned labels, they appear in the message browse screen.

![][image80]

#### 3.3.4.1 Assigning bulk labels

To apply a label to more than one message, you can select the label management button above the message pane:

![][image81]

![][image82]

You can also assign default labels for all messages by using the 'Set labels for all messages' function.

![][image83]

#### 3.3.4.2 Creating a new label

![][image84]

**![][image85]**

**Users can create new labels, import labels (overwriting all current label assignments)  or export all labels via the Labels interface.**

Below are several screenshots illustrating how to create a new restriction label.

![][image86]

![][image87]

Messages that are restricted until a particular date, or restricted for a given time period from the date of message creation, are considered machine-actionable, as ePADD is able to recognize when the restriction period has expired via an advanced search function.  
![][image88]

Messages marked with restriction labels will not automatically export to Discovery and Delivery. In order to export these messages, the user needs to manually assign them the label 'Cleared for Release.' Note that ePADD does not delete any messages that have restriction labels applied to them, they are simply not exported to subsequent modules.

Because ePADD enables users to assign multiple restriction labels to a given message or set of messages, you may find it helpful to review the set of messages that contains more than one restriction. You can perform this search query under Advanced Search.

![][image89]

**Viewing all messages with more than one restriction via advanced search.**

**Warning:** Since importing labels overwrites all current label assignments, this task should only be undertaken prior to accessioning / processing a collection.

#### 3.3.4.3 Assigning bulk labels by correspondent

ePADD also gives the user the ability to assign labels to all messages from a list of correspondents, using the *Correspondents List Search* option.

### 3.3.5 Assigning Images to the Collection

ePADD allows the user to assign up to three images to any given collection to improve the end-user experience: a profile image in Appraisal, and profile, landing page, and banner images in Processing.

Images are set in the location they appear. You can set the profile image from the browse page using the edit option within the greyed out profile image.

![][image90]![][image91]

Within Processing, you can add the landing image from the Collection Browse page, and the banner image from the Collection Details page, using a similar interface.

## 3.4 Exporting Email for Preservation

ePADD can export email messages, including their headers and attachments, in MBOX or EML format for long-term preservation or transfer to a digital preservation repository. Both formats are suitable for preserving email. This functionality is available in both the Appraisal and Processing modules.

To create a preservation package, select **Export** from the ePADD dashboard:

**![][image92]**

In ‘Export to Preservation’, select either ‘Export appraised emails in MBOX format’ or ‘Export appraised emails in EML format’ and click ‘Export’:

![][image93]

EML export uses Emailchemy. Without an active Emailchemy licence, the export runs in demo mode, which modifies the subject line of each exported email. All other email content remains unchanged. An active Emailchemy licence is required to export EML files without this modification.

Clicking ‘Export’ creates a folder containing the exported files. The folder name depends on the module:

* **Appraisal Module**: AppraisalNormalizedAppraised  
* **Processing Module:** ProcessingNormalizedProcessed

The folder is located within the *user/data/exportableAssets* directory of the corresponding module.

**Windows:**

C:\\Users\\\<username\>\\epadd-\<module\>\\user\\data\\exportableAssets\\

**macOS:**

/Users/\<username\>/epadd-\<module\>/user/data/exportableAssets/

Replace \<module\> with appraisal or processing, as appropriate.

&nbsp;

When files are imported into the Appraisal Module, ePADD creates the following directories:

* **AppraisalCanonicalAcquisitioned** – contains the original imported files, including files before conversion by Emailchemy and unmodified MBOX files.&nbsp;  
* **AppraisalNormalizedAcquisitioned** – contains files before any appraisal or processing either unmodified imported MBOX files or converted to MBOX by Emailchemy. ‘Normalized’ describes the file format used by ePADD as a common format. The ePADD ‘Normalized’ format is MBOX.&nbsp;

![][image94]

When exporting in EML format, the exported files are stored in the exported eml subdirectory of AppraisalNormalizedAppraised.

On Windows: C:\\Users\\\<username\>\\epadd-appraisal\\user\\data\\exportableAssets\\AppraisalNormalizedAppraised\\exported eml

On Mac: /Users/\<username\>/epadd-appraisal/user/data/exportableAssets/AppraisalNormalizedAppraised/exported eml

Here is an example of the folder structure within the EML folder:

![][image95]

**Downloading email messages**

Email messages can also be downloaded directly from ePADD's Export page in MBOX or EML format.

Users can choose from the following options:

* **All Messages** – Downloads all messages.  
* **Non-Restricted Messages** – Downloads messages without restriction labels.  
* **Restricted Messages** – Downloads messages with restriction labels.

![][image96]

For more information about restriction labels, see section **3.3.4 Labeling Email Messages**.

When downloading in EML format, the messages are provided as individual EML files within a ZIP archive. The Emailchemy licence requirements described above also apply to EML downloads:

![][image97]

![][image98]

When downloading in MBOX format, ePADD produces a single MBOX file containing the selected messages.

## 3.5 Exporting Message Attachments

Select Export from the Appraisal Module menu at the top of the screen to navigate to the Export screen.

![][image99]

![][image100]

You can choose to export attachments according to various criteria, including particular formats. The user can also indicate whether they would like to export only those attachments which have not been recognized by Apache Tika (and are therefore not indexed with ePADD), for further review. Note that messages are only flagged in this way if they have also been appraised or processed by ePADD.

## 3.6 Exporting Email to the Processing Module

Once you have reviewed the email archive, the final step is to export the appraised email archive (along with whatever supplemental information or annotations you have added) so it can be transferred to an archival repository, and/or processed in another ePADD module.

Select Export from the Appraisal Module menu at the top of the screen to navigate to the Export Screen.

![][image99]  
![][image101]

To export the archive to the Processing Module, first select Browse under *Export to Next ePADD Module.*

**![][image102]**  
**Selecting a folder location to export the appraised email archive**

Select the location for ePADD to export the email archive. Please ensure you have enough disk space to save the complete email archive export or the export will fail.

This step may take some time as ePADD assembles the exported email archive and accompanying documentation.

![][image103]

![][image104]

**An exported email archive**

**Note to Donor**: Once you have confirmed with the archival repository that the transfer is successful, you may wish to delete the ePADD working directories from your workstation. You can do so manually by deleting the following folders under your user directory: 'epadd-appraisal' and 'epadd-settings.'

During the ePADD installation process, a file named *epadd.properties* is created. The file is located in the user’s home directory, e.g.

On Windows: c:\\users\\\<username\>\\  
On Mac: /Users/\<username\>/

By default, this file contains the following text:

epadd.mode=appraisal

**![][image105]**

‘appraisal’ indicates which module or mode ePADD is in; if, for example, you were to restart ePADD it will open in the Appraisal module. To run a different ePADD module, replace ‘appraisal’ with ‘processing’, ‘discovery’ or ‘delivery’, as in these examples:

- epadd.mode=processing  
- epadd.mode=discovery  
- epadd.mode=delivery

**![][image106]**

If it is already running, you must restart ePADD. If no module is specified in the properties files, ePADD will start in its default mode, which is the Appraisal module.

Note that when changing the mode to ‘discovery’, you also need to start ePADD using the file  
epadd-discovery-standalone.jar as described elsewhere in this guide (please see section [5.2 Mounting the Discovery Module under a Web Server](#52-mounting-the-discovery-module-under-a-web-server)). Although ePADD will start in discovery mode using epadd.exe or epadd-standalone.jar, it will not completely work as intended.

# 4 Processing Module

![][image107]

Email archives must initially be ingested to ePADD from the Appraisal module. To work with a collection within the Processing module, it is best practice to first close ePADD, and change the default module using the epadd.properties file (see section [2.2: Installing ePADD](#22-installing-epadd) in this guide for more details on configuring ePADD). After confirming successful import of files into Processing, you can optionally delete files in Appraisal by deleting the 'epadd-appraisal' folder in your user directory.

If you are planning to run multiple modules on a single workstation, you may also navigate between modules by manually navigating to localhost:9099/epadd/switch-module using the web browser address bar. Again, please note that it is always best practice to close the archive using the 'Close' header menu option, and to fully quit ePADD, before beginning work in another module.

**Please note that if you wish to export an unprocessed MBOX file for preservation, this should be your first step upon starting up the Processing Module. Select Export from the navigation bar, then use the Export messages in MBOX format option.**

## 4.1 Importing an Email Archive from the Appraisal Module into the Processing Module and Assigning Accession Metadata

Please note that it is always best practice to fully quit ePADD before beginning work in another module. To work with a collection within the Processing module, it is recommended to first close ePADD, and change the default module using the epadd.properties file (see section [2.2: Installing ePADD](#22-installing-epadd) of this guide for more details on configuring ePADD).

ePADD uses the concept of accessions and collections. All email exported from Appraisal is considered an accession. To create a new collection within ePADD Processing, select 'Add' from the navigation bar, then use the 'Browse' button to navigate to the folder exported from the Appraisal Module. Assign accession metadata, then import the email archive identified by clicking the 'Import Accession' button.

Users can continue to review and create collection metadata in the Processing Module. Please refer to section 3.3.1 for more information on this process.  
![][image108]

Once you have added an accession to a new collection, you have the ability to add collection metadata.

Add an additional accession to an existing collection by selecting the Add Accession button,  from the Collection page, below Collection Details. You can also edit accession metadata at any time from the Collection page.

## 4.2 Assigning Collection Metadata

After the accession has been imported, you will be brought to the Browse screen. Close the archive using the close button in the navigation bar to return to the Collections page.

![][image109]

There you will find your  accession. Optionally, you can select the edit button to add a landing  page image. This image can also be set or changed later.

![][image110]

To edit the collection details, click on the image/title.

![][image111]

When we are done, this page will match what users will experience in the Discovery and Delivery modules.

Currently, it is lacking a banner image and any collection metadata\! Optionally, select the edit button to add a banner image. This image can also be set or changed later.

![][image112]

To add collection metadata, select the edit button next to the Summary![][image113].

![][image114]

![][image115]![][image116]

When you have finished adding information, click Save.

![][image117]

## 4.3 Appraisal Revisited

The **Processing Module** includes all of the review and appraisal functionality of the **Appraisal Module**, as well as additional functionality associated with archival processing. Please refer to **Section 3: Appraisal Module** for information on Reviewing Email (Browsing Email Messages; Editing Correspondents; Performing Entity Analysis, Performing Lexicon Analysis; and Reviewing Attachments) as well as Appraising Email (Searching Email Messages; Annotating Email Messages; and Restricting Email Messages).

There is no set order to perform the steps above. In fact, different email archives will benefit from different approaches. **However, we do recommend that you begin by verifying the bag checksum. This option can be accessed under the More from the Browse menu**.

The various unique functionalities provided by the Processing Module to assist with archival processing are described below.

## 4.4 Setting Default Labels

If you wish to reset any labels that were added in Appraisal, or set new default labels, navigate to Labels from the Browse screen and select Set labels for all messages.

![][image118]

The Processing Module contains an additional restriction label not present in the Appraisal Module \- 'Transfer to Delivery Only.' When a processed email collection is exported from the Processing Module to the Discovery and Delivery Modules, messages with this label applied will only export to the Delivery Module and not to the Discovery Module.

When processing a large email collection, it is not always possible to carry out appraisal on an entire collection, and a sub-set or smaller collection identified within the collection may be selected as an interim measure. A sub-set of appraised records rather than the collection in its entirety can be transferred to the Delivery module using the ‘permission’ label (We introduced a new type of label: the ‘permission’ label. You have to create a custom label of type ‘permission’ for example ‘Cleared for Transfer to Delivery module’).

## 4.5 Performing Authority Control

ePADD allows the user to reconcile correspondents and named entities extracted by ePADD with authorized headings (including OCLC FAST headings).

Within the Processing Module, select **Authorities** from the navigation bar to begin this process.

### 4.5.1 Reconciling Correspondents with Authorized Headings

```
For more information about how ePADD merges correspondents, as well as how to manually override merged correspondents, please see section [3.2.3: Editing Correspondents](#323-editing-correspondents) for further information.

The order of correspondents on the Authorities screen can be relisted alphabetically by person, or by volume of messages exchanged.  You can also select how many correspondent entries to view per page using the option box in the top left corner, as well as search for a particular correspondent using the search box at the top of the page.
```

&nbsp;

![][image119]

ePADD searches OCLC FAST for LC Subject Headings, as well as DBPedia, VIAF, and LCNAF. If an image is associated with the related DBPedia record, it can be displayed by hovering over the related DBPedia link.

```
If you are unsatisfied with ePADD’s results, you can add an authorized heading ID or local ID using the **\+** button.
```

&nbsp;

![][image120]

Any actions taken in the Authorities interface are autosaved. Authorized correspondents can be exported as a .CSV file by selecting **Export** from the navigation bar, then using the *Export Correspondents* option. See section [4.11: Exporting Correspondents](#411-exporting-correspondents) for more information.

## 4.6 Exporting Email for Preservation

The Processing Module supports the same preservation export and email download functionality as the Appraisal Module.

ePADD contains the following directories within the Processing Module's user/data/exportableAssets directory :

* **AppraisalCanonicalAcquisitioned** – contains the original imported files, including files before conversion by Emailchemy and unmodified MBOX files.&nbsp;  
* **AppraisalNormalizedAcquisitioned** – contains files before any appraisal or processing either unmodified imported MBOX files or converted to MBOX by Emailchemy. ‘Normalized’ describes the file format used by ePADD as a common format. The ePADD ‘Normalized’ format is MBOX.&nbsp;  
* If emails have been exported for preservation in the Appraisal Module then a directory **AppraisalNormalizedAppraised** is also present.

Users can export processed email in MBOX or EML format for long-term preservation or transfer to a digital preservation repository. Exported files are stored in the ProcessingNormalizedProcessed directory within the Processing Module's user/data/exportableAssets directory.

Users can also download all messages, non-restricted messages, or restricted messages directly in MBOX or EML format.

For detailed instructions, including Emailchemy licence requirements, export formats, download options, and file locations, see section **3.4 *Exporting Email for Preservation***.

&nbsp;

## 4.7 Downloadable .csv of search results

The header metadata for the email selection produced by the results of any search, generated by any means, (search, advanced search, from the correspondents list, from the lexicon) can be downloaded as a CSV file.

### Instructions

Select the ‘download messages as MBOX’ option on the selected search results page:  
![][image121]

The following options are presented on the next screen, where you can select ‘download search result in CSV format’:

![][image122]

The CSV will then download, consisting of the following header data fields:

* Date  
* From  
* To  
* CC  
* BCC  
* Subject  
* Message ID  
* Folder  
* Annotation  
* Label

![][image123]

## 

## 4.8 Exporting Message Headers

Message headers can be exported from Processing as a .CSV file to aid in social network analysis using the *Export Headers (CSV)* option. Exported fields include Date, From (resolved name), from (email address), To (resolved name), To (email address), CC (resolved name), CC (email address), BCC (resolved name), BCC (email address).

Select **Export** from the Processing Module menu at the top of the screen to navigate to the Export Screen, where you will find the section entitled ‘Export Headers (CSV):

![][image124]

## 4.9 Exporting Message Attachments

Users can also choose to export attachments according to various search criteria. The user can indicate whether they would like to export only those attachments which have not been recognized by Apache Tika (and are therefore not indexed with ePADD) for further review. Note that messages are only flagged in this way if they have also been appraised or processed by ePADD.

Select **Export** from the Processing Module menu at the top of the screen to navigate to the Export Screen, where you will find a section entitled ‘Export Attachments’:

![][image125]

## 4.10 Exporting Entities

Users can export recognized entities. The user can select whether they would like to export all entities or just particular entity types. Entities are exported as CSV and include the following fields: Entity name, number of related messages, start and end dates for related messages, entity type, and score (whether there is a direct match with an entity in DBPedia).

Select **Export** from the Processing Module menu at the top of the screen to navigate to the Export Screen, where you will find a section entitled ‘Export Entities’:

![][image126]

## 4.11 Exporting Correspondents

Users can export correspondents from the email archive. Users can choose to export only correspondents that have been confirmed via the Authorities interface (which include related unique identifiers), or only  unconfirmed correspondents. Unconfirmed correspondents are exported as CSV and include the following fields: Correspondent name, number of sent and received messages, number of mentions, and start and end dates for related messages.

Select **Export** from the Processing Module menu at the top of the screen to navigate to the Export Screen, where you will find a section entitled ‘Export correspondents’:

![][image127]

## 4.12 Exporting Original Text of Non-restricted Messages as Individual Text Files

Users can export all message text (subject line and message body) as individual TXT files to support topic modeling and other methods of text analysis.

Select **Export** from the Processing Module menu at the top of the screen to navigate to the Export Screen, where you will find the relevant section:

![][image128]

## 4.13 Exporting Email to the Delivery and Discovery Modules

Once you have reviewed the email archive, the final step is to export the processed email archive (along with whatever supplemental information or annotations you have added) for display in the Discovery and/or Delivery Modules.

Select **Export** from the Processing Module menu at the top of the screen to navigate to the Export Screen.

![][image129]

To export the archive to the Discovery and Delivery modules, first use the Browse button to select the location you wish to export under *Export to next ePADD module,* then click the confirm button.

**![][image130]**  
**Selecting a folder in which to export the email archive**

Please ensure you have enough disk space to save the complete email archive export or the export will fail.

This step may take some time as ePADD assembles the exported email archive and accompanying documentation.

![][image131]

![][image132]

**Exporting the processed email archive from the Processing Module creates two folders: one to support Discovery, and one to support Delivery**

# 5 Discovery Module

The Discovery Module is designed to run on a web-server, and allow remote users to search redacted header information and extracted entities from an email archive while limiting access to the full-text of messages..

## 5.1 Testing the Discovery Module

To test the Discovery module on your local workstation, copy the folder 'Email archive of \<name\>-Discovery,' exported through the Processing Module to the 'epadd-discovery' folder in your user directory. (Please see section [2: Getting Started](#2-getting-started) for more information about locating your user directory). If an 'epadd-discovery' folder does not already exist within your user directory, create that folder.

Then run ePADD in Discovery mode to view the archive as it will appear to remote users. To do this, set epadd.mode=discovery in your epadd.properties file and start ePADD using epadd-discovery-standalone.jar directly to test the module.

Note: Before exporting to Discovery, you may wish to reset the 'reviewed' status for all messages that were marked as 'reviewed' during Processing. To do this, while still in the Processing Module, navigate to Labels from the Browse screen and select 'Set labels for all messages.' This option is not available once you are viewing the archive in Discovery mode.

&nbsp;

## 5.2 Mounting the Discovery Module under a Web Server

Please follow the steps below to mount  the ePADD Discovery Module under a web server.

1\) Install the **Java runtime environmen**t (JRE 11+) on the machine that is going to mount the web server.

2\) Create a file called epadd.properties. It must be in plain text and stored in the user’s home directory (typically c:\\users\\\<username\>\\).

This file should contain the following text:

```
*epadd.mode=discovery*
```

&nbsp;

3\) Copy the exported “*ePADD archive of \<name\>-Discovery”* folder into \~/epadd-discovery of the home directory for the account that will be running the ePADD discovery module / web server.

**Important: Please ensure that you do not copy the Delivery version of the archive. It MUST be the Discovery version, as the Delivery version has unredacted information\!**

4\) Download and install the Discovery Module distribution file (**epadd-discovery-standalone.jar**). The web server is bundled with this distribution.

5\) Start ePADD with:

*java \-Djava.io.tmpdir=/tmp \-Depadd.mode.discovery=1 \-Dnobrowseropen= \-Xmx4g \-jar epadd-discovery-standalone.jar*  
Please adjust the \-Xmx option to assign as much memory you want, depending on the size of the archive(s).

Please also note that the java.io.tmpdir should be set to a directory where temporary files can be stored by ePADD. (java.io.tmpdir=/tmp will not work on Windows systems).

Please also ensure that the 9099 port is accessible from outside and will remain open.

**Caution: Please ensure that this directory is not periodically cleared by utilities like tmpwatch.**

6\) You can now open ePADD application in your internet browser by typing the URL  
[http\://{machine-name}:9099/](http://{machine-name}:9099/)

Where {machine-name} is the publicly visible name/IP of the machine where the epadd-discovery module is running.

## 5.3 Searching Entities Across Collections

```
ePADD supports cross-collection entity search. Navigate to Search from the Discovery navigation bar to access cross-collection entity search.
```

&nbsp;

## ![][image133]

Cross-collection entity search supports autocomplete. The user can navigate from search results to the related collection.

## 5.4 Navigating to a Collection

The Collections menu provides a user with access to the redacted email of any processed collections.

The user selects the collection they wish to access by clicking on the related image.

![][image134]

**Selecting a Collection**

The next screen provides basic details of the collection.  A user can proceed with viewing the collection by selecting the 'Enter' button.

![][image135]

**Access a Collection by clicking the 'ENTER' button.**

## 5.5 Browsing Email

The Browse Menu provides a user with access to several options to browse a redacted email archive, including by Correspondent as well as by extracted entity type (persons, organizations, and locations).

**![][image136]**

**Browse Menu in the Discovery Module**

## 5.6 Searching Email

![][image137]  
To search the email archive, navigate to the Search screen.

```
Beginning with v2.0, ePADD supports three types of search: simple search, advanced search, and multi-entity search.
```

&nbsp;

To use the simple search feature, simply type in your query into the simple search box available on the initial search screen. This will search across message headers (correspondents and subjects) and message text, as well as the text of all document attachments.

Once you have entered your term(s) and made your selection, click the *Search* button. The search string will appear highlighted on the message browse screen.

Advanced Search enables the user to to generate complex searches of email text, correspondents, attachments, and any actions previously taken on the messages, as well as a variety of other parameters.

![][image138]

![][image139]

Search Tips can be accessed by selecting the ![][image140]button in the top right corner of the *Advanced Search* screen.

ePADD also allows a user to search the email archive for entities contained in a given block of provided text, using *Multi-entity Search*. Any matching terms will be highlighted on the results page, and hyperlinked to corresponding messages.

![][image141]

**Search options within the Discovery Module**

![Macintosh HD:Users:jschne:Desktop:ePADD UI May 2015:Discovery:Screen Shot 2015-05-24 at 2.13.56 PM.png][image142]  
**Discovery Module search results, displaying only redacted headers and extracted entities**

# 6 Delivery Module

**![][image143]**  
**Delivery Module Navigation Bar**

The Delivery Module enables archival repositories to provide moderated full-text access to unrestricted email archives within a reading room.

## 6.1 Importing the Email Archive into the Delivery Module

In order for ePADD to recognize the email archive, you must first ensure it is in the proper location within your user directory. (See section [2: Getting Started](#2-getting-started) for more information about locating your user directory). If an 'epadd-delivery' folder does not already exist within your user directory, create that folder, and add the appropriate email archive (which should be a folder titled 'Email Archive of \<Archive Owner\>-Delivery') to that location.

Now run the ePADD software, and select the Delivery Module by pointing the browser to localhost:9099/epadd/switch-module and selecting the Discovery Module..

## 6.2 Navigating to a Collection

The Collections menu provides a user with moderated access to processed email archives.  
The user selects the collection they wish to access by clicking on the related image.

![][image144]

**Selecting a Collection**

The next screen provides basic details of the collection.  A user can proceed with viewing the collection by selecting the 'Enter' button.

![][image145]

## 6.3 Browsing the Email Archive

The Browse Menu provides a user with access to several options to browse the email archive, including by Correspondent or by extracted entity type (persons, organizations, and locations). The user can also generate complex tiered searches using a customizable lexicon, as well as browse image attachments, and download documents and other attachments to the workstation for further review.

![][image146]

**Browse Menu in the Delivery Module**

## 6.4 Searching the Email Archive

![][image147]![][image148]

To search the email archive, navigate to the Search screen.

Besides lexicons, accessible via the Browse page, ePADD supports five types of search: simple search, advanced search, multi-entity search, multi-term search, and correspondent list search.

To use the simple search feature, simply type in your query into the simple search box available on the initial search screen. This will search across message headers (correspondents and subjects) and message text, as well as the text of all document attachments.

Once you have entered your term(s) and made your selection, click the *Search* button. The search string will appear highlighted on the message browse screen.

Advanced Search enables the user to to generate complex searches of email text, correspondents, attachments, and any actions previously taken on the messages, as well as a variety of other parameters.  
![][image149]

![][image150]  
Search Tips can be accessed by selecting the help button ![][image72] in the top right corner of the *Advanced Search* screen.

ePADD also allows a user to search the email archive for entities contained in a given block of provided text, using *Multi-entity Search*. Any matching terms will be highlighted on the results page, and hyperlinked to corresponding messages.

Likewise, ePADD enables a user to search an email archive for multiple terms using *Multi-term Search*. Type or paste one term per line. Any matching terms will be highlighted on the results page, and hyperlinked to corresponding messages.

Finally, ePADD enables a user to search for a list of email addresses using Correspondent List Search. Type or paste one term per line to search for and apply labels to a set of email addresses.

## 6.5 Labeling messages

ePADD allows the user to label messages as 'reviewed' to assist with keeping track of research, to add an unlimited number of custom descriptive labels, as well as to request copies of messages from the email archive. Exported messages retain user annotations.

Users can optionally apply labels to a set of messages via the label management interface. Select the label management button, located to the above right of the message pane in the message browse/search result view:

# Appendix ePADD Data Flows

The following diagrams illustrate the progression of an email archive in ePADD, from import  through to the Appraisal, Processing and Discovery/Delivery modules.

This diagram shows how the email archive moves through the different stages when the optional Emailchemy license is activated:

![][image151]

While this diagram shows how the email archive moves through the different stages when the Emailchemy license is not activated:

![][image152]

[^1]: Different versions of the Emailchemy license can write a broader spectrum of formats. This narrower profile of formats was customized for the Emailchemy for ePADD license and selected for their preservation suitability.

[image1]: images/image1.png

[image2]: images/image2.png

[image3]: images/image3.png

[image4]: images/image4.png

[image5]: images/image5.png

[image6]: images/image6.png

[image7]: images/image7.png

[image8]: images/image8.png

[image9]: images/image9.png

[image10]: images/image10.png

[image11]: images/image11.png

[image12]: images/image12.png

[image13]: images/image13.png

[image14]: images/image14.png

[image15]: images/image15.png

[image16]: images/image16.png

[image17]: images/image17.png

[image18]: images/image18.png

[image19]: images/image19.png

[image20]: images/image20.png

[image21]: images/image21.png

[image22]: images/image22.png

[image23]: images/image23.png

[image24]: images/image24.png

[image25]: images/image25.png

[image26]: images/image26.png

[image27]: images/image27.png

[image28]: images/image28.png

[image29]: images/image29.png

[image30]: images/image30.png

[image31]: images/image31.png

[image32]: images/image32.png

[image33]: images/image33.png

[image34]: images/image34.png

[image35]: images/image35.png

[image36]: images/image36.png

[image37]: images/image37.png

[image38]: images/image38.png

[image39]: images/image39.png

[image40]: images/image40.png

[image41]: images/image41.png

[image42]: images/image42.png

[image43]: images/image43.png

[image44]: images/image44.png

[image45]: images/image45.png

[image46]: images/image46.png

[image47]: images/image47.png

[image48]: images/image48.png

[image49]: images/image49.png

[image50]: images/image50.png

[image51]: images/image51.png

[image52]: images/image52.png

[image53]: images/image53.png

[image54]: images/image54.png

[image55]: images/image55.png

[image56]: images/image56.png

[image57]: images/image57.png

[image58]: images/image58.png

[image59]: images/image59.png

[image60]: images/image60.png

[image61]: images/image61.png

[image62]: images/image62.png

[image63]: images/image63.png

[image64]: images/image64.png

[image65]: images/image65.png

[image66]: images/image66.png

[image67]: images/image67.png

[image68]: images/image68.png

[image69]: images/image69.png

[image70]: images/image70.png

[image71]: images/image71.png

[image72]: images/image72.png

[image73]: images/image73.png

[image74]: images/image74.png

[image75]: images/image75.png

[image76]: images/image76.png

[image77]: images/image77.png

[image78]: images/image78.png

[image79]: images/image79.png

[image80]: images/image80.png

[image81]: images/image81.png

[image82]: images/image82.png

[image83]: images/image83.png

[image84]: images/image84.png

[image85]: images/image85.png

[image86]: images/image86.png

[image87]: images/image87.png

[image88]: images/image88.png

[image89]: images/image89.png

[image90]: images/image90.png

[image91]: images/image91.png

[image92]: images/image92.png

[image93]: images/image93.png

[image94]: images/image94.png

[image95]: images/image95.png

[image96]: images/image96.png

[image97]: images/image97.png

[image98]: images/image98.png

[image99]: images/image99.png

[image100]: images/image100.png

[image101]: images/image101.png

[image102]: images/image102.png

[image103]: images/image103.png

[image104]: images/image104.png

[image105]: images/image105.png

[image106]: images/image106.png

[image107]: images/image107.png

[image108]: images/image108.png

[image109]: images/image109.png

[image110]: images/image110.png

[image111]: images/image111.png

[image112]: images/image112.png

[image113]: images/image113.png

[image114]: images/image114.png

[image115]: images/image115.png

[image116]: images/image116.png

[image117]: images/image117.png

[image118]: images/image118.png

[image119]: images/image119.png

[image120]: images/image120.png

[image121]: images/image121.png

[image122]: images/image122.png

[image123]: images/image123.png

[image124]: images/image124.png

[image125]: images/image125.png

[image126]: images/image126.png

[image127]: images/image127.png

[image128]: images/image128.png

[image129]: images/image129.png

[image130]: images/image130.png

[image131]: images/image131.png

[image132]: images/image132.png

[image133]: images/image133.png

[image134]: images/image134.png

[image135]: images/image135.png

[image136]: images/image136.png

[image137]: images/image137.png

[image138]: images/image138.png

[image139]: images/image139.png

[image140]: images/image140.png

[image141]: images/image141.png

[image142]: images/image142.png

[image143]: images/image143.png

[image144]: images/image144.png

[image145]: images/image145.png

[image146]: images/image146.png

[image147]: images/image147.png

[image148]: images/image148.png

[image149]: images/image149.png

[image150]: images/image150.png

[image151]: images/image151.png

[image152]: images/image152.png
