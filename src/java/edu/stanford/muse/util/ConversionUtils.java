package edu.stanford.muse.util;

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
  }
