package edu.stanford.muse.util;

import net.fortuna.mstor.connector.mbox.MboxFolder;
import org.simplejavamail.converter.EmailConverter;

import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/***
 * Email conversion utilities for converting various message formats into another
 */
public class ConversionUtils {

    /***
     * Parsing a message object and printing to System.out
     * @param m Message
     */
    static public void messageToString(Message m) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
            m.writeTo(out);
            System.out.println(out.toString("UTF-8"));
        } catch (Exception e) {
            System.out.println("Error parsing Message to string." + e.getMessage());
        }
    }

    /***
     * Saving an array of Messages to a MboxFolder location in a single file.
     * Creates the folder file if it doesn't exist
     * @param msgs Message Array - message object to be written to an mbox file
     * @param mboxFolder MboxFolder - actually represents an mbox file that all messages will be appended to. Will create is not previously exists
     * @throws Exception - blocking exceptions for calling method to handle
     */
    static public void saveMessagesAsMbox(Message[] msgs, MboxFolder mboxFolder) throws Exception {
        if (!mboxFolder.exists()) {
            mboxFolder.create(3);
        }
        mboxFolder.open(Folder.READ_WRITE);
        mboxFolder.appendMessages(msgs);
        mboxFolder.close();
    }

    /**
     * Scans a File directory for Outlook msg files, transforms them and bundles then together into an mboxFolder
     * @param outlookFiles File - List of msg files to be bundled in mboxFile
     * @param mboxFolder MboxFolder - location of mbox file
     * @throws Exception
     */
    static public void outlookMsgsToMboxFolder(List<File> outlookFiles, MboxFolder mboxFolder) throws Exception {
        List<MimeMessage> mimeMessages = outlookFiles.stream().map(msg -> {
            return EmailConverter.outlookMsgToMimeMessage(msg);
        }).collect(Collectors.toCollection(ArrayList::new));
        saveMessagesAsMbox(mimeMessages.toArray(new Message[0]), mboxFolder);
    }
}
