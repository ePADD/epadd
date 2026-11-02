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
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.json.JSONObject;
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
        if (afile == null || afile.equals("")) {
            jsonReason = "Empty input";
        } else if (archive == null) {
            // without the archive the checksums could not be updated, so don't delete anything
            jsonReason = "Collection is not loaded";
        } else {
          Path file = sidecarFile(sidecarDir, afile);

          if (file != null) {
            try {
                Files.delete(file);
                archive.updateFileInBag(sidecarDir, archive.baseDir);
                jsonResult = "ok";
                jsonReason = "File is deleted";
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

    /**
     * Returns the sidecar file with the given name, or null if there is none. Only regular files directly in
     * sidecarDir are returned, so that names like "../blobs/x" cannot be used to delete other files of the archive.
     */
    static Path sidecarFile(String sidecarDir, String name) {
        Path dir = Paths.get(sidecarDir).toAbsolutePath().normalize();
        Path file;
        try {
            file = dir.resolve(name).normalize();
        } catch (InvalidPathException e) {
            return null;
        }
        if (!dir.equals(file.getParent()) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            return null;
        return file;
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
