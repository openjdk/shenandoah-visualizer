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
    private static final String BACK = "back";
    private static final String PLAY_PAUSE = "play/pause";
    private static final String FORWARD = "choose file";


    private JToolBar fileToolbar;
    private JToolBar replayToolbar;
    private boolean isReplay;
    private JButton fileButton, backButton, playPauseButton, forwardButton;
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
        backButton.setEnabled(b);
        playPauseButton.setEnabled(b);
        forwardButton.setEnabled(b);
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
        this.backButton = new JButton("Back");
        backButton.setActionCommand(BACK);
        backButton.addActionListener(this);
        replayToolbar.add(this.backButton);

        this.playPauseButton = new JButton("Play/Pause");
        playPauseButton.setActionCommand(PLAY_PAUSE);
        playPauseButton.addActionListener(this);
        replayToolbar.add(this.playPauseButton);

        this.forwardButton = new JButton("Forward");
        forwardButton.setActionCommand(FORWARD);
        forwardButton.addActionListener(this);
        replayToolbar.add(this.forwardButton);
    }

    public void setFileButtonListener(ActionListener a) {
        fileButton.addActionListener(a);
    }

    public void setBackButtonListener(ActionListener a) {
        backButton.addActionListener(a);
    }

    public void setPlayPauseButtonListener(ActionListener a) {
        playPauseButton.addActionListener(a);
    }

    public void setForwardButtonListener(ActionListener a) {
        forwardButton.addActionListener(a);
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
        } else if (BACK.equals(cmd)) {
            System.out.println(BACK + " button pressed.");
        } else if (PLAY_PAUSE.equals(cmd)) {
            System.out.println(PLAY_PAUSE + " button pressed.");
        } else if (FORWARD.equals(cmd)) {
            System.out.println(FORWARD + " button pressed.");
        }
    }

}
