package jmri.jmrix.dccpp;

import jmri.SpeedStepMode;
import jmri.util.JUnitUtil;

import org.junit.Assert;
import org.junit.jupiter.api.*;

/**
 * Test for the jmri.jmrix.dccpp.DCCppThrottle class
 *
 * @author Paul Bender
 * @author Mark Underwood
 * @author Egbert Broerse 2021
 */
public class DCCppThrottleTest extends jmri.jmrix.AbstractThrottleTest {

    @Test
    public void testCtor() {
        DCCppThrottle t = new DCCppThrottle(memo, tc);
        Assert.assertNotNull(t);
    }

    /**
     * Test of getSpeedIncrement method, of class AbstractThrottle.
     */
    @Override
    @Test
    public void testGetSpeedIncrement() {
        float expResult = 1.0F/126.0F;
        float result = instance.getSpeedIncrement();
        Assert.assertEquals(expResult, result, 0.0);
    }

    /**
     * Test of getSpeedStepMode method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testGetSpeedStepMode() {
        SpeedStepMode expResult = SpeedStepMode.NMRA_DCC_128;
        SpeedStepMode result = instance.getSpeedStepMode();
        Assert.assertEquals(expResult, result);
    }

    /**
     * 
     */
    @Test
    @Override
    public void testGetIsForward() {
        Assert.assertTrue(instance.getIsForward()); //new throttle defaults to Forward
        instance.setIsForward(true);
        Assert.assertTrue(instance.getIsForward());
        instance.setIsForward(false);
        Assert.assertFalse(instance.getIsForward());
    }

    /**
     * Test of setF0 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF0() {
        boolean f0 = false;
        instance.setF0(f0);
    }

    /**
     * Test of setF1 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF1() {
        boolean f1 = false;
        instance.setF1(f1);
    }

    /**
     * Test of setF2 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF2() {
        boolean f2 = false;
        instance.setF2(f2);
    }

    /**
     * Test of setF3 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF3() {
        boolean f3 = false;
        instance.setF3(f3);
    }

    /**
     * Test of setF4 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF4() {
        boolean f4 = false;
        instance.setF4(f4);
    }

    /**
     * Test of setF5 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF5() {
        boolean f5 = false;
        instance.setF5(f5);
    }

    /**
     * Test of setF6 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF6() {
        boolean f6 = false;
        instance.setF6(f6);
    }

    /**
     * Test of setF7 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF7() {
        boolean f7 = false;
        instance.setF7(f7);
    }

    /**
     * Test of setF8 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF8() {
        boolean f8 = false;
        instance.setF8(f8);
    }

    /**
     * Test of setF9 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF9() {
        boolean f9 = false;
        instance.setF9(f9);
    }

    /**
     * Test of setF10 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF10() {
        boolean f10 = false;
        instance.setF10(f10);
    }

    /**
     * Test of setF11 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF11() {
        boolean f11 = false;
        instance.setF11(f11);
    }

    /**
     * Test of setF12 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF12() {
        boolean f12 = false;
        instance.setF12(f12);
    }

    /**
     * Test of setF13 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF13() {
        boolean f13 = false;
        instance.setF13(f13);
    }

    /**
     * Test of setF14 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF14() {
        boolean f14 = false;
        instance.setF14(f14);
    }

    /**
     * Test of setF15 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF15() {
        boolean f15 = false;
        instance.setF15(f15);
    }

    /**
     * Test of setF16 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF16() {
        boolean f16 = false;
        instance.setF16(f16);
    }

    /**
     * Test of setF17 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF17() {
        boolean f17 = false;
        instance.setF17(f17);
    }

    /**
     * Test of setF18 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF18() {
        boolean f18 = false;
        instance.setF18(f18);
    }

    /**
     * Test of setF19 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF19() {
        boolean f19 = false;
        instance.setF19(f19);
    }

    /**
     * Test of setF20 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF20() {
        boolean f20 = false;
        instance.setF20(f20);
    }

    /**
     * Test of setF21 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF21() {
        boolean f21 = false;
        instance.setF21(f21);
    }

    /**
     * Test of setF22 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF22() {
        boolean f22 = false;
        instance.setF22(f22);
    }

    /**
     * Test of setF23 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF23() {
        boolean f23 = false;
        instance.setF23(f23);
    }

    /**
     * Test of setF24 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF24() {
        boolean f24 = false;
        instance.setF24(f24);
    }

    /**
     * Test of setF25 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF25() {
        boolean f25 = false;
        instance.setF25(f25);
    }

    /**
     * Test of setF26 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF26() {
        boolean f26 = false;
        instance.setF26(f26);
    }

    /**
     * Test of setF27 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF27() {
        boolean f27 = false;
        instance.setF27(f27);
    }

    /**
     * Test of setF28 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSetF28() {
        boolean f28 = false;
        instance.setF28(f28);
    }

    /**
     * Test of sendFunctionGroup1 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSendFunctionGroup1() {
    }

    /**
     * Test of sendFunctionGroup2 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSendFunctionGroup2() {
    }

    /**
     * Test of sendFunctionGroup3 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSendFunctionGroup3() {
    }

    /**
     * Test of sendFunctionGroup4 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSendFunctionGroup4() {
    }

    /**
     * Test of sendFunctionGroup5 method, of class AbstractThrottle.
     */
    @Test
    @Override
    public void testSendFunctionGroup5() {
    }

    @Test
    public void testThrottleMonitors() {
        DCCppMessage msg = new DCCppMessage("t 1 2 3 1");
        Assert.assertEquals("Monitor string", "Throttle Cmd: Register: 1, Address: 2, Speed: 3, Direction: Forward", msg.toMonitorString());
        msg = new DCCppMessage("t 2 3 1");
        Assert.assertEquals("Monitor string", "ThrottleV3 Cmd: Address: 2, Speed: 3, Direction: Forward", msg.toMonitorString());
    }

    @Test
    public void testLocoStateReplies() {
        DCCppReply l = DCCppReply.parseDCCppReply("l 1 2 123 789"); //reverse speed 122
        Assert.assertEquals("Monitor string", "Loco State: Cab:1 Slot:2 Dir:Reverse Speed:122 F0-28:10101000110000000000000000000", l.toMonitorString());
        Assert.assertFalse("reverse is false", l.getDirectionBool());
        Assert.assertEquals("reverse is 0", 0, l.getDirectionInt());
        Assert.assertFalse("not eStop", l.isEStop());
        l = DCCppReply.parseDCCppReply("l 99 0 246 32768"); //forward speed 117
        Assert.assertEquals("Monitor string", "Loco State: Cab:99 Slot:0 Dir:Forward Speed:117 F0-28:00000000000000010000000000000", l.toMonitorString());
        Assert.assertTrue("forward is true", l.getDirectionBool());
        Assert.assertEquals("forward is 1", 1, l.getDirectionInt());
        Assert.assertFalse("not eStop", l.isEStop());
        l = DCCppReply.parseDCCppReply("l 88 3 1 0"); //eStop (reverse)
        Assert.assertEquals("Monitor string", "Loco State: Cab:88 Slot:3 Dir:Reverse Speed:-1 F0-28:00000000000000000000000000000", l.toMonitorString());
        Assert.assertTrue("eStop", l.isEStop());
        l = DCCppReply.parseDCCppReply("l 88 3 129 0"); //eStop (forward)
        Assert.assertEquals("Monitor string", "Loco State: Cab:88 Slot:3 Dir:Forward Speed:-1 F0-28:00000000000000000000000000000", l.toMonitorString());
        Assert.assertTrue("eStop", l.isEStop());
    }

    // Test the constructor with an address specified.
    @Test
    public void testCtorWithArg() throws Exception {
        Assert.assertNotNull(instance);
    }

    // Test the initialization sequence.
    @Test
    public void testInitSequence() throws Exception {
        Assert.assertEquals("Throttle in THROTTLEIDLE state", DCCppThrottle.THROTTLEIDLE, ((DCCppThrottle)instance).requestState);
    }

    private DCCppInterfaceScaffold tc;
    private DCCppSystemConnectionMemo memo;
    private DCCppThrottleManager tm;

    @Override
    @BeforeEach
    public void setUp() throws Exception {
        JUnitUtil.setUp();
        tc = new DCCppInterfaceScaffold(new DCCppCommandStation());
        memo = new DCCppSystemConnectionMemo(tc);
        tm = new DCCppThrottleManager(memo);
        jmri.InstanceManager.setDefault(jmri.ThrottleManager.class, tm);
        instance = new DCCppThrottle(memo, new jmri.DccLocoAddress(3, false), tc);
    }

    @Override
    @AfterEach
    public void tearDown() throws Exception {
        // no need to dispose of instance
        if (tm != null) {
            tm.dispose();
        }
        memo.dispose();
        memo = null;
        tc.terminateThreads();
        tc = null;
        JUnitUtil.tearDown();
    }

}
