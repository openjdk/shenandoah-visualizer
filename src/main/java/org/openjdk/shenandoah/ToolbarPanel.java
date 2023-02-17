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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.text.ParseException;

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
    private final RenderRunner renderRunner;
    private JToolBar fileToolbar, replayToolbar, statusToolbar, speedToolbar, timestampToolBar;
    private JButton fileButton, backOneButton, backFiveButton, playPauseButton;
    private JButton forwardOneButton, forwardFiveButton, realtimeModeButton, endSnapshotButton;
    private JButton halfSpeedButton, doubleSpeedButton, resetSpeedMultiplierButton;
    private JSpinner speedSpinner;
    JSpinner.NumberEditor speedEditor;
    private JTextField fileNameField, lastActionField, modeField, timestampField;
    private JLabel modeLabel, lastActionLabel, speedLabel, timestampLabel;
    private JSlider slider = new JSlider();

    public boolean speedButtonPressed = false;


    public ToolbarPanel(RenderRunner renderRunner) {
        super(new GridBagLayout());

        this.renderRunner = renderRunner;

        setPreferredSize(new Dimension(INITIAL_WIDTH, INITIAL_HEIGHT));

        fileToolbar = new JToolBar();
        fileToolbar.setFloatable(false);

        realtimeModeButton = new JButton("Switch to Realtime");
        realtimeModeButton.setActionCommand(REALTIME);
        realtimeModeButton.addActionListener(this);
        realtimeModeButton.setFocusable(false);
        fileToolbar.add(realtimeModeButton);

        fileButton = new JButton("Load file");
        fileButton.setActionCommand(CHOOSE_FILE);
        fileButton.addActionListener(this);
        fileButton.setFocusable(false);
        fileToolbar.add(this.fileButton);

        // TODO: We'll want to update the event count in Live mode
        slider.setMaximum(renderRunner.snapshotCount());
        slider.setMinimum(0);
        slider.setOrientation(SwingConstants.HORIZONTAL);
        slider.setValue(0);
        slider.setFocusable(false);

        fileNameField = new JTextField();
        fileNameField.setEditable(false);
        fileNameField.setFocusable(false);
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
        lastActionField.setFocusable(false);
        statusToolbar.add(lastActionField);

        modeLabel = new JLabel("Mode:");
        statusToolbar.add(modeLabel);

        modeField = new JTextField();
        modeField.setEditable(false);
        modeField.setFocusable(false);
        statusToolbar.add(modeField);

        timestampLabel = new JLabel("Timestamp: ");
        timestampToolBar.add(timestampLabel);

        timestampField = new JTextField();
        timestampField.setEditable(false);
        timestampField.setFocusable(false);
        timestampToolBar.add(timestampField);

        addPlaybackButtons();
        addSpeedButtons();

        setEnabledPlaybackToolbars(true);

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

        realtimeModeButton.addActionListener(event -> {
            DataProvider data = new DataProvider(null);
            renderRunner.loadLive(data);
            setFileNameField("");
        });
        fileButton.addActionListener(this::onFileButtonEvent);
        playPauseButton.addActionListener(this::onPlayPauseEvent);
        backOneButton.addActionListener(event -> renderRunner.stepBy(-1));
        backFiveButton.addActionListener(event -> renderRunner.stepBy(-5));
        forwardOneButton.addActionListener(event -> renderRunner.stepBy(1));
        forwardFiveButton.addActionListener(event -> renderRunner.stepBy(5));
        endSnapshotButton.addActionListener(event -> renderRunner.stepToEnd());
        slider.addChangeListener(changeEvent -> renderRunner.stepTo(slider.getValue()));
        speedSpinner.addChangeListener(this::onSpeedSpinnerChanged);
        halfSpeedButton.addActionListener(event -> {
            double speed = Math.max(0.1, renderRunner.getPlaybackSpeed() * 0.5);
            changeSpeed(speed);
        });
        doubleSpeedButton.addActionListener(event -> {
            double speed = Math.max(0.1, renderRunner.getPlaybackSpeed() * 2.0);
            changeSpeed(speed);
        });
        resetSpeedMultiplierButton.addActionListener(event -> changeSpeed(1.0));
    }

    private void changeSpeed(double speed) {
        speedButtonPressed = true;
        renderRunner.setPlaybackSpeed(speed);
        setSpeedValue(speed);
        speedButtonPressed = false;
        setLastActionField("Set speed to " + speed);
    }

    private void onSpeedSpinnerChanged(ChangeEvent changeEvent) {
        if (!speedButtonPressed) {
            double speed = getSpeedValue();
            if (speed != renderRunner.getPlaybackSpeed()) {
                setLastActionField("Changed playback speed to: " + speed);
                renderRunner.setPlaybackSpeed(speed);
            }
        }
    }

    private void setEnabledPlaybackToolbars(boolean b) {
        setEnablePlaybackButtons(b);
        setEnableSpeedButtons(b);
    }

    private void setEnablePlaybackButtons(boolean b) {
        backOneButton.setEnabled(b);
        backFiveButton.setEnabled(b);
        playPauseButton.setEnabled(b);
        forwardOneButton.setEnabled(b);
        forwardFiveButton.setEnabled(b);
        endSnapshotButton.setEnabled(b);
        slider.setEnabled(b);
    }

    private void setEnableSpeedButtons(boolean b) {
        speedSpinner.setEnabled(b);
        halfSpeedButton.setEnabled(b);
        doubleSpeedButton.setEnabled(b);
        resetSpeedMultiplierButton.setEnabled(b);
    }

    private void addPlaybackButtons() {
        this.backFiveButton = new JButton("-5");
        backFiveButton.setActionCommand(BACK_5);
        backFiveButton.addActionListener(this);
        backFiveButton.setFocusable(false);
        replayToolbar.add(this.backFiveButton);

        this.backOneButton = new JButton("-1");
        backOneButton.setActionCommand(BACK_1);
        backOneButton.addActionListener(this);
        backOneButton.setFocusable(false);
        replayToolbar.add(this.backOneButton);

        this.playPauseButton = new JButton("Play/Pause");
        playPauseButton.setActionCommand(PLAY_PAUSE);
        playPauseButton.addActionListener(this);
        playPauseButton.setFocusable(false);
        replayToolbar.add(this.playPauseButton);

        this.forwardOneButton = new JButton("+1");
        forwardOneButton.setActionCommand(FORWARD_1);
        forwardOneButton.addActionListener(this);
        forwardOneButton.setFocusable(false);
        replayToolbar.add(this.forwardOneButton);

        this.forwardFiveButton = new JButton("+5");
        forwardFiveButton.setActionCommand(FORWARD_5);
        forwardFiveButton.addActionListener(this);
        forwardFiveButton.setFocusable(false);
        replayToolbar.add(this.forwardFiveButton);

        this.endSnapshotButton = new JButton("End Snapshot");
        endSnapshotButton.setActionCommand(END_SNAPSHOT);
        endSnapshotButton.setFocusable(false);
        endSnapshotButton.addActionListener(this);
        replayToolbar.add(this.endSnapshotButton);
    }

    private void addSpeedButtons() {
        speedLabel = new JLabel("Playback Speed: ");
        statusToolbar.add(speedLabel);

        this.speedSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 10.0, 0.1));
        this.speedEditor = new JSpinner.NumberEditor(speedSpinner, "#.#");
        speedSpinner.setEditor(speedEditor);
        speedSpinner.setFocusable(false);
        speedEditor.setFocusable(false);
        statusToolbar.add(speedSpinner);

        halfSpeedButton = new JButton("0.5x");
        halfSpeedButton.setActionCommand(SPEED_0_5);
        halfSpeedButton.addActionListener(this);
        halfSpeedButton.setFocusable(false);
        speedToolbar.add(halfSpeedButton);

        doubleSpeedButton = new JButton("2x");
        doubleSpeedButton.setActionCommand(SPEED_2);
        doubleSpeedButton.addActionListener(this);
        doubleSpeedButton.setFocusable(false);
        speedToolbar.add(doubleSpeedButton);

        resetSpeedMultiplierButton = new JButton("RESET");
        resetSpeedMultiplierButton.setActionCommand(SPEED_RESET);
        resetSpeedMultiplierButton.addActionListener(this);
        resetSpeedMultiplierButton.setFocusable(false);
        speedToolbar.add(resetSpeedMultiplierButton);
    }

    public double getSpeedValue() {
        JFormattedTextField speedField = getTextField(speedSpinner);
        requestFocusInWindow();
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
            return ((JSpinner.DefaultEditor) editor).getTextField();
        } else {
            return null;
        }
    }

    // File Toolbar Listeners
    private void onPlayPauseEvent(ActionEvent ae) {
        if (renderRunner.isPaused()) {
            setLastActionField("Play button pressed.");
        } else {
            setLastActionField("Pause button pressed.");
        }
        renderRunner.togglePlayback();
    }

    private void onFileButtonEvent(ActionEvent ae) {
        JFileChooser fc = new JFileChooser();
        int returnValue = fc.showOpenDialog(null);
        if (returnValue == JFileChooser.APPROVE_OPTION) {
            String filePath = fc.getSelectedFile().getAbsolutePath();
            renderRunner.loadPlayback(filePath);
            slider.setMaximum(renderRunner.snapshotCount());
            renderRunner.setPlaybackSpeed(1.0);
            setSpeedValue(1.0);
            setFileNameField(filePath);
            setLastActionField("File selected: " + filePath);
            renderRunner.frame.repaint();
        }
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
        } else if (!(PLAY_PAUSE.equals(cmd) || SPEED_0_5.equals(cmd) || SPEED_2.equals(cmd) || SPEED_RESET.equals(cmd))) {
            lastActionField.setText(cmd + " button pressed.");
        }
    }

    public final void setValue(int value) {
        if (SwingUtilities.isEventDispatchThread()) {
            slider.setValue(value);
        } else {
            SwingUtilities.invokeLater(() -> slider.setValue(value));
        }
    }

    @Override
    public void paint(Graphics g) {
        timestampField.setText(renderRunner.snapshot().time() + " ms");
        super.paint(g);
    }
}
