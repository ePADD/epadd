/*
  Servlet to delete a Sidecar File
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


@WebServlet(name = "sidecarFileDelete", urlPatterns = {"/ajax/sidecarFileDelete"})
public class SidecarFileDelete extends HttpServlet {

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
        String jsonResult="failed", jsonReason;
        
        String archiveID = request.getParameter("archive");
        Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID);  
        String sidecarDir = "";
//        if (archive != null) sidecarDir = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.SIDECAR_DIR;
        if (archive != null) sidecarDir = archive.getSidecarDir();
        
        String afile = request.getParameter("file");
        if (afile.equals("")) {
            jsonReason = "Empty input";            
        }  else {
//          afile = afile.replaceAll("\\/", "/");
//          afile = afile.replaceAll("\\\\", "/");

  	  java.io.File file = FileUtils.getFile(sidecarDir + File.separator + afile);
        
          if(file.exists()) {
            try {
                file.delete();
                if (archive!=null) {
                    archive.updateFileInBag(sidecarDir, archive.baseDir);
                    jsonResult = "ok";
                    jsonReason = "File is deleted";
                }
                else {
                    jsonReason = "Failed in updating Checksum";
                }
            } catch (Exception e1) {
                jsonReason = e1.getMessage();
            } 
          } else {
            jsonReason = "File not found";
          }            
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
            Util.print_exception("SidecarFileDelete", e, JSPHelper.log);
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
