/*
 * Copyright (c) 2023, Amazon.com, Inc. All rights reserved.
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
import java.awt.event.KeyAdapter;
import java.text.ParseException;

class ToolbarPanel extends JPanel
        implements ActionListener {

    private static final String BACK_1 = "Step back 1";
    private static final String BACK_5 = "Step back 5";
    private static final String PLAY_PAUSE = "Pause";
    private static final String FORWARD_1 = "Step forward 1";
    private static final String FORWARD_5 = "Step forward 5";
    private static final String END_SNAPSHOT = "End snapshot";

    private static final String REALTIME = "Realtime";
    private static final String CHOOSE_FILE = "choose file";

    private static final String SPEED_0_5 = "Multiplied speed by 0.5";
    private static final String SPEED_2 = "Multiplied speed by 2";
    private static final String SPEED_RESET = "Reset speed to 1";
    private final RenderRunner renderRunner;
    private final JToolBar replayToolbar, statusToolbar, speedToolbar;
    private JButton backOneButton, backFiveButton, playPauseButton, forwardOneButton, forwardFiveButton, endSnapshotButton;
    private final JButton realtimeModeButton;
    private JButton halfSpeedButton, doubleSpeedButton, resetSpeedMultiplierButton;
    private JSpinner speedSpinner;
    private JSpinner.NumberEditor speedEditor;
    private final JTextField fileNameField, lastActionField, modeField, timestampField;
    private final JSlider slider;

    boolean speedButtonPressed = false;


    ToolbarPanel(RenderRunner renderRunner, KeyAdapter keyShortcutAdapter) {
        super(new GridBagLayout());

        this.renderRunner = renderRunner;

        addKeyListener(keyShortcutAdapter);

        JToolBar fileToolbar = new JToolBar();
        fileToolbar.setFloatable(false);

        var history = new RegionHistory(renderRunner, keyShortcutAdapter);
        renderRunner.addPopup(history);

        var historyButton = new JButton("Show History");
        historyButton.addActionListener(e -> history.setVisible(true));
        fileToolbar.add(historyButton);

        realtimeModeButton = new JButton("Switch to Live");
        realtimeModeButton.setActionCommand(REALTIME);
        realtimeModeButton.addActionListener(this);
        realtimeModeButton.setFocusable(false);
        fileToolbar.add(realtimeModeButton);

        JButton fileButton = new JButton("Load file");
        fileButton.setActionCommand(CHOOSE_FILE);
        fileButton.addActionListener(this);
        fileButton.setFocusable(false);
        fileToolbar.add(fileButton);

        fileNameField = new JTextField();
        fileNameField.setEditable(false);
        fileNameField.setFocusable(false);
        fileToolbar.add(fileNameField);

        slider = new JSlider();
        slider.setMinimum(0);
        slider.setOrientation(SwingConstants.HORIZONTAL);
        slider.setValue(0);
        slider.setFocusable(false);
        renderRunner.onRecordingLoaded(() -> {
            SwingUtilities.invokeLater(() -> {
                slider.setMaximum(renderRunner.snapshotCount());
            });
        });

        replayToolbar = new JToolBar();
        replayToolbar.setFloatable(false);
        replayToolbar.setPreferredSize(new Dimension(400, 10));

        statusToolbar = new JToolBar();
        statusToolbar.setFloatable(false);

        speedToolbar = new JToolBar();
        speedToolbar.setFloatable(false);

        JToolBar timestampToolBar = new JToolBar();
        timestampToolBar.setFloatable(false);

        JLabel lastActionLabel = new JLabel("Last action:");
        statusToolbar.add(lastActionLabel);

        lastActionField = new JTextField();
        lastActionField.setEditable(false);
        lastActionField.setFocusable(false);
        statusToolbar.add(lastActionField);

        JLabel modeLabel = new JLabel("Mode:");
        statusToolbar.add(modeLabel);

        modeField = new JTextField();
        modeField.setEditable(false);
        modeField.setFocusable(false);
        statusToolbar.add(modeField);

        JLabel timestampLabel = new JLabel("Timestamp: ");
        timestampToolBar.add(timestampLabel);

        timestampField = new JTextField();
        timestampField.setEditable(false);
        timestampField.setFocusable(false);
        timestampToolBar.add(timestampField);

        addPlaybackButtons();
        addSpeedButtons();

        setEnabledPlaybackToolbars();

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
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 2;
            c.weighty = 1;
            add(fileToolbar, c);
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
            renderRunner.loadLive(null);
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

    private void setEnabledPlaybackToolbars() {
        setEnablePlaybackButtons();
        setEnableSpeedButtons();
    }

    private void setEnablePlaybackButtons() {
        backOneButton.setEnabled(true);
        backFiveButton.setEnabled(true);
        playPauseButton.setEnabled(true);
        forwardOneButton.setEnabled(true);
        forwardFiveButton.setEnabled(true);
        endSnapshotButton.setEnabled(true);
        slider.setEnabled(true);
    }

    private void setEnableSpeedButtons() {
        speedSpinner.setEnabled(true);
        halfSpeedButton.setEnabled(true);
        doubleSpeedButton.setEnabled(true);
        resetSpeedMultiplierButton.setEnabled(true);
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

        this.playPauseButton = new JButton(PLAY_PAUSE);
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
        JLabel speedLabel = new JLabel("Playback Speed: ");
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

    double getSpeedValue() {
        JFormattedTextField speedField = getTextField(speedSpinner);
        if (speedField == null) {
            return 0.0;
        }

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

    void setSpeedValue(double speed) {
        JFormattedTextField speedField = getTextField(speedSpinner);
        if (speedField == null) {
            return;
        }

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
            renderRunner.setPlaybackSpeed(1.0);
            setSpeedValue(1.0);
            setFileNameField(filePath);
            setLastActionField("File selected: " + filePath);
        }
    }

    void setFileNameField(String s) {
        fileNameField.setText(s);
    }

    void setLastActionField(String s) {
        lastActionField.setText(s);
    }

    public void actionPerformed(ActionEvent a) {
        String cmd = a.getActionCommand();
        if (CHOOSE_FILE.equals(cmd)) {
            setLastActionField("Switched to recorded playback mode.");
        } else if (REALTIME.equals(cmd)) {
            setLastActionField("Switched to live playback mode.");
        } else if (!(PLAY_PAUSE.equals(cmd) || SPEED_0_5.equals(cmd) || SPEED_2.equals(cmd) || SPEED_RESET.equals(cmd))) {
            setLastActionField(cmd + " button pressed.");
        }
    }

    @Override
    public void paint(Graphics g) {
        timestampField.setText(renderRunner.snapshot().time() + " ms");
        slider.setValue(renderRunner.cursor());
        realtimeModeButton.setEnabled(!renderRunner.isLive());
        modeField.setText(renderRunner.status());
        playPauseButton.setText(renderRunner.isPaused() ? "Play" : "Pause");
        super.paint(g);
    }
}
