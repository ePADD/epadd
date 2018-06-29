<%@page language="java" contentType="application/json;charset=UTF-8"%>
<%@page trimDirectiveWhitespaces="true"%>
<%@page language="java" import="org.json.*"%>
<%@page language="java" import="edu.stanford.muse.webapp.*"%>
<%@page language="java" import="edu.stanford.muse.index.*"%>
<%@page import="edu.stanford.muse.AddressBookManager.CorrespondentAuthorityMapper"%><%@ page import="gov.loc.repository.bagit.verify.BagVerifier"%><%@ page import="java.io.IOException"%><%@ page import="gov.loc.repository.bagit.domain.Bag"%><%@ page import="gov.loc.repository.bagit.exceptions.*"%><%@ page import="edu.stanford.muse.util.Util"%>
<%
// does a login for a particular account, and adds the emailStore to the session var emailStores (list of stores for the current doLogin's)
JSPHelper.setPageUncacheable(response);

JSONObject result = new JSONObject();
Archive archive = JSPHelper.getArchive(request);
if (archive == null) {
    JSONObject obj = new JSONObject();
    obj.put("status", 1);
    obj.put("error", "No archive in session");
    out.println (obj);
    JSPHelper.log.info(obj);
    return;
}

Bag bag = archive.getArchiveBag();
BagVerifier bv = new BagVerifier();
String errorMessage="";
        try {
            bv.isValid(bag, true);
        } catch (IOException e) {
            e.printStackTrace();
            errorMessage = "IO Exception:" + e.getMessage();
        } catch (MissingPayloadManifestException e) {
            e.printStackTrace();
            errorMessage = "Payload manifest missing: " + e.getMessage();
        } catch (MissingBagitFileException e) {
            e.printStackTrace();
            errorMessage = "Bagit file missing: " + e.getMessage();
        } catch (MissingPayloadDirectoryException e) {
            e.printStackTrace();
            errorMessage = "Payload directory missing: " + e.getMessage();
        } catch (FileNotInPayloadDirectoryException e) {
            e.printStackTrace();
            errorMessage = "No file in payload directory: " + e.getMessage();
        } catch (InterruptedException e) {
            e.printStackTrace();
            errorMessage = "Validation procedure interrupted :" + e.getMessage();
        } catch (MaliciousPathException e) {
            e.printStackTrace();
            errorMessage = "Malicious Path found: " + e.getMessage();
        } catch (CorruptChecksumException e) {
            e.printStackTrace();
            errorMessage = "Checksum is corrupted: " + e.getMessage();
        } catch (VerificationException e) {
            e.printStackTrace();
            errorMessage = "Can not verify the bag: " + e.getMessage();
        } catch (UnsupportedAlgorithmException e) {
            e.printStackTrace();
            errorMessage = "Algorithm used for constructing the bag is not supported: " + e.getMessage();
        } catch (InvalidBagitFileFormatException e) {
            e.printStackTrace();
            errorMessage = "Bag file format is invalid: " + e.getMessage();
        }

        if(Util.nullOrEmpty(errorMessage)){
            result.put("Status",0);
            result.put("resultPage","verify-bag.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&checkDone=true&result=success");
        }else{
            result.put("Status",1);
            result.put("resultPage","verify-bag.jsp?archiveID="+ArchiveReaderWriter.getArchiveIDForArchive(archive)+"&checkDone=true&result=failure");
        }
out.println (result.toString());
%>
