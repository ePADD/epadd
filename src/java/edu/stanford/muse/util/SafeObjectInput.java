package edu.stanford.muse.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputFilter;
import java.io.ObjectInputStream;

/**
 * Opens Java serialization streams that can only create the classes ePADD itself stores.
 *
 * Archives are exchanged between institutions, so their serialized files are untrusted input. Without a filter,
 * reading such a file can run code from any serializable class on the classpath, before any cast is evaluated.
 * Use this for everything that is read from a file or from an index, rather than new ObjectInputStream.
 *
 * A class that is rejected here makes the read fail with an InvalidClassException and is logged as an error. If a
 * valid archive fails to load with such an error, the class it names needs to be added to ALLOWED_CLASSES.
 */
public final class SafeObjectInput {

    private static final Logger log = LogManager.getLogger(SafeObjectInput.class);

    /**
     * Patterns as in ObjectInputFilter.Config.createFilter. Arrays are checked by their element type and primitive
     * arrays are always accepted. Third party classes are named individually where possible, and java.net.URL is
     * left out on purpose, as reading one can trigger a DNS lookup.
     */
    static final String ALLOWED_CLASSES = String.join(";",
            "edu.stanford.muse.**",
            "edu.stanford.epadd.**",

            "java.lang.Object",
            "java.lang.Enum",
            "java.lang.Number",
            "java.lang.Boolean",
            "java.lang.Byte",
            "java.lang.Character",
            "java.lang.Double",
            "java.lang.Float",
            "java.lang.Integer",
            "java.lang.Long",
            "java.lang.Short",
            "java.lang.String",
            "java.lang.String$CaseInsensitiveComparator",
            "java.io.File",
            "java.math.BigDecimal",
            "java.math.BigInteger",
            "java.time.*",
            "java.util.*",
            "java.util.concurrent.*",
            "java.util.concurrent.atomic.*",
            "java.util.concurrent.locks.*",
            "java.util.regex.Pattern",

            "javax.mail.Address",
            "javax.mail.internet.InternetAddress",
            "javax.mail.internet.NewsAddress",
            "com.google.common.collect.*",
            // Archive.dupMessageInfo
            "groovy.lang.Tuple",
            "groovy.lang.Tuple2",

            "!*");

    /** The NER models and gazettes also store this map, which archives don't need. */
    static final String ALLOWED_MODEL_CLASSES = "org.apache.commons.collections4.map.CaseInsensitiveMap;" + ALLOWED_CLASSES;

    private static final ObjectInputFilter FILTER = createFilter(ALLOWED_CLASSES);

    private static final ObjectInputFilter MODEL_FILTER = createFilter(ALLOWED_MODEL_CLASSES);

    private SafeObjectInput() {
    }

    private static ObjectInputFilter createFilter(String allowedClasses) {
        ObjectInputFilter allowed = ObjectInputFilter.Config.createFilter(allowedClasses);
        return info -> {
            ObjectInputFilter.Status status = allowed.checkInput(info);
            if (status == ObjectInputFilter.Status.REJECTED)
                log.error("Refused to read class " + info.serialClass().getName() + " from a serialized file. The file may have been tampered with.");
            return status;
        };
    }

    public static ObjectInputStream open(InputStream in) throws IOException {
        return open(in, FILTER);
    }

    /** For the NER models and gazettes from the settings folder or the application's resources. */
    public static ObjectInputStream openModel(InputStream in) throws IOException {
        return open(in, MODEL_FILTER);
    }

    private static ObjectInputStream open(InputStream in, ObjectInputFilter filter) throws IOException {
        ObjectInputStream ois = new ObjectInputStream(in);
        ois.setObjectInputFilter(filter);
        return ois;
    }
}
