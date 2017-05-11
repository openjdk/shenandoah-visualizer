package org.openjdk.shenandoah;

import java.awt.*;

public class Colors {

    static final Color TIMELINE_IDLE        = Color.BLACK;
    static final Color TIMELINE_MARK        = new Color(100, 100, 0);
    static final Color TIMELINE_EVACUATING  = new Color(100, 0, 0);
    static final Color TIMELINE_UPDATEREFS  = new Color(0, 100, 100);

    static final Color TLAB_ALLOC           = new Color(0, 250, 250);
    static final Color TLAB_ALLOC_BORDER    = new Color(0, 191, 190);
    static final Color GCLAB_ALLOC          = new Color(185, 0, 250);
    static final Color GCLAB_ALLOC_BORDER   = new Color(118, 0, 160);
    static final Color USED                 = new Color(150, 150, 150);
    static final Color LIVE                 = new Color(0, 200, 0);
    static final Color LIVE_BORDER          = new Color(0, 100, 0);
    static final Color CSET                 = Color.YELLOW;
    static final Color HUMONGOUS            = Color.RED;
}
