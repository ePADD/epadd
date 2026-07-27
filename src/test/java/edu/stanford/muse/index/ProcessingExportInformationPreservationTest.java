package edu.stanford.muse.index;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.mail.Address;
import javax.mail.BodyPart;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.URLName;
import javax.mail.internet.ContentType;
import javax.mail.internet.MimeUtility;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ProcessingExportInformationPreservationTest {

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void exportToProcessingPreservesMessageInformation() throws Exception {
        File archiveRoot = temporaryFolder.newFolder("archive");
        File appraisalAssets = new File(archiveRoot,
                Archive.BAG_DATA_FOLDER + File.separator
                        + Archive.EXPORTABLE_ASSETS_SUBDIR + File.separator
                        + Archive.EXPORTABLE_ASSETS_APPRAISAL_NORMALIZED_APPRAISED_SUBDIR);
        Files.createDirectories(appraisalAssets.toPath());

        File sourceMbox = new File(appraisalAssets, "inbox.mbox");
        write(sourceMbox, multipartMessageWithAttachment());

        Archive archive = Archive.createArchive();
        archive.baseDir = archiveRoot.getAbsolutePath();
        EmailExporter exporter = new EmailExporter(EmailExporter.EXPORT_PROCESSING, archive, null);

        Method saveProcessed = EmailExporter.class.getDeclaredMethod(
                "saveProcessed", ArrayList.class, String.class,
                gov.loc.repository.bagit.domain.Bag.class, String.class);
        saveProcessed.setAccessible(true);
        String baseDir = archiveRoot.getAbsolutePath() + File.separator;
        String processingAssets = (String) saveProcessed.invoke(exporter, null, baseDir, null, baseDir);

        assertNotNull("Processing export should produce an assets directory", processingAssets);
        File exportedMbox = new File(processingAssets, sourceMbox.getName());
        assertEquals(semanticMessages(sourceMbox), semanticMessages(exportedMbox));
    }

    private static List<String> semanticMessages(File mbox) throws Exception {
        Properties properties = new Properties();
        properties.setProperty("mstor.mbox.metadataStrategy", "NONE");
        properties.setProperty("mstor.mbox.cacheBuffers", "disabled");
        properties.setProperty("mstor.cache.disabled", "true");
        properties.setProperty("mstor.mbox.encoding", "UTF-8");

        Session session = Session.getInstance(properties);
        Store store = session.getStore(new URLName("mstor:" + mbox.getAbsolutePath()));
        store.connect();
        Folder folder = store.getFolder(mbox.getAbsolutePath());
        folder.open(Folder.READ_ONLY);
        try {
            List<String> result = new ArrayList<>();
            for (Message message : folder.getMessages()) {
                StringBuilder snapshot = new StringBuilder();
                appendHeader(snapshot, "message-id", message.getHeader("Message-ID"));
                appendAddresses(snapshot, "from", message.getFrom());
                appendAddresses(snapshot, "to", message.getRecipients(Message.RecipientType.TO));
                appendAddresses(snapshot, "cc", message.getRecipients(Message.RecipientType.CC));
                appendAddresses(snapshot, "bcc", message.getRecipients(Message.RecipientType.BCC));
                snapshot.append("subject:").append(message.getSubject()).append('\n');
                Date sentDate = message.getSentDate();
                snapshot.append("sent-date:").append(sentDate == null ? "" : sentDate.getTime()).append('\n');
                appendPart(snapshot, message, 0);
                result.add(snapshot.toString());
            }
            return result;
        } finally {
            folder.close(false);
            store.close();
        }
    }

    private static void appendPart(StringBuilder snapshot, Part part, int depth) throws Exception {
        String indent = repeat("  ", depth);
        ContentType contentType = new ContentType(part.getContentType());
        String baseType = contentType.getBaseType().toLowerCase(Locale.ROOT);
        snapshot.append(indent).append("content-type:").append(baseType).append('\n');
        snapshot.append(indent).append("disposition:").append(nullToEmpty(part.getDisposition())).append('\n');
        snapshot.append(indent).append("filename:").append(decode(nullToEmpty(part.getFileName()))).append('\n');

        Object content = part.getContent();
        if (content instanceof Multipart) {
            Multipart multipart = (Multipart) content;
            snapshot.append(indent).append("parts:").append(multipart.getCount()).append('\n');
            for (int i = 0; i < multipart.getCount(); i++) {
                BodyPart child = multipart.getBodyPart(i);
                appendPart(snapshot, child, depth + 1);
            }
        } else if (content instanceof Message) {
            appendPart(snapshot, (Message) content, depth + 1);
        } else if (baseType.startsWith("text/")) {
            snapshot.append(indent).append("text:")
                    .append(normalizeLineEndings(String.valueOf(content))).append('\n');
        } else {
            snapshot.append(indent).append("sha256:").append(sha256(part.getInputStream())).append('\n');
        }
    }

    private static void appendHeader(StringBuilder snapshot, String name, String[] values) {
        snapshot.append(name).append(':');
        if (values != null)
            snapshot.append(String.join(",", values));
        snapshot.append('\n');
    }

    private static void appendAddresses(StringBuilder snapshot, String name, Address[] addresses) {
        snapshot.append(name).append(':');
        if (addresses != null)
            snapshot.append(String.join(",", Arrays.stream(addresses)
                    .map(Address::toString).toArray(String[]::new)));
        snapshot.append('\n');
    }

    private static String sha256(InputStream input) throws Exception {
        try (InputStream in = input) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1)
                digest.update(buffer, 0, read);
            StringBuilder hex = new StringBuilder();
            for (byte b : digest.digest())
                hex.append(String.format("%02x", b));
            return hex.toString();
        }
    }

    private static String decode(String value) throws Exception {
        return value.isEmpty() ? value : MimeUtility.decodeText(value);
    }

    private static String normalizeLineEndings(String value) {
        return value.replace("\r\n", "\n").replace('\r', '\n');
    }

    private static String repeat(String value, int count) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < count; i++)
            result.append(value);
        return result.toString();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static void write(File file, String value) throws Exception {
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(value.getBytes(StandardCharsets.UTF_8));
        }
    }

    private static String multipartMessageWithAttachment() {
        return "From sender@example.org Sat Jan 01 00:00:00 2022\n"
                + "Message-ID: <processing-round-trip@example.org>\n"
                + "Date: Sat, 1 Jan 2022 00:00:00 +0000\n"
                + "From: Alice Example <sender@example.org>\n"
                + "To: Bob Example <recipient@example.org>\n"
                + "Subject: Processing export preservation\n"
                + "MIME-Version: 1.0\n"
                + "Content-Type: multipart/mixed; boundary=outer-boundary\n\n"
                + "--outer-boundary\n"
                + "Content-Type: multipart/alternative; boundary=alternative-boundary\n\n"
                + "--alternative-boundary\n"
                + "Content-Type: text/plain; charset=UTF-8\n"
                + "Content-Transfer-Encoding: quoted-printable\n\n"
                + "Hello Bob,=0APlain text body.\n"
                + "--alternative-boundary\n"
                + "Content-Type: text/html; charset=UTF-8\n\n"
                + "<html><body><p>Hello Bob,</p><p>HTML body.</p></body></html>\n"
                + "--alternative-boundary--\n"
                + "--outer-boundary\n"
                + "Content-Type: application/octet-stream; name=example.bin\n"
                + "Content-Disposition: attachment; filename=example.bin\n"
                + "Content-Transfer-Encoding: base64\n\n"
                + "AAECAwQF\n"
                + "--outer-boundary--\n";
    }
}
