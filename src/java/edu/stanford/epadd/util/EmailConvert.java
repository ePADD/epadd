/*
  Servlet to call Emailchemy
*/
package edu.stanford.epadd.util;

import com.google.common.collect.Multimap;
import com.weirdkid.emailchemy.api.*;
import edu.stanford.muse.email.FolderInfo;
import edu.stanford.muse.email.StatusProvider;
import edu.stanford.muse.util.JSONUtils;
import edu.stanford.muse.util.Util;
import edu.stanford.muse.webapp.JSPHelper;
import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.json.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;


@WebServlet(name = "EmailConvert", urlPatterns = {"/ajax/emailConvert"})
public class EmailConvert extends HttpServlet implements StatusProvider, ConverterListener, LicenseDongleListener {

    // If you change "EPADD_EMAILCHEMY_TMP" then you also have to change
    // in epadd.js: var isNonMbox = checked[i].name.includes("epadd_emailchemy_tmp");
    public static final String EPADD_EMAILCHEMY_TMP = "epadd_emailchemy_tmp";
    JSONObject resultJsonObject;
    String outFolder;
    private int nMessageConverted = 0;
    private Consumer<StatusProvider> consumer;
    private String status = "Start converting emails to Mbox ...";
    private int pctComplete = 0;
    private static String tmpDir = "";
    private static String licenseStatus = "No license dongle present.";
    private List<File> files;
    private Format inFormat;
    private int nFolders;
    private int nFoldersConverted;
    private String lastMessage = "";
    private File currentFolder;
    private static final Logger log =  LogManager.getLogger(FolderInfo.class);

    private static EmailConvert emailConvert;
    private static Random random = new Random();
    @Override
    public String getStatusMessage() {
        return JSONUtils.getStatusJSON(this.status, pctComplete, 10, 20);
    }

    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    private Converter converter;

//    @Controller
//    @RequestMapping(value = "clause")
//    public class ClauseController {
//
//        @RequestMapping(value="getUpdate")
//        @ResponseBody
//        public String selectClause(ModelMap model) {
//            DbManager dbManager = DAO.getDbManager;
//            return "dbManager.latitude";
//        }
//    }
    public static String getLicenseStatus() {
//        int i = random.nextInt();
//        if (i % 2 == 0)
//        {
//            System.out.println("XLicense dongle has been removed");
//            return "License dongle has been removed";
//        }
//        else
//        {
//            System.out.println("XLicense active");
//
//            return "License active";
//        }
//return "License active";
       if (licenseStatus.endsWith("."))
       {
           licenseStatus = licenseStatus.substring(0, licenseStatus.length() -1);
       }
       return licenseStatus;

    }

    protected void processRequest() {
        // Putting something in the resultJsonObject tells getStatus.jsp that the operation has finished.
        resultJsonObject.put("nMessages", nMessageConverted);
        resultJsonObject.put("outFolder", this.outFolder);
    }


    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">

    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        processRequest();
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request  servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException      if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {
        setUpConversion(request);
    }

    public static void activateLicense()
    {
        emailConvert = new EmailConvert();
        License license = new License();
        license.activateWithDongle(emailConvert);
        System.out.println("activateWithDongle");
    }
    private void setUpConversion(HttpServletRequest request) throws UnsupportedEncodingException {
        License license = new License();
        license.activateWithDongle(this);
        String p_inFile = request.getParameter("inFile");
        String p_inFormat = request.getParameter("inFormat");
        files = new ArrayList<>();
        nFoldersConverted = 0;

        File dir = new File(p_inFile);
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null) {
            for (File child : directoryListing) {
                files.add(child);
            }
        } else {
            files.add(dir);// Handle the case where dir is not really a directory.
            // Checking dir.isDirectory() above would not be sufficient
            // to avoid race conditions with another process that deletes
            // directories.
        }
        nFolders = files.size();

        String actionName = request.getRequestURI();
        String opID = request.getParameter("opID");
        Multimap<String, String> paramMap = JSPHelper.convertRequestToMap(request);
        //create a new operation object with the information necessary to run this long running async task.
        OperationInfo opinfo = new OperationInfo(actionName, opID, paramMap) {
            @Override
            public void onStart(JSONObject resultJSON) {

                //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
                //operationinfo object
                Consumer<StatusProvider> setStatusProvider = this::setStatusProvider;
                startConversion(p_inFile, p_inFormat, setStatusProvider, resultJSON);//(this.getParametersMap(), setStatusProvider, fsession, resultJSON);
            }

            @Override
            public void onCancel() {
//                //creating a lambda expression that will be used by functions to set the statusprovider without knowing the
//                //operationinfo object
//                Consumer<StatusProvider> setStatusProvider = statusProvider -> this.setStatusProvider(statusProvider);
//                cancelSetOwnerAddress(setStatusProvider);
                if (converter != null) {
                    converter.interrupt();
                }
            }
        };
        JSPHelper.setOperationInfo(request.getSession(), opID, opinfo);
        //</editor-fold>

        //<editor-fold desc="Starting the operation">
        opinfo.run();
    }

    public static void deleteTmpDir()
    {
        try {
            String s = getTmpDir();
            FileUtils.deleteDirectory(new File(getTmpDir()));
        } catch (IOException e) {
            log.warn("Temp directory " + getTmpDir() + " could not be deleted. " + e);
        }
    }

    private void startConversion(String p_inFile, String p_inFormat, Consumer<StatusProvider> setStatusProvider, JSONObject resultJson) {
        resultJsonObject = resultJson;
        if (setStatusProvider != null) {
            setStatusProvider.accept(this);
        }
        this.consumer = setStatusProvider;
        String p_outFolder = null;
        try {
            p_outFolder = Files.createTempDirectory(EPADD_EMAILCHEMY_TMP).toString();
            tmpDir = p_outFolder;
        } catch (IOException e) {
            e.printStackTrace();
        }
        this.outFolder = p_outFolder;
        new File(p_outFolder).mkdir();

        this.inFormat = Format.forName(p_inFormat);
        convertFolder(files.remove(files.size() - 1), outFolder, inFormat);
    }

    private void convertFolder(File source, String outFolder, Format inFormat) {
        String outFolderWithEmailFileName = outFolder + File.separatorChar + Util.filePathTail(source.toString());
        new File(outFolderWithEmailFileName).mkdir();
        lastMessage = "Found " + nFolders + " folders to convert. Finished converting " + nFoldersConverted + " folders. ";
        sendStatus(lastMessage);

        // Make sure folder exists
        File destination = new File(outFolderWithEmailFileName);

        //Just mbox for now.
        OutputFormat outFormat = OutputFormat.forName("mbox");
        converter = new Converter(source, inFormat, destination, outFormat);
        converter.addConverterListener(this);
        currentFolder = source;
        //converter.withLogFile("C:\\Users\\jochen\\log.log");
        converter.run();
    }

    public static String getTmpDir() {
        return tmpDir;
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

    @Override
    public void openingFile(File file) {

    }

    @Override
    public void messageConverted(Date date, String s, File file) {
        nMessageConverted++;
        if (nMessageConverted % 100 == 0) {
            String currentFolderString = "";
            if (currentFolder != null) {
                currentFolderString = currentFolder.toString();
            }
            sendStatus(lastMessage + " " + nMessageConverted + " messages converted (" + converter.getPercentComplete() + "%) for folder " + currentFolderString);
        }
    }

    private void sendStatus(String message) {
        this.status = message;
        this.consumer.accept(this);
    }

    @Override
    public void conversionComplete(boolean b) {
        nFoldersConverted++;
        nMessageConverted = 0;
        if (files.size() > 0) {
            currentFolder = files.remove(files.size() - 1);
            convertFolder(currentFolder, outFolder, inFormat);
        } else {
            System.out.println("Conversion Complete -------------------------------------");
            processRequest();
        }
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public void invalidInputFile(File file, Throwable throwable) {
        System.out.println("invalidInputFile");

    }

    @Override
    public void dongleRemoved() {
        licenseStatus = "License dongle has been removed.";
        System.out.println("License dongle has been removed.");
    }

    @Override
    public void dongleError(String s) {
        licenseStatus = "An error has occured with license dongle.";
        System.out.println("An error has occured with license dongle. Error: " + s);

    }

    @Override
    public void donglePresent() {
        licenseStatus = "License active.";
        System.out.println("License active.");

    }
}
