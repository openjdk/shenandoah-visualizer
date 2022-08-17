/*
 * ====
 *     Copyright (c) 2021, Amazon.com, Inc. All rights reserved.
 *     DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *     This code is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License version 2 only, as
 *     published by the Free Software Foundation.  Oracle designates this
 *     particular file as subject to the "Classpath" exception as provided
 *     by Oracle in the LICENSE file that accompanied this code.
 *
 *     This code is distributed in the hope that it will be useful, but WITHOUT
 *     ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *     FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *     version 2 for more details (a copy is included in the LICENSE file that
 *     accompanied this code).
 *
 *     You should have received a copy of the GNU General Public License version
 *     2 along with this work; if not, write to the Free Software Foundation,
 *     Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 *     Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 *     or visit www.oracle.com if you need additional information or have any
 *     questions.
 * ====
 *
 * Copyright (c) 2020, Red Hat, Inc. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package org.openjdk.shenandoah;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.List;

public class ToolbarPanel extends JPanel
                          implements ActionListener {
    private static final int INITIAL_WIDTH = 2000;
    private static final int INITIAL_HEIGHT = 1200;

    private static final String BACK_1 = "Step back 1";
    private static final String BACK_5 = "Step back 5";
    private static final String PLAY_PAUSE = "play/pause";
    private static final String FORWARD_1 = "Step forward 1";
    private static final String FORWARD_5 = "Step forward 5";
    private static final String END_SNAPSHOT = "End snapshot";

    private static final String PLAYBACK = "Playback";
    private static final String REALTIME = "Realtime";
    private static final String CHOOSE_FILE = "choose file";

    private static final String SPEED_0_5 = "Multiplied speed by 0.5";
    private static final String SPEED_2 = "Multiplied speed by 2";
    private static final String SPEED_RESET = "Reset speed to 1";


    private JToolBar fileToolbar, replayToolbar, statusToolbar, speedToolbar, timestampToolBar;
    private JButton fileButton, backButton_1, backButton_5, playPauseButton, forwardButton_1, forwardButton_5, realtimeModeButton, endSnapshotButton;
    private JButton speedMultiplierButton_0_5, speedMultiplierButton_2, resetSpeedMultiplierButton;
    private JSpinner speedSpinner;
    JSpinner.NumberEditor speedEditor;
    private JTextField fileNameField, lastActionField, modeField, timestampField;
    private JLabel modeLabel, lastActionLabel, speedLabel, timestampLabel;
    private JSlider slider = new JSlider();

    public boolean speedButtonPressed = false;

    private List<Snapshot> snapshots;


    public ToolbarPanel(boolean isReplay) {
        super(new GridBagLayout());
        setPreferredSize(new Dimension(INITIAL_WIDTH, INITIAL_HEIGHT));

        fileToolbar = new JToolBar();
        fileToolbar.setFloatable(false);

        realtimeModeButton = new JButton("Switch to Realtime");
        realtimeModeButton.setActionCommand(REALTIME);
        realtimeModeButton.addActionListener(this);
        fileToolbar.add(realtimeModeButton);

        fileButton = new JButton("Load file");
        fileButton.setActionCommand(CHOOSE_FILE);
        fileButton.addActionListener(this);
        fileToolbar.add(this.fileButton);

        slider.setMinimum(0);
        slider.setOrientation(SwingConstants.HORIZONTAL);
        slider.setValue(0);
        slider.setFocusable(false);

        fileNameField = new JTextField();
        fileNameField.setEditable(false);
        fileToolbar.add(fileNameField);

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 2;
            c.weighty = 1;
            add(fileToolbar, c);
        }

        replayToolbar = new JToolBar();
        replayToolbar.setFloatable(false);

        statusToolbar = new JToolBar();
        statusToolbar.setFloatable(false);

        speedToolbar = new JToolBar();
        speedToolbar.setFloatable(false);

        timestampToolBar = new JToolBar();
        timestampToolBar.setFloatable(false);

        lastActionLabel = new JLabel("Last action:");
        statusToolbar.add(lastActionLabel);

        lastActionField = new JTextField();
        lastActionField.setEditable(false);
        statusToolbar.add(lastActionField);

        modeLabel = new JLabel("Mode:");
        statusToolbar.add(modeLabel);

        modeField = new JTextField();
        modeField.setEditable(false);
        statusToolbar.add(modeField);

        timestampLabel = new JLabel("Timestamp: ");
        timestampToolBar.add(timestampLabel);

        timestampField = new JTextField();
        timestampField.setEditable(false);
        timestampToolBar.add(timestampField);

        addPlaybackButtons();
        addSpeedButtons();

        if (!isReplay) {
            setEnabledPlaybackToolbars(false);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 3;
            c.weighty = 1;
            add(slider, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 1;
            c.weighty = 1;
            add(timestampToolBar, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 1;
            c.weightx = 2;
            c.weighty = 1;
            add(replayToolbar, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 2;
            c.weightx = 2;
            c.weighty = 1;
            add(statusToolbar, c);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 2;
            c.weightx = 2;
            c.weighty = 1;
            add(speedToolbar, c);
        }
    }

    private void setEnabledPlaybackToolbars(boolean b) {
        setEnablePlaybackButtons(b);
        setEnableSpeedButtons(b);
    }

    private void setEnablePlaybackButtons(boolean b) {
        backButton_1.setEnabled(b);
        backButton_5.setEnabled(b);
        playPauseButton.setEnabled(b);
        forwardButton_1.setEnabled(b);
        forwardButton_5.setEnabled(b);
        endSnapshotButton.setEnabled(b);
        slider.setEnabled(b);
    }

    private void setEnableSpeedButtons(boolean b) {
        speedSpinner.setEnabled(b);
        speedMultiplierButton_0_5.setEnabled(b);
        speedMultiplierButton_2.setEnabled(b);
        resetSpeedMultiplierButton.setEnabled(b);
    }

    private void addPlaybackButtons() {
        this.backButton_5 = new JButton("-5");
        backButton_5.setActionCommand(BACK_5);
        backButton_5.addActionListener(this);
        replayToolbar.add(this.backButton_5);

        this.backButton_1 = new JButton("-1");
        backButton_1.setActionCommand(BACK_1);
        backButton_1.addActionListener(this);
        replayToolbar.add(this.backButton_1);

        this.playPauseButton = new JButton("Play/Pause");
        playPauseButton.setActionCommand(PLAY_PAUSE);
        playPauseButton.addActionListener(this);
        replayToolbar.add(this.playPauseButton);

        this.forwardButton_1 = new JButton("+1");
        forwardButton_1.setActionCommand(FORWARD_1);
        forwardButton_1.addActionListener(this);
        replayToolbar.add(this.forwardButton_1);

        this.forwardButton_5 = new JButton("+5");
        forwardButton_5.setActionCommand(FORWARD_5);
        forwardButton_5.addActionListener(this);
        replayToolbar.add(this.forwardButton_5);

        this.endSnapshotButton = new JButton("End Snapshot");
        endSnapshotButton.setActionCommand(END_SNAPSHOT);
        endSnapshotButton.setFocusable(false);
        endSnapshotButton.addActionListener(this);
        replayToolbar.add(this.endSnapshotButton);
    }

    private void addSpeedButtons() {
        speedLabel = new JLabel("Playback Speed: ");
        statusToolbar.add(speedLabel);

        this.speedSpinner = new JSpinner(new SpinnerNumberModel(1.0,0.1,10.0,0.1));
        this.speedEditor = new JSpinner.NumberEditor(speedSpinner,"#.#");
        speedSpinner.setEditor(speedEditor);
        statusToolbar.add(speedSpinner);

        speedMultiplierButton_0_5 = new JButton("0.5x");
        speedMultiplierButton_0_5.setActionCommand(SPEED_0_5);
        speedMultiplierButton_0_5.addActionListener(this);
        speedToolbar.add(speedMultiplierButton_0_5);

        speedMultiplierButton_2 = new JButton("2x");
        speedMultiplierButton_2.setActionCommand(SPEED_2);
        speedMultiplierButton_2.addActionListener(this);
        speedToolbar.add(speedMultiplierButton_2);

        resetSpeedMultiplierButton = new JButton("RESET");
        resetSpeedMultiplierButton.setActionCommand(SPEED_RESET);
        resetSpeedMultiplierButton.addActionListener(this);
        speedToolbar.add(resetSpeedMultiplierButton);
    }

    public double getSpeedValue() {
        JFormattedTextField speedField = getTextField(speedSpinner);
        double speedValue = (double) speedField.getValue();
        try {
            speedEditor.commitEdit();
            speedSpinner.commitEdit();
        } catch (ParseException e) {
            System.out.println("Speed value must be a double in range (0.0,10.0]. Inputted value: " + speedValue);
        }
        return speedValue;
    }

    public void setSpeedValue(double speed) {
        JFormattedTextField speedField = getTextField(speedSpinner);
        try {
            speedField.setValue(speed);
            speedEditor.commitEdit();
            speedSpinner.commitEdit();
        } catch (ParseException e) {
            System.out.println("Speed value must be a double in range (0.0,10.0]. Inputted value: " + speed);
        }
    }

    private JFormattedTextField getTextField(JSpinner spinner) {
        JComponent editor = spinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            return ((JSpinner.DefaultEditor)editor).getTextField();
        } else {
            return null;
        }
    }

    // File Toolbar Listeners
    public void setFileButtonListener(ActionListener a) {
        fileButton.addActionListener(a);
    }

    public void setRealtimeModeButtonListener(ActionListener a) {
        realtimeModeButton.addActionListener(a);
    }

    // Replay Toolbar Listeners
    public void setBackButton_1_Listener(ActionListener a) {
        backButton_1.addActionListener(a);
    }

    public void setBackButton_5_Listener(ActionListener a) {
        backButton_5.addActionListener(a);
    }

    public void setPlayPauseButtonListener(ActionListener a) {
        playPauseButton.addActionListener(a);
    }

    public void setForwardButton_1_Listener(ActionListener a) {
        forwardButton_1.addActionListener(a);
    }

    public void setForwardButton_5_Listener(ActionListener a) {
        forwardButton_5.addActionListener(a);
    }
    public void setEndSnapshotButtonListener(ActionListener a) {
        endSnapshotButton.addActionListener(a);
    }
    public void setSliderListener(ChangeListener c) {
        slider.addChangeListener(c);
    }

    // Speed Toolbar Listeners
    public void setSpeedSpinnerListener(ChangeListener c) {
        speedSpinner.addChangeListener(c);
    }

    public void setSpeed_0_5_Listener(ActionListener ae) {
        speedMultiplierButton_0_5.addActionListener(ae);
    }

    public void setSpeed_2_Listener(ActionListener ae) {
        speedMultiplierButton_2.addActionListener(ae);
    }

    public void setResetSpeedListener(ActionListener ae) {
        resetSpeedMultiplierButton.addActionListener(ae);
    }

    public void setFileNameField(String s) {
        fileNameField.setText(s);
    }

    public void setLastActionField(String s) {
        lastActionField.setText(s);
    }

    public void setEnabledRealtimeModeButton(boolean b) {
        realtimeModeButton.setEnabled(b);
    }

    public void setModeField(String s) {
        if (REALTIME.equals(s)) {
            modeField.setText(REALTIME);
        } else if (PLAYBACK.equals(s)) {
            modeField.setText(PLAYBACK);
        }
    }

    public void actionPerformed(ActionEvent a) {
        String cmd = a.getActionCommand();
        if (CHOOSE_FILE.equals(cmd)) {
            setEnabledPlaybackToolbars(true);
            setModeField(PLAYBACK);
            setEnabledRealtimeModeButton(true);
        } else if (REALTIME.equals(cmd)) {
            lastActionField.setText("Switched to realtime mode.");
            setEnabledPlaybackToolbars(false);
            setModeField(REALTIME);
            setEnabledRealtimeModeButton(false);
        } else if ( !(PLAY_PAUSE.equals(cmd) || SPEED_0_5.equals(cmd) || SPEED_2.equals(cmd) || SPEED_RESET.equals(cmd)) ){
            lastActionField.setText(cmd + " button pressed.");
        }
    }
    public final void setSnapshots(List<Snapshot> snapshots) {
        this.snapshots = snapshots;
    }
    public final void setSize(int size) {
        slider.setMaximum(size);
    }
    public final void setValue(int value) {
        slider.setValue(value);
    }
    public int currentSliderValue() {
        if ((slider.getValue() - 1) >= 0) {
            timestampField.setText(Long.toString(snapshots.get(slider.getValue() - 1).time()) + " ms");
        }
        return slider.getValue();
    }
 }
