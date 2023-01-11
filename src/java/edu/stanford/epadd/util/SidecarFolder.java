/*
  Servlet to list Sidecar files
*/
package edu.stanford.epadd.util;

import java.io.IOException;
//import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import org.json.JSONObject;
import org.json.JSONArray;
import edu.stanford.muse.index.Archive;
import edu.stanford.muse.index.ArchiveReaderWriter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
//import java.nio.file.attribute.BasicFileAttributes;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

@WebServlet(name = "SidecarFolder", urlPatterns = {"/ajax/sidecarFolder"})
public class SidecarFolder extends HttpServlet {

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

        String archiveID = request.getParameter("archive");
        Archive archive = ArchiveReaderWriter.getArchiveForArchiveID(archiveID); 
//        String sidecarDir = archive.baseDir + File.separator + Archive.BAG_DATA_FOLDER + File.separator + Archive.SIDECAR_DIR;
        String sidecarDir = "";
        if (archive != null) sidecarDir = archive.getSidecarDir();        

        JSONObject json = new JSONObject(); 
        
        if (!sidecarDir.equals("")) {
            Set<String> sidecarFiles = listFilesUsingFilesList(sidecarDir);
            List<String> files = new ArrayList<>(sidecarFiles);
        
            json.put("recordsTotal", files.size());
            json.put("recordsFiltered", files.size());

            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            JSONArray data = new JSONArray();
        
            for (String file : files) { 
                JSONArray row = new JSONArray();
                File file1 = new File(sidecarDir + File.separator + file);
                row.put(file);
                row.put(sdf.format(file1.lastModified()));
                data.put(row);
            }
            json.put("data", data);
        }
        response.setContentType("application/Json");
        response.getWriter().print(json.toString());
    }
    
    private Set<String> listFilesUsingFilesList(String dir) throws IOException {
            try (Stream<Path> stream = Files.list(Paths.get(dir))) {
                return stream
                .filter(file -> !Files.isDirectory(file))
                .map(Path::getFileName)
                .map(Path::toString)
                .collect(Collectors.toSet());
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
        return "Short description";
    }// </editor-fold>

}
