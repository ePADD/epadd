package edu.stanford.epadd.util;

import com.weirdkid.emailchemy.api.Converter;
import com.weirdkid.emailchemy.api.ConverterListener;
import com.weirdkid.emailchemy.api.Format;
import com.weirdkid.emailchemy.api.OutputFormat;
import edu.stanford.muse.email.StaticStatusProvider;
import edu.stanford.muse.email.StatusProvider;
import edu.stanford.muse.util.Util;
import org.apache.logging.log4j.LogManager;

import java.io.File;
import java.util.Date;
import java.util.concurrent.CountDownLatch;
import java.util.function.Consumer;

public class EmlCreator implements ConverterListener {
    private final Consumer<StatusProvider> setStatusProvider;
    private final CountDownLatch countDownLatch;
    private int nMessagesCOnverted;


    public EmlCreator(Consumer<StatusProvider> setStatusProvider) {
        this.setStatusProvider = setStatusProvider;
        this.nMessagesCOnverted = 0;
        countDownLatch = new CountDownLatch(1);

    }

    private void sendStatus(String message) {
        if (setStatusProvider != null) {
            setStatusProvider.accept(new StaticStatusProvider(message));
        }
    }

    public String convertToEml(String pathToFile, String pathToMboxDir, int nEmails) {
        sendStatus("Converting " + nEmails + " emails to EML format");

        Format inFormat = Format.forName("mbox");
        OutputFormat outFormat = OutputFormat.forName("eml");
        File mboxFile = new File(pathToFile);

        String emlDirName = "eml_files";

        // Make sure folder exists
        File emlFolder = new File(pathToMboxDir + File.separatorChar + emlDirName);
        emlFolder.mkdir();
        Converter converter = new Converter(mboxFile, inFormat, emlFolder, outFormat);
        converter.addConverterListener(this);
        //converter.withLogFile("C:\\Users\\jochen\\log.log");
        converter.run();
        try {
            countDownLatch.await();
        } catch (InterruptedException e) {
            Util.print_exception("InterruptedException whilst converting emails", e, LogManager.getLogger(EmlCreator.class));
        }
        return emlFolder.toString();
    }


    @Override
    public void openingFile(File file) {

    }

    @Override
    public void messageConverted(Date date, String s, File file) {
        nMessagesCOnverted++;
        sendStatus(nMessagesCOnverted + " messages converted");
      //  status messages
    }

    @Override
    public void conversionComplete(boolean b) {
        countDownLatch.countDown();
    }

    @Override
    public void invalidInputFile(File file, Throwable throwable) {

    }
}
