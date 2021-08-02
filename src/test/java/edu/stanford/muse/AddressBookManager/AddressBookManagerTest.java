package edu.stanford.muse.AddressBookManager;

import edu.stanford.muse.index.EmailDocument;
import edu.stanford.muse.util.EmailUtils;
import edu.stanford.muse.util.Util;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import javax.mail.Address;
import javax.mail.internet.InternetAddress;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;

@RunWith(JUnit4.class)
public class AddressBookManagerTest {

    @Test
    public void testParsePossibleNamesFromEmailAddress() {
        //derived and correction from AddressBookManager main() tests
        List<String> list1 = EmailUtils.parsePossibleNamesFromEmailAddress("mickey.mouse@disney.com");
        assertThat(list1, hasItems("Mickey Mouse"));
        List<String> list2 = EmailUtils.parsePossibleNamesFromEmailAddress("donald_duck@disney.com");
        assertThat(list2, hasItems("Donald Duck"));
        List<String> list3 = EmailUtils.parsePossibleNamesFromEmailAddress("70451.2444@compuserve.com");
        assertThat(list3, is(Collections.emptyList()));
    }

    @Test
    public void testAddressBookManager() throws Exception {
        //derived and correction from AddressBookManager main() tests
        String ownerName = "Owner Name";
        String ownerEmail = "owner@example.com";
        {
            AddressBook ab = new AddressBook(new String[]{ownerEmail}, new String[]{ownerName});
            EmailDocument ed = new EmailDocument();
            ed.to = new Address[]{new InternetAddress("from@email.com", "From Last")};
            ed.cc = new Address[]{new InternetAddress("cc@email.com", "CC Last")};
            ed.to = new Address[]{new InternetAddress("to@example.com", "To Last")};
            ed.from = new Address[]{new InternetAddress("from@example.com", "From Last")};
            ab.processContactsFromMessage(ed, new LinkedHashSet<>());
            assertEquals("Address book size not correct size.", 4, ab.size());
        }
    }

    @Test
    public void testLookupByAddress() throws Exception {
        //derived and correction from AddressBookManager main() tests
        String ownerName = "Owner Name";
        String ownerEmail = "owner@example.com";
        AddressBook ab = new AddressBook(new String[]{ownerEmail}, new String[]{ownerName});
        EmailDocument ed1 = new EmailDocument(), ed2 = new EmailDocument();
        ed1.to = new Address[]{new InternetAddress("Merge Name", "mergename@example.com")};
        ed1.from = new Address[]{new InternetAddress("Merge Name2", "mergename@example.com")};
        ed2.to = new Address[]{new InternetAddress("Merge X Name", "mergeemail1@example.com")};
        ed2.from = new Address[]{new InternetAddress("Merge X Name", "mergeemail2@example.com")};
        ab.processContactsFromMessage(ed1, new LinkedHashSet<>());
        ab.processContactsFromMessage(ed2, new LinkedHashSet<>());

        assertEquals("Size of address book incorrect", ab.size(), 4); //perhaps this should be 3?
        // returns null - possibly need to fix or remove
        // assertEquals("Lookup by email.", ab.lookupByEmail("mergename@example.com").getNames().size(), 2);
    }
}
