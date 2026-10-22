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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
    private static String tmpDir = "NOT_YET_INITIALISED";
    private static String licenseStatus = "No license dongle present.";
    private List<File> files;
    private Format inFormat;
    private int nFolders;
    private int nFoldersConverted;
    private String lastMessage = "";
    private File currentFolder;
    private File sourceRoot; // the file or folder selected for conversion
    private static final String EML_FORMAT = "eml";
    private static final Logger log =  LogManager.getLogger(FolderInfo.class);

    // Emailchemy converts on its own thread and reports back only through the ConverterListener callbacks, so
    // failures there are easily lost and the import page waits forever. The fields below make sure every
    // conversion ends with either a success or an error result (see succeed() and fail()).
    private static final long INACTIVITY_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(10);
    private static volatile EmailConvert activeConversion; // the conversion that receives Emailchemy's uncaught exceptions
    private static boolean uncaughtExceptionHandlerInstalled = false;
    private volatile boolean finished;
    private volatile boolean cancelled;
    private volatile long lastActivity;
    private ScheduledExecutorService watchdog;

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
        prepareFiles(p_inFile, p_inFormat);

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
                cancelled = true;
                endConversion();
                interruptConverter();
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
        finished = false;
        cancelled = false;
        activeConversion = this;
        installUncaughtExceptionHandler();

        // this runs on OperationInfo's executor, which silently swallows exceptions, so report every failure explicitly
        try {
            this.inFormat = Format.forName(p_inFormat);
            if (inFormat == null) {
                fail("Unsupported input format for Emailchemy: " + p_inFormat, null);
                return;
            }
            if (files.isEmpty()) {
                fail("No files found to convert in " + p_inFile, null);
                return;
            }
            this.outFolder = Files.createTempDirectory(EPADD_EMAILCHEMY_TMP).toString();
            tmpDir = outFolder;

            startWatchdog();
            convertFolder(files.remove(files.size() - 1), outFolder, inFormat);
        } catch (Throwable t) {
            fail("Could not start converting " + p_inFile + " with Emailchemy: " + t, t);
        }
    }

    /** Reports a successful conversion to the import page. */
    private synchronized void succeed() {
        if (finished)
            return;
        endConversion();
        resultJsonObject.put("status", 0);
        processRequest();
    }

    /** Reports a failed conversion to the import page (only the first failure is reported) and stops converting. */
    private synchronized void fail(String message, Throwable t) {
        if (finished || cancelled)
            return;
        endConversion();
        log.error("Email conversion failed: " + message, t);
        interruptConverter();
        // put "error" before "status": the import page treats the result as ready as soon as it is non-empty
        resultJsonObject.put("error", message);
        resultJsonObject.put("status", 1);
    }

    /**
     * Converter.interrupt() throws a NullPointerException when the converter has no running conversion (not yet
     * started or already finished), which made cancelling an import fail with an error.
     */
    private void interruptConverter() {
        if (converter == null)
            return;
        try {
            converter.interrupt();
        } catch (Exception e) {
            log.warn("Could not interrupt Emailchemy converter (probably not converting at the moment): " + e);
        }
    }

    private void endConversion() {
        finished = true;
        if (watchdog != null)
            watchdog.shutdownNow();
        if (activeConversion == this)
            activeConversion = null;
    }

    /** Fails the conversion if Emailchemy makes no progress for INACTIVITY_TIMEOUT_MS, e.g. when it stops without calling back. */
    private void startWatchdog() {
        lastActivity = System.currentTimeMillis();
        watchdog = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "EmailConvert-watchdog");
            thread.setDaemon(true);
            return thread;
        });
        watchdog.scheduleWithFixedDelay(() -> {
            if (System.currentTimeMillis() - lastActivity > INACTIVITY_TIMEOUT_MS)
                fail("Emailchemy stopped responding while converting " + currentFolder + " (no progress for "
                        + TimeUnit.MILLISECONDS.toMinutes(INACTIVITY_TIMEOUT_MS) + " minutes).", null);
        }, 1, 1, TimeUnit.MINUTES);
    }

    /**
     * Exceptions thrown on Emailchemy's own conversion thread are not reported through ConverterListener; that
     * thread just dies and no further callbacks arrive. Report such exceptions as a failure of the active conversion,
     * then pass them on to the previously installed handler as before.
     */
    private static synchronized void installUncaughtExceptionHandler() {
        if (uncaughtExceptionHandlerInstalled)
            return;
        Thread.UncaughtExceptionHandler previous = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, e) -> {
            EmailConvert active = activeConversion;
            if (active != null && isFromEmailchemy(e))
                active.fail("Emailchemy failed while converting " + active.currentFolder + ": " + e, e);
            if (previous != null) {
                previous.uncaughtException(thread, e);
            } else {
                System.err.print("Exception in thread \"" + thread.getName() + "\" ");
                Util.print_exception(e, log);
            }
        });
        uncaughtExceptionHandlerInstalled = true;
    }

    private static boolean isFromEmailchemy(Throwable e) {
        for (Throwable cause = e; cause != null; cause = cause.getCause())
            for (StackTraceElement element : cause.getStackTrace())
                if (element.getClassName().startsWith("com.weirdkid."))
                    return true;
        return false;
    }

    /** Sets up the list of files and folders that are passed to Emailchemy one by one. */
    private void prepareFiles(String p_inFile, String p_inFormat) {
        files = new ArrayList<>();
        nFoldersConverted = 0;

        File dir = new File(p_inFile);
        sourceRoot = dir;
        File[] directoryListing = dir.listFiles();
        if (directoryListing != null && EML_FORMAT.equalsIgnoreCase(p_inFormat)) {
            // Emailchemy does not convert a folder in EML format (it returns without converting or calling back),
            // so pass it every .eml file individually, including those in subfolders
            files.addAll(collectFiles(dir));
        } else if (directoryListing != null) {
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
    }

    /** All regular files below dir, in subfolders too; hidden files such as .DS_Store are skipped. */
    private static List<File> collectFiles(File dir) {
        List<File> result = new ArrayList<>();
        File[] children = dir.listFiles();
        if (children == null) {
            log.warn("Cannot list contents of folder " + dir + ", skipping it");
            return result;
        }
        for (File child : children) {
            if (child.getName().startsWith("."))
                continue;
            if (child.isDirectory())
                result.addAll(collectFiles(child));
            else if (child.isFile())
                result.add(child);
        }
        return result;
    }

    private void convertFolder(File source, String outFolder, Format inFormat) {
        // keep the subfolder structure of the source, so that the folders are preserved in ePADD
        String relativePath = Util.filePathTail(source.toString());
        if (sourceRoot != null && !source.equals(sourceRoot) && source.toPath().startsWith(sourceRoot.toPath()))
            relativePath = sourceRoot.toPath().relativize(source.toPath()).toString();
        String outFolderWithEmailFileName = outFolder + File.separatorChar + relativePath;
        new File(outFolderWithEmailFileName).mkdirs();
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
        lastActivity = System.currentTimeMillis();
    }

    @Override
    public void messageConverted(Date date, String s, File file) {
        lastActivity = System.currentTimeMillis();
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
        lastActivity = System.currentTimeMillis();
        if (finished || cancelled)
            return;
        if (!b)
            log.warn("Emailchemy reported that converting " + currentFolder + " was not successful");
        nFoldersConverted++;
        nMessageConverted = 0;
        // called on Emailchemy's thread: an exception here would otherwise end the conversion without any result
        try {
            if (files.size() > 0) {
                currentFolder = files.remove(files.size() - 1);
                convertFolder(currentFolder, outFolder, inFormat);
            } else {
                log.info("Emailchemy conversion complete");
                succeed();
            }
        } catch (Throwable t) {
            fail("Converting " + currentFolder + " with Emailchemy failed: " + t, t);
        }
    }

    @Override
    public void cancel() {

    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void invalidInputFile(File file, Throwable throwable) {
        lastActivity = System.currentTimeMillis();
        log.warn("Emailchemy could not convert " + file + ", skipping it", throwable);
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
