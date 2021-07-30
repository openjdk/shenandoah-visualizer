/*
 * ====
 *     Copyright (c) 2020, Red Hat, Inc. All rights reserved.
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
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class ToolbarPanel extends JPanel
                          implements ActionListener {
    private static final int INITIAL_WIDTH = 1500;
    private static final int INITIAL_HEIGHT = 1200;
    private static final String CHOOSE_FILE = "choose file";
    private static final String BACK_1 = "Step back 1";
    private static final String BACK_5 = "Step back 5";
    private static final String PLAY_PAUSE = "play/pause";
    private static final String FORWARD_1 = "Step forward 1";
    private static final String FORWARD_5 = "Step forward 5";
    private static final String PLAYBACK = "Playback";
    private static final String REALTIME = "Realtime";
    private static final String PLAYING = "Playing";
    private static final String PAUSED = "Paused";
    private static final String IDLE = "Idle";


    private JToolBar fileToolbar, replayToolbar, statusToolbar;
    private boolean isReplay;
    private JButton fileButton, backButton_1, backButton_5, playPauseButton, forwardButton_1, forwardButton_5;
    private JButton realtimeModeButton;
    private JTextField fileNameField, lastActionField, modeField;
    private JLabel modeLabel, lastActionLabel;

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

        fileNameField = new JTextField();
        fileNameField.setEditable(false);
        fileToolbar.add(fileNameField);

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 2;
            c.weighty = 1;
            add(fileToolbar, c);
        }

        replayToolbar = new JToolBar();
        replayToolbar.setFloatable(false);
        addReplayButtons();
        if (!isReplay) {
            setEnableReplayButtons(false);
        }

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 1;
            c.gridy = 0;
            c.weightx = 2;
            c.weighty = 1;
            add(replayToolbar, c);
        }

        statusToolbar = new JToolBar();
        statusToolbar.setFloatable(false);

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

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 1;
            c.weightx = 4;
            c.weighty = 1;
            add(statusToolbar, c);
        }
    }

    private void setEnableReplayButtons(boolean b) {
        backButton_1.setEnabled(b);
        backButton_5.setEnabled(b);
        playPauseButton.setEnabled(b);
        forwardButton_1.setEnabled(b);
        forwardButton_5.setEnabled(b);
    }

    private void addReplayButtons() {
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
    }

    public void setFileButtonListener(ActionListener a) {
        fileButton.addActionListener(a);
    }

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

    public void setRealtimeModeButtonListener(ActionListener a) {
        realtimeModeButton.addActionListener(a);
    }

    public void setFileNameField(String s) {
        fileNameField.setText(s);
    }

    public void setLastActionField(String s) {
        lastActionField.setText(s);
    }

    public void setMode(String s) {
        if (REALTIME.equals(s)) {
            modeField.setText(REALTIME);
            realtimeModeButton.setEnabled(false);
        } else if (PLAYBACK.equals(s)) {
            modeField.setText(PLAYBACK);
            realtimeModeButton.setEnabled(true);
        }
    }

    public void actionPerformed(ActionEvent a) {
        String cmd = a.getActionCommand();
        if (CHOOSE_FILE.equals(cmd)) {
            isReplay = true;
            setEnableReplayButtons(true);
            setMode(PLAYBACK);
        } else if (REALTIME.equals(cmd)) {
            lastActionField.setText("Switched to realtime mode.");
            setEnableReplayButtons(false);
            setMode(REALTIME);
        } else {
            lastActionField.setText(cmd + " button pressed.");
        }
    }

}
