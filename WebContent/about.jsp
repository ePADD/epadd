<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<!DOCTYPE HTML>
<html>
<head>
    <title>About ePADD</title>

    <link rel="icon" type="image/png" href="images/epadd-favicon.png">

    <script src="js/jquery.js" type="text/javascript"></script>
    <script src="js/jquery/jquery.tools.min.js" type="text/javascript"></script>

    <link rel="stylesheet" href="bootstrap/dist/css/bootstrap.min.css">
    <script type="text/javascript" src="bootstrap/dist/js/bootstrap.min.js"></script>

    <jsp:include page="css/css.jsp"/>
    <script src="js/muse.js" type="text/javascript"></script>
    <script src="js/epadd.js"></script>
    <style>.heading {font-size: 16px; color: #69AA35}</style>
  </head>
</head>
<body>
<%@include file="header.jspf"%>
<h1>About ePADD</h1>
<div style="margin-left:160px;margin-right:160px">

<p>
    ePADD is free and open source software developed by Stanford University's Special Collections & University Archives
    that supports the appraisal, processing, preservation, discovery, and delivery of historical email archives. ePADD
    incorporates techniques from computer science and computational linguistics, including machine learning, natural
    language processing, and named entity recognition to help users access and search email collections of historical
    and cultural value.

    The ePADD Discovery Module is a multi-institutional email discovery platform that allows contributing institutions
    to publish their email collection metadata. If you use ePADD to process email collections and would like to publish
    your email collection metadata, please contact the ePADD team for more information at epadd_project@stanford.edu or
    consult the ePADD Discovery Module Contributor Guide

    Email messages in the ePADD Discovery Module have been redacted to ensure the privacy of donors and other
    correspondents. Please contact the host repository if you would like to request access to full messages, including
    any attachments.

    The ePADD Project has received funding from Stanford University Libraries Treat and Weber Grants in 2012, 2015, and
    2019. From 2012-2015, The ePADD project has received funding from the National Historical Publications & Records
    Commission to develop the first full version of the software package. From 2015-2018, the project received funding
    from the Institute of Museum and Library Services to develop a further six versions of ePADD. In 2020, the ePADD
    project received funding from the Andrew W. Mellon Foundation to continue development on the software for one year.


</p>
</div>

<h1>ePADD Software</h1>
<div style="margin-left:160px;margin-right:160px">
<p>
    The ePADD software client is browser-based and compatible with Chrome and Firefox. It is optimized for Windows 10,
    OS X 10.13, and Ubuntu 16.04 machines, using Java 12.

    <%--[note on link to user manual]--%>

    <a target="_blank" href="https://github.com/ePADD/epadd/releases/latest">Download the latest version of the software on GitHub.</a>
</p>
</div>
<h1>Technical Information</h1>

  <div style="margin-left:160px;margin-right:160px">
      <p>
        ePADD version <%= edu.stanford.epadd.Version.version%>  © Stanford University
        <p>Build information:
        ePADD <%= edu.stanford.epadd.Version.buildInfo%> <br/>

          © Stanford University
      <p>
      The ePADD logo, project documentation (including installation and user guide), and other non-software products of
      the ePADD team are subject to the Creative Commons Attribution 2.0 Generic license (CC By 2.0).
        <p>
      Unless otherwise indicated, software items in this repository are distributed under the terms of the Apache
      License, Version 2.0 (the "License"); you may not use these files except in compliance with the License. You may
      obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required by applicable law or
      agreed to in writing, software distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
      WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language
      governing permissions and limitations under the License.
      <p>
      For more information about ePADD, including links to an installation and user guide, mailing list registration,
      and community forum support, please visit <a href="https://library.stanford.edu/projects/epadd">this page</a>.
  </p>
<h1>Credits</h1>
For a full list of credits, please <a href="https://library.stanford.edu/projects/epadd/about">click here</a>.
      <!--
      <p>
      <p></p>
      <div class="block">
      <div class="heading">Research and Development</div>

      Sudheendra Hangal, Ashoka University and Amuse Labs <br/>
      Vihari Piratla, Amuse Labs<br/>
      Sit Manovit, iXora Inc.<br/>
      Peter Chan, Stanford University Libraries<br/>
      Glynn Edwards, Stanford University Libraries<br/>
      Josh Schneider, Stanford University Libraries<br/>
      </div>
      <p>

      <div class="block">
      <div class="heading">Design</div>
      Saumya Sarangi, Lollypop Design<br/>
      Mandeep RJ, Lollypop Design<br/>
      </div>
      <p>

      <div class="block">
      <div class="heading">Initial specifications (requirements and wireframes)</div>

      Daniel Hartwig, Stanford University Libraries<br/>
      Daniel Jarvis, Hoover Institute, Stanford<br/>
      Lisa Miller, Hoover Institute, Stanford<br/>
      Aimee Morgan, formerly Stanford University Libraries<br/>
      Laura O'Hara, formerly SLAC National Accelerator Laboratory<br/>
      </div>
      </p>
      <p>
      <div class="block">
      <div class="heading">Testing and Collaboration</div>
          Donald Mennerich, New York University Libraries<br/>
          Susan Thomas, Bodleian Library, Oxford University<br/>
          Riccardo Ferrante and Lynda Schmitz Fuhrig, Smithsonian Institution Archives<br/>
          Terry Catapano, Stephen Davis, and Dina Sokolova, Columbia University<br/>
      </div>
      <p>
      <div class="block">
          <div class="heading">Advisors</div>
              Jeremy Leighton John, British Library<br/>
          Monica S. Lam, Stanford University<br/>
          Phillip R. Malone, Stanford University<br/>
          Pam Maples, Stanford University<br/>
          Meg McAleer, Library of Congress<br/>
          Chris Prom, University of Illinois<br/>
          Ben Shneiderman, University of Maryland<br/>
          Jeff Ubois, Macarthur Foundation<br/>
      <p>
      <div class="block">
          <div class="heading">Funding</div>

      <a href="http://www.archives.gov/nhprc/NHPRC">National Historical Publications &amp; Records Commission</a><br/>
      Payton J. Treat Fund from the Stanford University Libraries <br/>
      U.S. <a href="http://nsf.gov">National Science Foundation</a> (for Muse)<br/>
      <a href="http://mobisocial.stanford.edu">Mobisocial Laboratory</a> at Stanford University (for Muse)<br/>
      <p>
      <p>
-->
      <h1>Software</h1><p>
      Under <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache License 2.0</a>:<br/>
      <a href="http://apache.org">Apache</a> Commons (fileupload, lang3, io, httpclient, cli, codec), lucene, tika, opennlp, tomcat, maven, ant © Apache Software Foundation<br/>
      <a href="http://mobisocial.stanford.edu">Muse</a> © Mobisocial Laboratory, Stanford University <br/>
      <a href="https://code.google.com/p/google-gson/">Google Gson</a> © Google<br/>
      <a href="http://opencsv.sourceforge.net">OpenCSV</a> © Glen Smith and others <br/>
      <p>
      <a href="http://java.oracle.com">Java SE Platform Products</a> © Oracle - <a href="http://www.oracle.com/technetwork/java/javase/terms/license/index.html">Oracle BCL</a><br/>
      <a href="http://launch4j.sourceforge.net/">Launch4j</a> © Grzegorz Kowal <a href="http://opensource.org/licenses/BSD-3-Clause">BSD 3-clause license and <a href="http://opensource.org/licenses/mit-license.html">MIT license</a> <br/>
      <a href="http://fontawesome.io">Font Awesome</a> © Dave Gandy <a href="http://opensource.org/licenses/mit-license.html">MIT license</a>, <a href="http://scripts.sil.org/OFL">SIL OFL 1.1</a><br/>
      <a href="http://fontawesome.io">Bootstrap</a> © Twitter <a href="https://github.com/twbs/bootstrap/blob/master/LICENSE">MIT license</a><br/>
      <a href="http://jsoup.org">Jsoup</a> © Jonathan Hedley <a href="https://jsoup.org/license">MIT License</a><br/>
      <a href="http://d3js.org">D3.js</a> © Mike Bostock <a href="https://opensource.org/licenses/BSD-3-Clause">BSD 3-clause License<br/>
      <a href="http://jquery.org">jQuery</a> © jQuery Foundation <a href=https://jquery.org/license/">MIT License</a><br/>
      <a href="https://www.abeautifulsite.net/jquery-file-tree">jQuery File Tree</a> © A Beautiful Site <a href="https://www.abeautifulsite.net/jquery-file-tree#licensing">MIT License</a><br/>
      <a href="https://www.devbridge.com/sourcery/components/jquery-autocomplete/">jQuery AutoComplete</a> © Tomas Kirda <a href="https://github.com/devbridge/jQuery-Autocomplete/blob/master/dist/license.txt">MIT License</a>
      <p></p>
      <a href="https://github.com/cooliris/embed-wall">Cooliris Image wall</a> (Special thanks to Soujanya Bhumkar, Austin Shoemaker and team for making the software available to us)<br/>
      Copyright (c) 2015 Yahoo, Inc.

      This software is provided 'as-is', without any express or implied
      warranty. In no event will the authors be held liable for any damages
      arising from the use of this software.

      Permission is granted to anyone to use this software for any purpose,
      including commercial applications, and to alter it and redistribute it
      freely, subject to the following restrictions:

      1. The origin of this software must not be misrepresented; you must not
      claim that you wrote the original software. If you use this software
      in a product, an acknowledgement in the product documentation would be
      appreciated but is not required.
      2. Altered source versions must be plainly marked as such, and must not be
      misrepresented as being the original software.
      3. This notice may not be removed or altered from any source distribution.

      <p></p>

      <a href="http://www.csie.ntu.edu.tw/~cjlin/libsvm/">LIBSVM</a> <br/>

      Copyright (c) 2000-2014 Chih-Chung Chang and Chih-Jen Lin
      All rights reserved.

      Redistribution and use in source and binary forms, with or without
      modification, are permitted provided that the following conditions
      are met:

      1. Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

      2. Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.

      3. Neither name of copyright holders nor the names of its contributors
      may be used to endorse or promote products derived from this software
      without specific prior written permission.


      THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
      ``AS IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
      LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
      A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE REGENTS OR
      CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
      EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
      PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
      PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
      LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
      NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
      SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

      <p></p>

      <a href="http://mstor.sourceforge.net/index.html">Mstor</a><br/>
      Copyright (c) 2007, Ben Fortuna
      All rights reserved.

      Redistribution and use in source and binary forms, with or without modification, are permitted provided that the following conditions are met:

      Redistributions of source code must retain the above copyright notice, this list of conditions and the following disclaimer.
      Redistributions in binary form must reproduce the above copyright notice, this list of conditions and the following disclaimer in the documentation and/or other materials provided with the distribution.
      Neither the name of Ben Fortuna nor the names of any other contributors may be used to endorse or promote products derived from this software without specific prior written permission.
      THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

</div>

</body>
</html>
