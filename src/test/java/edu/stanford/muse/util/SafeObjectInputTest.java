package edu.stanford.muse.util;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.Multimap;
import edu.stanford.muse.epaddpremis.EpaddPremis;
import groovy.lang.Tuple2;
import org.apache.commons.collections4.functors.ConstantTransformer;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import untrusted.Payload;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.io.*;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.Assert.*;

public class SafeObjectInputTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    @Before
    public void reset() {
        Payload.executed = false;
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        return bytes.toByteArray();
    }

    private static Object read(byte[] bytes) throws IOException, ClassNotFoundException {
        try (ObjectInputStream in = SafeObjectInput.open(new ByteArrayInputStream(bytes))) {
            return in.readObject();
        }
    }

    private static void assertRoundTrips(Object o) throws Exception {
        assertEquals(o, read(serialize(o)));
    }

    private void assertRejectedWithoutRunning(Object o) throws Exception {
        try {
            read(serialize(o));
            fail("Should have been rejected: " + o.getClass());
        } catch (InvalidClassException expected) {
            assertTrue(expected.getMessage(), expected.getMessage().contains("REJECTED"));
        }
        assertFalse("The payload ran", Payload.executed);
    }

    @Test
    public void thePayloadRunsWithoutTheFilter() throws Exception {
        try (ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(serialize(new Payload())))) {
            in.readObject();
        }
        assertTrue("This test class would prove nothing if the payload did not run unfiltered", Payload.executed);
    }

    @Test
    public void rejectsAClassThatIsNotAllowed() throws Exception {
        assertRejectedWithoutRunning(new Payload());
    }

    @Test
    public void rejectsAClassInsideACollection() throws Exception {
        Map<String, Object> map = new HashMap<>();
        map.put("key", new ArrayList<>(Collections.singletonList(new Payload())));
        assertRejectedWithoutRunning(map);
    }

    @Test
    public void rejectsAnArrayOfAClass() throws Exception {
        assertRejectedWithoutRunning(new Payload[]{new Payload()});
    }

    @Test
    public void rejectsAProxy() throws Exception {
        Object proxy = Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Comparable.class, Serializable.class}, new Payload());
        assertRejectedWithoutRunning(proxy);
    }

    @Test
    public void rejectsCommonsCollectionsClasses() throws Exception {
        assertRejectedWithoutRunning(new ConstantTransformer<>("x"));
    }

    @Test
    public void rejectsAUrl() throws Exception {
        assertRejectedWithoutRunning(new URL("http://example.invalid/"));
    }

    @Test
    public void readsTheJdkTypesArchivesUse() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("list", new ArrayList<>(Arrays.asList(1, 2L, 1.5f, 2.5d, true, (short) 3, (byte) 4, 'c')));
        map.put("linkedList", new LinkedList<>(Collections.singletonList("a")));
        map.put("sets", new LinkedHashSet<>(new HashSet<>(Collections.singletonList("a"))));
        map.put("date", new Date(0));
        map.put("file", new File("x"));
        map.put("unmodifiable", Collections.unmodifiableSet(new HashSet<>(Collections.singletonList("a"))));
        map.put("concurrent", new ConcurrentHashMap<>(Collections.singletonMap("a", "b")));
        TreeMap<String, String> caseInsensitive = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        caseInsensitive.put("a", "b");
        map.put("caseInsensitive", caseInsensitive);
        assertRoundTrips(map);
    }

    @Test
    public void readsArrays() throws Exception {
        assertArrayEquals(new byte[]{1, 2}, (byte[]) read(serialize(new byte[]{1, 2})));
        assertArrayEquals(new Object[]{"a", 1}, (Object[]) read(serialize(new Object[]{"a", 1})));
    }

    @Test
    public void readsTheThirdPartyTypesArchivesUse() throws Exception {
        assertArrayEquals(new Address[]{new InternetAddress("a@example.org", "A")}, (Address[]) read(serialize(new Address[]{new InternetAddress("a@example.org", "A")})));
        assertArrayEquals(new InternetAddress[]{new InternetAddress("a@example.org")}, (InternetAddress[]) read(serialize(new InternetAddress[]{new InternetAddress("a@example.org")})));

        Multimap<String, Tuple2<String, String>> linkedHash = LinkedHashMultimap.create();
        linkedHash.put("a", new Tuple2<>("b", "c"));
        assertEquals(linkedHash, read(serialize(linkedHash)));

        Multimap<String, String> linkedList = LinkedListMultimap.create();
        linkedList.put("a", "b");
        assertEquals(linkedList, read(serialize(linkedList)));
    }

    @Test
    public void readsEpaddClasses() throws Exception {
        assertRoundTrips(new Pair<>("a", "b"));
    }

    @Test
    public void readObjectFromFileRejectsTheClass() throws Exception {
        File file = tmp.newFile("authorities.ser");
        Util.writeObjectToFile(file.getPath(), new Payload());
        try {
            Util.readObjectFromFile(file.getPath());
            fail("Should have been rejected");
        } catch (InvalidClassException expected) {
            assertFalse(Payload.executed);
        }
    }

    /** A crafted premisobject.ser is read on every archive load, even if the rest of the archive is harmless. */
    @Test
    public void readingPremisDoesNotRunThePayload() throws Exception {
        File baseDir = tmp.newFolder("archive");
        File data = new File(baseDir, "data");
        assertTrue(data.mkdirs());
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(new File(data, "premisobject.ser")))) {
            out.writeObject(new Payload());
        }
        try {
            EpaddPremis.readPremisObject(null, baseDir.getPath(), "data");
        } catch (RuntimeException e) {
            // creating the replacement without an archive may fail, which doesn't matter here
        }
        assertFalse(Payload.executed);
    }
}
