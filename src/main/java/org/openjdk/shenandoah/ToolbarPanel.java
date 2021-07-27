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
    private static final String BACK_1 = "back 1";
    private static final String BACK_5 = "back 5";
    private static final String PLAY_PAUSE = "play/pause";
    private static final String FORWARD_1 = "forward 1";
    private static final String FORWARD_5 = "forward 5";


    private JToolBar fileToolbar;
    private JToolBar replayToolbar;
    private boolean isReplay;
    private JButton fileButton, backButton_1, backButton_5, playPauseButton, forwardButton_1, forwardButton_5;
    private JTextField fileNameField;

    public ToolbarPanel(boolean isReplay) {
        super(new GridBagLayout());
        setPreferredSize(new Dimension(INITIAL_WIDTH, INITIAL_HEIGHT));

        this.isReplay = isReplay;

        this.fileToolbar = new JToolBar();
        fileToolbar.setFloatable(false);

        this.fileButton = new JButton("Load file");
        fileButton.setActionCommand(CHOOSE_FILE);
        fileButton.addActionListener(this);
        fileToolbar.add(this.fileButton);

        this.fileNameField = new JTextField();
        fileNameField.setEditable(false);
        fileToolbar.add(fileNameField);

        {
            GridBagConstraints c = new GridBagConstraints();
            c.fill = GridBagConstraints.BOTH;
            c.gridx = 0;
            c.gridy = 0;
            c.weightx = 2;
            c.weighty = 1;
            add(this.fileToolbar, c);
        }

        this.replayToolbar = new JToolBar();
        replayToolbar.setFloatable(false);
        addReplayButtons();
        if (!this.isReplay) {
            setEnableReplayButtons(false);
        }
        addReplayToolbar();
    }

    private void setEnableReplayButtons(boolean b) {
        backButton_1.setEnabled(b);
        backButton_5.setEnabled(b);
        playPauseButton.setEnabled(b);
        forwardButton_1.setEnabled(b);
        forwardButton_5.setEnabled(b);
    }

    public void addReplayToolbar() {
        System.out.println("Inside addReplayToolbar");
        GridBagConstraints c = new GridBagConstraints();
        c.fill = GridBagConstraints.BOTH;
        c.gridx = 1;
        c.gridy = 0;
        c.weightx = 2;
        c.weighty = 1;
        this.add(this.replayToolbar, c);
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

    public void setFileNameField(String s) {
        fileNameField.setText(s);
    }

    public void actionPerformed(ActionEvent a) {
        String cmd = a.getActionCommand();
        if (CHOOSE_FILE.equals(cmd)) {
            isReplay = true;
            setEnableReplayButtons(true);
            System.out.println(CHOOSE_FILE + " button pressed.");
        } else if (BACK_1.equals(cmd) || BACK_5.equals(cmd)) {
            System.out.println(cmd + " button pressed.");
        } else if (PLAY_PAUSE.equals(cmd)) {
            System.out.println(PLAY_PAUSE + " button pressed.");
        } else if (FORWARD_1.equals(cmd) || FORWARD_5.equals(cmd)) {
            System.out.println(cmd + " button pressed.");
        }
    }

}
