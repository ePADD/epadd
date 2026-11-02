/*
  Servlet to delete a folder
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
import java.lang.ref.WeakReference;
import java.nio.file.Files;
import java.nio.file.Path;
import org.json.JSONObject;
import org.apache.commons.io.FileUtils;
import edu.stanford.muse.Config;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import edu.stanford.muse.webapp.ModeConfig;
import edu.stanford.muse.webapp.SimpleSessions;

@WebServlet(name = "DelFolder", urlPatterns = {"/ajax/delFolder"})
public class DelFolder extends HttpServlet {

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

        String newfolder = request.getParameter("folder");
        if (newfolder == null || newfolder.equals("")) {
            jsonReason = "Empty folder";
        }  else {
          File file = collectionFolder(newfolder, modeBaseDir());

          if (file != null) {
            try {
                // the page doesn't send the archive ID, so also look the archive up by its folder
                if (archive == null || !file.equals(new File(archive.baseDir).getCanonicalFile())) {
                    WeakReference<Archive> loaded = ArchiveReaderWriter.getArchiveFromGlobalArchiveMap(new File(newfolder).getAbsolutePath());
                    if (loaded == null)
                        loaded = ArchiveReaderWriter.getArchiveFromGlobalArchiveMap(file.getPath());
                    archive = loaded == null ? null : loaded.get();
                }
                if (archive != null) {
                    archive.close();
                    ArchiveReaderWriter.removeFromGlobalArchiveMap(archive.baseDir, archive);
                    archive = null;
                }
                Path leftover = deleteCollection(file);
                jsonResult = "ok";
                jsonReason = leftover == null ? "Collection is deleted"
                        : "Collection is deleted, but some of its files could not be removed from " + leftover;
            } catch (Exception e1) {
                Util.print_exception("DelFolder", e1, JSPHelper.log);
                jsonReason = e1.getMessage();
            }
          } else {
            jsonReason = "folder not found";
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
            Util.print_exception("DelFolder.PrintWriter", e, JSPHelper.log);
        }
    }

    /** The folder that holds the collections of the current mode, as on collection-detail.jsp. */
    private static String modeBaseDir() {
        if (ModeConfig.isProcessingMode())
            return Config.REPO_DIR_PROCESSING;
        if (ModeConfig.isDeliveryMode())
            return Config.REPO_DIR_DELIVERY;
        if (ModeConfig.isDiscoveryMode())
            return Config.REPO_DIR_DISCOVERY;
        return Config.REPO_DIR_APPRAISAL;
    }

    /**
     * Returns the collection folder for the given path, or null if it is not a collection, i.e. a folder directly
     * in modeBaseDir. This stops the servlet from deleting any other folder.
     */
    static File collectionFolder(String folder, String modeBaseDir) {
        try {
            File base = new File(modeBaseDir).getCanonicalFile();
            File file = new File(folder).getCanonicalFile();
            if (!file.isDirectory() || !base.equals(file.getParentFile()))
                return null;
            return file;
        } catch (IOException | SecurityException e) {
            return null;
        }
    }

    /**
     * Deletes a collection folder. It is renamed first, so if a file in it is still open (Windows), nothing is
     * deleted. Returns null, or the renamed folder if some files in it could not be deleted.
     */
    static Path deleteCollection(File collection) throws IOException {
        Path dir = collection.toPath();
        Path trash = dir.resolveSibling(dir.getFileName() + ".deleting-" + System.currentTimeMillis());
        Files.move(dir, trash);
        try {
            // without its session file, a partly deleted folder is no longer listed as a collection
            Files.deleteIfExists(trash.resolve(Archive.BAG_DATA_FOLDER).resolve(Archive.SESSIONS_SUBDIR).resolve("default" + SimpleSessions.getSessionSuffix()));
            FileUtils.deleteDirectory(trash.toFile());
            return null;
        } catch (IOException e) {
            Util.print_exception("Could not delete all files of the collection in " + trash, e, JSPHelper.log);
            return trash;
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
