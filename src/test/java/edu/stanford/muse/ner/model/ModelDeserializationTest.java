package edu.stanford.muse.ner.model;

import edu.stanford.muse.Config;
import edu.stanford.muse.util.SafeObjectInput;
import org.apache.commons.collections4.map.CaseInsensitiveMap;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import untrusted.Payload;

import java.io.*;
import java.lang.reflect.Method;
import java.util.zip.GZIPOutputStream;

import static org.junit.Assert.*;

public class ModelDeserializationTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    private String originalSettingsDir;

    @Before
    public void useTemporarySettingsDir() {
        originalSettingsDir = Config.SETTINGS_DIR;
        Config.SETTINGS_DIR = tmp.getRoot().getPath();
        Payload.executed = false;
    }

    @After
    public void restoreSettingsDir() {
        Config.SETTINGS_DIR = originalSettingsDir;
    }

    private static byte[] serialize(Object o) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ObjectOutputStream out = new ObjectOutputStream(bytes)) {
            out.writeObject(o);
        }
        return bytes.toByteArray();
    }

    private void writeSerGz(String path, Object o) throws IOException {
        File file = new File(tmp.getRoot(), path);
        file.getParentFile().mkdirs();
        try (ObjectOutputStream out = new ObjectOutputStream(new GZIPOutputStream(new FileOutputStream(file)))) {
            out.writeObject(o);
        }
    }

    @Test
    public void modelsMayContainCaseInsensitiveMaps() throws Exception {
        CaseInsensitiveMap<String, String> map = new CaseInsensitiveMap<>();
        map.put("Key", "value");
        try (ObjectInputStream in = SafeObjectInput.openModel(new ByteArrayInputStream(serialize(map)))) {
            assertEquals("value", ((CaseInsensitiveMap<?, ?>) in.readObject()).get("KEY"));
        }
    }

    @Test(expected = InvalidClassException.class)
    public void archivesMayNotContainCaseInsensitiveMaps() throws Exception {
        try (ObjectInputStream in = SafeObjectInput.open(new ByteArrayInputStream(serialize(new CaseInsensitiveMap<String, String>())))) {
            in.readObject();
        }
    }

    @Test
    public void openModelRejectsOtherClasses() throws Exception {
        try (ObjectInputStream in = SafeObjectInput.openModel(new ByteArrayInputStream(serialize(new Payload())))) {
            in.readObject();
            fail("Should have been rejected");
        } catch (InvalidClassException expected) {
            assertFalse(Payload.executed);
        }
    }

    @Test
    public void loadModelDoesNotRunAPayload() throws Exception {
        writeSerGz("SeqModel.ser.gz", new Payload());
        assertNull(SequenceModel.loadModel("SeqModel.ser.gz"));
        assertFalse(Payload.executed);
    }

    @Test
    public void loadGazetteDoesNotRunAPayload() throws Exception {
        writeSerGz("rules" + File.separator + "gazettes.ser.gz", new Payload());
        for (Class<?> model : new Class<?>[]{SequenceModel.class, NBModel.class}) {
            Method loadGazette = model.getDeclaredMethod("loadGazette", String.class);
            loadGazette.setAccessible(true);
            assertNull(model.getSimpleName(), loadGazette.invoke(null, "rules"));
            assertFalse(model.getSimpleName(), Payload.executed);
        }
    }
}
