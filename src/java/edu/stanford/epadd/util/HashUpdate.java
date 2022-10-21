/*
  Servlet to update checksum
*/
package edu.stanford.epadd.util;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import org.json.JSONObject;
import org.apache.commons.io.FileUtils;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;

import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import java.util.Arrays;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

@WebServlet(name = "HashUpdate", urlPatterns = {"/ajax/hashUpdate"})
public class HashUpdate extends HttpServlet {

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        JSONObject json = new JSONObject(); 
        String jsonResult="failed", jsonReason="";
        
        String archiveID = request.getParameter("archive");
        Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);  


            try {
                StandardSupportedAlgorithms algorithm[] = { StandardSupportedAlgorithms.MD5, StandardSupportedAlgorithms.SHA256};
                boolean includeHiddenFiles = false;
                Logger log = LogManager.getLogger(DelFolder.class);
                
                if (archive != null) {
                    archive.close();
                    
                    File tmp = Util.createTempDirectory();
                    tmp.delete();
                    FileUtils.moveDirectory(Paths.get(archive.baseDir+File.separatorChar+Archive.BAG_DATA_FOLDER).toFile(),tmp.toPath().toFile());
                    File wheretocopy = Paths.get(archive.baseDir).toFile();
                    Util.deleteDir(wheretocopy.getPath(),log);
                    FileUtils.moveDirectory(tmp.toPath().toFile(),wheretocopy);
                    
                    Bag bag = BagCreator.bagInPlace(Paths.get(archive.baseDir), Arrays.asList(algorithm), includeHiddenFiles);
                    archive.openForRead();
                    archive.setArchiveBag(bag);
                    
                    jsonResult = "ok";
                    jsonReason = "Checksum is updated";
                } else {
                    jsonReason = "Archive not found";
                }
                
            } catch (IOException | NoSuchAlgorithmException e1) {
                jsonReason = e1.getMessage();
            }

        json.put("result", jsonResult);
        json.put("reason", jsonReason);            
        
        try ( PrintWriter out = response.getWriter()) {
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            out.print(json.toString());
            out.flush();            
        } catch (Exception e) {
//            e.printStackTrace();
            Util.print_exception("DelFolder.PrintWriter", e, JSPHelper.log);
        }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Delete a folder";
    }// </editor-fold>

}
