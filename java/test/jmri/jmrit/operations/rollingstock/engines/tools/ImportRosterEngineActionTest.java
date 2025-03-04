package jmri.jmrit.operations.rollingstock.engines.tools;

import java.awt.GraphicsEnvironment;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ResourceBundle;

import org.junit.Assert;
import org.junit.Assume;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import jmri.InstanceManager;
import jmri.jmrit.operations.OperationsTestCase;
import jmri.jmrit.operations.rollingstock.engines.Engine;
import jmri.jmrit.operations.rollingstock.engines.EngineManager;
import jmri.jmrit.roster.Roster;
import jmri.jmrit.roster.RosterEntry;
import jmri.util.FileUtil;
import jmri.util.JUnitUtil;
import jmri.util.swing.JemmyUtil;

/**
 *
 * @author Paul Bender Copyright (C) 2017
 */
@Timeout(20)
public class ImportRosterEngineActionTest extends OperationsTestCase {
    
    ResourceBundle rb = ResourceBundle.getBundle("jmri.jmrit.roster.JmritRosterBundle");

    @Test
    public void testCTor() {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        ImportRosterEngineAction t = new ImportRosterEngineAction();
        Assert.assertNotNull("exists", t);
    }

    @Test
    public void testFailedImport() {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());
        ImportRosterEngineAction importRosterAction = new ImportRosterEngineAction();
        Assert.assertNotNull("exists", importRosterAction);
        importRosterAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));

        Thread run = JUnitUtil.getThreadByName("Import Roster Engines");

        jmri.util.JUnitUtil.waitFor(() -> {
            return run.getState().equals(Thread.State.WAITING);
        }, "wait for dialog");
        
        JemmyUtil.pressDialogButton(rb.getString("SelectRosterGroup"), Bundle.getMessage("ButtonOK"));
        JemmyUtil.pressDialogButton(Bundle.getMessage("ImportFailed"), Bundle.getMessage("ButtonOK"));

        if (run != null) {
            try {
                run.join();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
    }

    @Test
    public void testImport(@TempDir File rosterDir) throws IOException, FileNotFoundException {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());

        // copied from "RosterTest"
        // store files in random temp directory
        FileUtil.createDirectory(rosterDir);

        File f = new File(rosterDir, "rosterTest.xml");
        // File should never be there is TemporaryFolder working
        if (f.exists()) {
            Assert.fail("rosterTest.xml in " + rosterDir + " already present: " + f);
        }

        // create a roster with known contents
        Roster r = Roster.getDefault();
        r.setRosterLocation(rosterDir.getAbsolutePath());
        r.setRosterIndexFileName("rosterTest.xml");

        RosterEntry e1 = new RosterEntry("file name Bob");
        e1.setId("Bob");
        e1.setDccAddress("123");
        e1.setRoadNumber("123");
        e1.setRoadName("SPABCDEFGHIJKLM"); // string length exceeds Control.max_len_string_attibute
        e1.setModel("NewModelABCDEFGH"); // string length exceeds Control.max_len_string_attibute
        e1.setOwner("OwnerNameABCD"); // string length exceeds Control.max_len_string_attibute
        e1.ensureFilenameExists();
        e1.putAttribute("key a", "value a");
        e1.putAttribute("key b", "value b");
        r.addEntry(e1);

        ImportRosterEngineAction importRosterAction = new ImportRosterEngineAction();
        Assert.assertNotNull("exists", importRosterAction);
        importRosterAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));

        Thread run = JUnitUtil.getThreadByName("Import Roster Engines");

        jmri.util.JUnitUtil.waitFor(() -> {
            return run.getState().equals(Thread.State.WAITING);
        }, "wait for dialog");
        
        JemmyUtil.pressDialogButton(rb.getString("SelectRosterGroup"), Bundle.getMessage("ButtonOK"));
        JemmyUtil.pressDialogButton(Bundle.getMessage("SuccessfulImport"), Bundle.getMessage("ButtonOK"));

        if (run != null) {
            try {
                run.join();
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        Engine e = InstanceManager.getDefault(EngineManager.class).getByRoadAndNumber("SPABCDEFGHIJ", "123");
        Assert.assertNotNull(e);

        Assert.assertEquals("model", "NewModelABCD", e.getModel());
        Assert.assertEquals("road", "SPABCDEFGHIJ", e.getRoadName());
        Assert.assertEquals("owner", "OwnerNameABC", e.getOwner());
    }

    @Test
    public void testImportNoModel(@TempDir File rosterDir) throws IOException, FileNotFoundException {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());

        // copied from "RosterTest"
        // store files in random temp directory
        FileUtil.createDirectory(rosterDir);

        File f = new File(rosterDir, "rosterTest.xml");
        // File should never be there is TemporaryFolder working
        if (f.exists()) {
            Assert.fail("rosterTest.xml in " + rosterDir + " already present: " + f);
        }

        // create a roster with known contents
        Roster r = Roster.getDefault();
        r.setRosterLocation(rosterDir.getAbsolutePath());
        r.setRosterIndexFileName("rosterTest.xml");

        RosterEntry e1 = new RosterEntry("file name Bob");
        e1.setId("Bob");
        e1.setDccAddress("123");
        e1.setRoadNumber("123");
        e1.setRoadName("SP");
        e1.ensureFilenameExists();
        e1.putAttribute("key a", "value a");
        e1.putAttribute("key b", "value b");
        r.addEntry(e1);

        ImportRosterEngineAction importRosterAction = new ImportRosterEngineAction();
        Assert.assertNotNull("exists", importRosterAction);
        importRosterAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));

        Thread run = JUnitUtil.getThreadByName("Import Roster Engines");

        jmri.util.JUnitUtil.waitFor(() -> {
            return run.getState().equals(Thread.State.WAITING);
        }, "wait for dialog");
        
        JemmyUtil.pressDialogButton(rb.getString("SelectRosterGroup"), Bundle.getMessage("ButtonOK"));
        JemmyUtil.pressDialogButton(Bundle.getMessage("SuccessfulImport"), Bundle.getMessage("ButtonOK"));

        if (run != null) {
            try {
                run.join();
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        Engine e = InstanceManager.getDefault(EngineManager.class).getByRoadAndNumber("SP", "123");
        Assert.assertNotNull(e);

        jmri.util.JUnitAppender.assertWarnMessage("Roster Id: Bob hasn't been assigned a model name");
    }

    @Test
    public void testImportNoRoadNumber(@TempDir File rosterDir) throws IOException, FileNotFoundException {
        Assume.assumeFalse(GraphicsEnvironment.isHeadless());

        // copied from "RosterTest"
        // store files in random temp directory
        FileUtil.createDirectory(rosterDir);

        File f = new File(rosterDir, "rosterTest.xml");
        // File should never be there is TemporaryFolder working
        if (f.exists()) {
            Assert.fail("rosterTest.xml in " + rosterDir + " already present: " + f);
        }

        // create a roster with known contents
        Roster r = Roster.getDefault();
        r.setRosterLocation(rosterDir.getAbsolutePath());
        r.setRosterIndexFileName("rosterTest.xml");

        RosterEntry e1 = new RosterEntry("file name Bob");
        e1.setId("Bob");
        e1.setDccAddress("123");
        // no road number
        e1.setRoadName("SP");
        e1.ensureFilenameExists();
        e1.putAttribute("key a", "value a");
        e1.putAttribute("key b", "value b");
        r.addEntry(e1);

        ImportRosterEngineAction importRosterAction = new ImportRosterEngineAction();
        Assert.assertNotNull("exists", importRosterAction);
        importRosterAction.actionPerformed(new ActionEvent(this, ActionEvent.ACTION_PERFORMED, null));

        Thread run = JUnitUtil.getThreadByName("Import Roster Engines");

        jmri.util.JUnitUtil.waitFor(() -> {
            return run.getState().equals(Thread.State.WAITING);
        }, "wait for dialog");
        
        JemmyUtil.pressDialogButton(rb.getString("SelectRosterGroup"), Bundle.getMessage("ButtonOK"));
        JemmyUtil.pressDialogButton(Bundle.getMessage("ImportFailed"), Bundle.getMessage("ButtonOK"));

        if (run != null) {
            try {
                run.join();
            } catch (InterruptedException e) {
                // do nothing
            }
        }

        jmri.util.JUnitAppender.assertErrorMessage("Roster Id: Bob doesn't have a road name and road number");
    }

    // private final static Logger log =
    // LoggerFactory.getLogger(ImportRosterEngineActionTest.class);
}
