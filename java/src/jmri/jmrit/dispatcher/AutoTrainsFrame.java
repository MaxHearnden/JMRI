package jmri.jmrit.dispatcher;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JSlider;

import jmri.DccThrottle;
import jmri.Throttle;
import jmri.util.JmriJFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * AutoTrainsFrame provides a user interface to trains that are running
 * automatically under Dispatcher.
 * <p>
 * There is only one AutoTrains window. AutoTrains are added and deleted from
 * this window as they are added or terminated.
 * <p>
 * This file is part of JMRI.
 * <p>
 * JMRI is open source software; you can redistribute it and/or modify it under
 * the terms of version 2 of the GNU General Public License as published by the
 * Free Software Foundation. See the "COPYING" file for a copy of this license.
 * <p>
 * JMRI is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * @author Dave Duchamp Copyright (C) 2010
 */
public class AutoTrainsFrame extends jmri.util.JmriJFrame {

    public AutoTrainsFrame(DispatcherFrame disp) {
        super(false, true);
        _dispatcher = disp;
        initializeAutoTrainsWindow();
    }

    // instance variables
    private final DispatcherFrame _dispatcher;
    private final ArrayList<AutoActiveTrain> _autoTrainsList = new ArrayList<>();
    private final ArrayList<java.beans.PropertyChangeListener> _listeners = new ArrayList<>();
    //Keep track of throttle and listeners to update frame with their current state.
    private final ArrayList<jmri.Throttle> _throttles = new ArrayList<>();
    private final ArrayList<java.beans.PropertyChangeListener> _throttleListeners = new ArrayList<>();

    // accessor functions
    public ArrayList<AutoActiveTrain> getAutoTrainsList() {
        return _autoTrainsList;
    }

    public void addAutoActiveTrain(AutoActiveTrain aat) {
        if (aat != null) {
            _autoTrainsList.add(aat);
            java.beans.PropertyChangeListener throttleListener = this::handleThrottleChange;
            _throttleListeners.add(throttleListener);

            _throttles.add(null); //adds a place holder
            //set up the throttle prior to attaching the listener to the ActiveTrain
            setupThrottle(aat);

            ActiveTrain at = aat.getActiveTrain();
            java.beans.PropertyChangeListener listener;
            at.addPropertyChangeListener(listener = this::handleActiveTrainChange);
            _listeners.add(listener);

            displayAutoTrains();
        }
    }

    public void removeAutoActiveTrain(AutoActiveTrain aat) {
        for (int i = 0; i < _autoTrainsList.size(); i++) {
            if (_autoTrainsList.get(i) == aat) {
                removeThrottleListener(aat);
                _autoTrainsList.remove(i);
                ActiveTrain at = aat.getActiveTrain();
                at.removePropertyChangeListener(_listeners.get(i));
                _throttles.remove(i);
                _listeners.remove(i);
                _throttleListeners.remove(i);
                displayAutoTrains();
                return;
            }
        }
    }

    private void handleActiveTrainChange(java.beans.PropertyChangeEvent e) {
        if (e.getPropertyName().equals("mode")) {
            handleChangeOfMode(e);
        } else if (e.getPropertyName().equals("status")) {
            handleChangeOfStatus(e);
            handleChangeOfStatus(e);
        }
        displayAutoTrains();
    }

    private void handleChangeOfStatus(java.beans.PropertyChangeEvent e) {
        if ((int) e.getOldValue() == ActiveTrain.WORKING) {
            // put back listener
            addThrottleListener(((ActiveTrain) e.getSource()).getAutoActiveTrain());
        }
    }

    private synchronized void handleChangeOfMode(java.beans.PropertyChangeEvent e) {
        for (AutoActiveTrain aat : _autoTrainsList) {
            if (aat.getActiveTrain() == e.getSource()) {
                int newValue = (Integer) e.getNewValue();
                int oldValue = (Integer) e.getOldValue();
                if (newValue == ActiveTrain.DISPATCHED) {
                    removeThrottleListener(aat);
                } else if (oldValue == ActiveTrain.DISPATCHED) {
                    setupThrottle(aat);
                }
            }
        }
    }

    private void setupThrottle(AutoActiveTrain aat) {
        if (aat.getThrottle() != null) {
            int index = _autoTrainsList.indexOf(aat);
            if (_throttles.get(index) == null) {
                _throttles.add(index, aat.getThrottle());
                addThrottleListener(aat);
            }
        }
        else {
            log.debug("{}: No Throttle (yet)",aat.getActiveTrain().getTrainName());
        }
    }

    private void handleThrottleChange(java.beans.PropertyChangeEvent e) {
        if (!e.getPropertyName().equals(Throttle.SPEEDSETTING) && !e.getPropertyName().equals(Throttle.ISFORWARD)) {
            return; //ignore if not speed or direction
        }
        DccThrottle thr = (DccThrottle) e.getSource();
        int index = _throttles.indexOf(thr);
        if (index == -1) {
            jmri.Throttle waThrottle1 =  (Throttle) e.getSource();
            int DDCAddress =  waThrottle1.getLocoAddress().getNumber() ;
            log.trace("handleThrottleChange - using locoaddress [{}]", DDCAddress);
            for (jmri.Throttle waThrottle  : _throttles ) {
                if (waThrottle != null) {
                    if ( DDCAddress == waThrottle.getLocoAddress().getNumber()) {
                        index = _throttles.indexOf(waThrottle);
                        if (index == -1) {
                            log.warn("handleThrottleChange - cannot find throttle DCCAddress");
                            return;
                        }
                    }
                }
            }
            if (index == -1) {
                log.warn("handleThrottleChange - cannot find throttle index");
                return;
            }
        }
        JLabel status = _throttleStatus.get(index);
        if (!status.isVisible()) {
            return;
        }
        jmri.DccLocoAddress addy = (jmri.DccLocoAddress) _throttles.get(index).getLocoAddress();
        updateStatusLabel(status, jmri.InstanceManager.throttleManagerInstance().getThrottleInfo(addy, Throttle.SPEEDSETTING), jmri.InstanceManager.throttleManagerInstance().getThrottleInfo(addy, Throttle.ISFORWARD));
    }

    private void updateStatusLabel(JLabel status, Object speed, Object forward) {
        StringBuilder sb = new StringBuilder();
        int spd = Math.round((Float) speed * 100);
        sb.append("" + spd);
        sb.append("% ");
        if ((Boolean) forward) {
            sb.append("(fwd)");
        } else {
            sb.append("(rev)");
        }
        status.setText(sb.toString());
        autoTrainsFrame.pack();
    }

    private void addThrottleListener(AutoActiveTrain aat) {
        int index = _autoTrainsList.indexOf(aat);
        if (index == -1) {
            return;
        }
        if (_throttles.get(index) != null) {
            jmri.DccLocoAddress addy = (jmri.DccLocoAddress) _throttles.get(index).getLocoAddress();
            jmri.InstanceManager.throttleManagerInstance().attachListener(addy, _throttleListeners.get(index));
            JLabel status = _throttleStatus.get(index);
            updateStatusLabel(status, jmri.InstanceManager.throttleManagerInstance().getThrottleInfo(addy, Throttle.SPEEDSETTING), jmri.InstanceManager.throttleManagerInstance().getThrottleInfo(addy, Throttle.ISFORWARD));
        }
    }

    private void removeThrottleListener(AutoActiveTrain aat) {
        int index = _autoTrainsList.indexOf(aat);
        if (index == -1) {
            return;
        }
        if (_throttles.get(index) != null) {
            jmri.InstanceManager.throttleManagerInstance().removeListener(_throttles.get(index).getLocoAddress(), _throttleListeners.get(index));
        }
    }

    // variables for AutoTrains window
    protected JmriJFrame autoTrainsFrame = null;
    private Container contentPane = null;
    //This would be better refactored this all into a sub-class, rather than multiple arraylists.
    // note: the following array lists are synchronized with _autoTrainsList
    private final ArrayList<JPanel> _JPanels = new ArrayList<>();
    private final ArrayList<JLabel> _throttleStatus = new ArrayList<>();
    private final ArrayList<JLabel> _trainLabels = new ArrayList<>();
    private final ArrayList<JButton> _stopButtons = new ArrayList<>();
    private final ArrayList<JButton> _manualButtons = new ArrayList<>();
    private final ArrayList<JButton> _resumeAutoRunningButtons = new ArrayList<>();
    private final ArrayList<JRadioButton> _forwardButtons = new ArrayList<>();
    private final ArrayList<JRadioButton> _reverseButtons = new ArrayList<>();
    private final ArrayList<JSlider> _speedSliders = new ArrayList<>();

    private final ArrayList<JSeparator> _separators = new ArrayList<>();

    private void initializeAutoTrainsWindow() {
        autoTrainsFrame = this;
        autoTrainsFrame.setTitle(Bundle.getMessage("TitleAutoTrains"));
        JMenuBar menuBar = new JMenuBar();
        setJMenuBar(menuBar);
        autoTrainsFrame.addHelpMenu("package.jmri.jmrit.dispatcher.AutoTrains", true);
        contentPane = autoTrainsFrame.getContentPane();
        contentPane.setLayout(new BoxLayout(contentPane, BoxLayout.Y_AXIS));
        // set up 6 auto trains to size the panel
        for (int i = 0; i < 6; i++) {
            newTrainLine();
            if (i == 0) {
                _separators.get(i).setVisible(false);
            }
        }
        contentPane.add(new JSeparator());
        contentPane.add(new JSeparator());
        JPanel pB = new JPanel();
        pB.setLayout(new FlowLayout());
        JButton stopAllButton = new JButton(Bundle.getMessage("StopAll"));
        pB.add(stopAllButton);
        stopAllButton.addActionListener(this::stopAllPressed);
        stopAllButton.setToolTipText(Bundle.getMessage("StopAllButtonHint"));
        contentPane.add(pB);
        autoTrainsFrame.pack();
        placeWindow();
        displayAutoTrains();
        autoTrainsFrame.setVisible(true);
    }

    private void newSeparator() {
        JSeparator sep = new JSeparator();
        _separators.add(sep);
        contentPane.add(sep);
    }

    private void newTrainLine() {
        int i = _JPanels.size();
        final String s = "" + i;
        newSeparator();
        JPanel px = new JPanel();
        px.setLayout(new FlowLayout());
        _JPanels.add(px);
        JLabel tLabel = new JLabel("      ");
        px.add(tLabel);
        px.add(tLabel);
        _trainLabels.add(tLabel);
        JButton tStop = new JButton(Bundle.getMessage("ResumeButton"));
        px.add(tStop);
        _stopButtons.add(tStop);
        tStop.addActionListener(e -> stopResume(s));
        JButton tManual = new JButton(Bundle.getMessage("ToManualButton"));
        px.add(tManual);
        _manualButtons.add(tManual);
        tManual.addActionListener(e -> manualAuto(s));
        JButton tResumeAuto = new JButton(Bundle.getMessage("ResumeAutoButton"));
        px.add(tResumeAuto);
        _resumeAutoRunningButtons.add(tResumeAuto);
        tResumeAuto.addActionListener(e -> resumeAutoOperation(s));
        tResumeAuto.setVisible(false);
        tResumeAuto.setToolTipText(Bundle.getMessage("ResumeAutoButtonHint"));
        ButtonGroup directionGroup = new ButtonGroup();
        JRadioButton fBut = new JRadioButton(Bundle.getMessage("ForwardRadio"));
        px.add(fBut);
        _forwardButtons.add(fBut);
        fBut.addActionListener(e -> directionButton(s));
        directionGroup.add(fBut);
        JRadioButton rBut = new JRadioButton(Bundle.getMessage("ReverseRadio"));
        px.add(rBut);
        _reverseButtons.add(rBut);
        rBut.addActionListener(e -> directionButton(s));
        directionGroup.add(rBut);
        JSlider sSlider = new JSlider(0, 100, 0);
        px.add(sSlider);
        _speedSliders.add(sSlider);
        sSlider.addChangeListener(e -> {
            int val = ((JSlider) (e.getSource())).getValue();
            sliderChanged(s, val);
        });

        JLabel _throttle = new JLabel();
        _throttle.setPreferredSize(new Dimension(100,20)); // prevent JFrame to resize on each % change
        _throttle.setText("Speed Unknown");
        _throttleStatus.add(_throttle);
        px.add(_throttle);
        contentPane.add(px);
    }

    private void placeWindow() {
        // get size and placement of Dispatcher Window, screen size, and window size
        Point dispPt = new Point(0, 0);
        Dimension dispDim = new Dimension(0, 0);

        if (_dispatcher.isShowing()) {
            dispPt = _dispatcher.getLocationOnScreen();
            dispDim = _dispatcher.getSize();
        }
        Dimension screenDim = Toolkit.getDefaultToolkit().getScreenSize();
        int screenHeight = screenDim.height - 120;
        int screenWidth = screenDim.width - 20;
        Dimension dim = getSize();
        int width = dim.width;
        int height = dim.height;
        // place AutoTrains window to the right of Dispatcher window, if it will fit.
        int upperLeftX = dispPt.x + dispDim.width;
        int upperLeftY = 0;
        if ((upperLeftX + width) > screenWidth) {
            // won't fit, place it below Dispatcher window, if it will fit.
            upperLeftX = 0;
            upperLeftY = dispPt.y + dispDim.height;
            if ((upperLeftY + height) > screenHeight) {
                // if all else fails, place it at the upper left of the screen, and let the user adjust placement
                upperLeftY = 0;
            }
        }
        setLocation(upperLeftX, upperLeftY);
    }

    public void stopResume(String s) {
        int index = getTrainIndex(s);
        if (index >= 0) {
            AutoActiveTrain aat = _autoTrainsList.get(index);
            if (aat.getAutoEngineer() != null) {
                ActiveTrain at = aat.getActiveTrain();
                if (at.getStatus() == ActiveTrain.STOPPED) {
                    // resume
                    aat.setEngineDirection();
                    aat.getAutoEngineer().setHalt(false);
                    aat.restoreSavedSpeedAndDirection();
                    at.setStatus(aat.getSavedStatus());
                    if ((at.getStatus() == ActiveTrain.RUNNING)
                            || (at.getStatus() == ActiveTrain.WAITING)) {
                        aat.setSpeedBySignal();
                    }
                } else if (at.getStatus() == ActiveTrain.DONE) {
                    // restart
                    at.allocateAFresh();
                    at.restart();
                } else {
                    // stop
                    aat.getAutoEngineer().setHalt(true);
                    aat.saveSpeedAndDirection();
                    aat.setSavedStatus(at.getStatus());
                    at.setStatus(ActiveTrain.STOPPED);
                    if (at.getMode() == ActiveTrain.MANUAL) {
                        _speedSliders.get(index).setValue(0);
                    }
                }
            } else {
                log.error("unexpected null autoEngineer");
            }
        }
        displayAutoTrains();
    }

    public void manualAuto(String s) {
        int index = getTrainIndex(s);
        if (index >= 0) {
            AutoActiveTrain aat = _autoTrainsList.get(index);
            ActiveTrain at = aat.getActiveTrain();
            // if train is AUTOMATIC mode, change it to MANUAL
            if (at.getMode() == ActiveTrain.AUTOMATIC) {
                at.setMode(ActiveTrain.MANUAL);
                if (aat.getAutoEngineer() != null) {
                    aat.saveSpeedAndDirection();
                    aat.getAutoEngineer().setHalt(true);
                    aat.setTargetSpeed(0.0f);
                    aat.waitUntilStopped();
                    aat.getAutoEngineer().setHalt(false);

                }
            } else if (at.getMode() == ActiveTrain.MANUAL) {
                at.setMode(ActiveTrain.AUTOMATIC);
                aat.restoreSavedSpeedAndDirection();
                if ((at.getStatus() == ActiveTrain.RUNNING)
                        || (at.getStatus() == ActiveTrain.WAITING)) {
                    if (aat.getCurrentSignal() != null) {
                    aat.setSpeedBySignal();
                    }
                }
            }
        }
        displayAutoTrains();
    }

    public void resumeAutoOperation(String s) {
        int index = getTrainIndex(s);
        if (index >= 0) {
            AutoActiveTrain aat = _autoTrainsList.get(index);
            aat.resumeAutomaticRunning();
        }
        displayAutoTrains();
    }

    public void directionButton(String s) {
        int index = getTrainIndex(s);
        if (index >= 0) {
            AutoActiveTrain aat = _autoTrainsList.get(index);
            ActiveTrain at = aat.getActiveTrain();
            if (at.getMode() == ActiveTrain.MANUAL) {
                aat.setForward(_forwardButtons.get(index).isSelected());
            } else {
                log.warn("unexpected direction button change on line {}", s);
            }
        }
    }

    public void sliderChanged(String s, int value) {
        int index = getTrainIndex(s);
        if (index >= 0) {
            AutoActiveTrain aat = _autoTrainsList.get(index);
            ActiveTrain at = aat.getActiveTrain();
            if (at.getMode() == ActiveTrain.MANUAL) {
                float speedValue = value;
                speedValue = speedValue * 0.01f;
                aat.getAutoEngineer().setSpeedImmediate(speedValue);
            } else {
                log.warn("unexpected slider change on line {}", s);
            }
        }
    }

    private int getTrainIndex(String s) {
        int index = -1;
        try {
            index = Integer.parseInt(s);
        } catch (Exception e) {
            log.warn("exception when parsing index from AutoTrains window - {}", s);
        }
        if ((index >= 0) && (index < _autoTrainsList.size())) {
            return index;
        }
        log.error("bad train index in auto trains table {}", index);
        return (-1);
    }

    public void stopAllPressed(ActionEvent e) {
        for (AutoActiveTrain aat : _autoTrainsList) {
            ActiveTrain at = aat.getActiveTrain();
            if ((at.getStatus() != ActiveTrain.STOPPED) && (aat.getAutoEngineer() != null)) {
                aat.getAutoEngineer().setHalt(true);
                aat.saveSpeedAndDirection();
                aat.setSavedStatus(at.getStatus());
                at.setStatus(ActiveTrain.STOPPED);
            }
        }
        displayAutoTrains();
    }

    protected void displayAutoTrains() {
        // set up AutoTrains to display
        while (_autoTrainsList.size() > _JPanels.size()) {
            newTrainLine();
        }
        for (int i = 0; i < _autoTrainsList.size(); i++) {
            AutoActiveTrain aat = _autoTrainsList.get(i);
            if (aat != null) {
                if (i > 0) {
                    JSeparator sep = _separators.get(i);
                    sep.setVisible(true);
                }
                JPanel panel = _JPanels.get(i);
                panel.setVisible(true);
                ActiveTrain at = aat.getActiveTrain();
                JLabel tName = _trainLabels.get(i);
                if (_throttles.get(i) != null) {
                    updateStatusLabel(_throttleStatus.get(i),_throttles.get(i).getSpeedSetting(),_throttles.get(i).getIsForward());
                } else {
                    updateStatusLabel(_throttleStatus.get(i), 0.0f, true);
                }
                tName.setText(at.getTrainName());
                JButton stopButton = _stopButtons.get(i);
                if (at.getStatus() == ActiveTrain.STOPPED) {
                    stopButton.setText(Bundle.getMessage("ResumeButton"));
                    stopButton.setToolTipText(Bundle.getMessage("ResumeButtonHint"));
                    _resumeAutoRunningButtons.get(i).setVisible(false);
                } else if (at.getStatus() == ActiveTrain.DONE) {
                    stopButton.setText(Bundle.getMessage("RestartButton"));
                    stopButton.setToolTipText(Bundle.getMessage("RestartButtonHint"));
                    _resumeAutoRunningButtons.get(i).setVisible(false);
                } else if (at.getStatus() == ActiveTrain.WORKING) {
                    stopButton.setVisible(false);
                } else {
                    stopButton.setText(Bundle.getMessage("StopButton"));
                    stopButton.setToolTipText(Bundle.getMessage("StopButtonHint"));
                    stopButton.setVisible(true);
                }
                JButton manualButton = _manualButtons.get(i);
                if (at.getMode() == ActiveTrain.AUTOMATIC) {
                    manualButton.setText(Bundle.getMessage("ToManualButton"));
                    manualButton.setToolTipText(Bundle.getMessage("ToManualButtonHint"));
                    manualButton.setVisible(true);
                    _resumeAutoRunningButtons.get(i).setVisible(false);
                    _forwardButtons.get(i).setVisible(false);
                    _reverseButtons.get(i).setVisible(false);
                    _speedSliders.get(i).setVisible(false);
                    _throttleStatus.get(i).setVisible(true);
                } else if (at.getMode() == ActiveTrain.DISPATCHED) {
                    manualButton.setVisible(false);
                    _resumeAutoRunningButtons.get(i).setVisible(true);
                    _forwardButtons.get(i).setVisible(false);
                    _reverseButtons.get(i).setVisible(false);
                    _speedSliders.get(i).setVisible(false);
                    _throttleStatus.get(i).setVisible(false);
                } else {
                    manualButton.setText(Bundle.getMessage("ToAutoButton"));
                    manualButton.setToolTipText(Bundle.getMessage("ToAutoButtonHint"));
                    _forwardButtons.get(i).setVisible(true);
                    _reverseButtons.get(i).setVisible(true);
                    _speedSliders.get(i).setVisible(true);
                    _throttleStatus.get(i).setVisible(false);
                    _forwardButtons.get(i).setSelected(aat.getForward());
                    _reverseButtons.get(i).setSelected(!aat.getForward());
                    int speedValue = (int) (aat.getTargetSpeed() * 100.0f);
                    _speedSliders.get(i).setValue(speedValue);
                }
            }
        }
        // clear unused item rows, if needed
        for (int j = _autoTrainsList.size(); j < _JPanels.size(); j++) {
            JPanel panel = _JPanels.get(j);
            panel.setVisible(false);
            JSeparator sep = _separators.get(j);
            sep.setVisible(false);
        }
        autoTrainsFrame.pack();
        autoTrainsFrame.setAutoRequestFocus(false);
        autoTrainsFrame.setVisible(true);
    }

    private final static Logger log = LoggerFactory.getLogger(AutoTrainsFrame.class);

}


