package org.openjdk.shenandoah;

import java.awt.*;

public class Colors {

    static final Color TIMELINE_IDLE        = Color.BLACK;
    static final Color TIMELINE_MARK        = new Color(100, 100, 0);
    static final Color TIMELINE_EVACUATING  = new Color(100, 0, 0);
    static final Color TIMELINE_UPDATEREFS  = new Color(0, 100, 100);
    static final Color TIMELINE_TRAVERSAL   = TIMELINE_EVACUATING;

    static final Color SHARED_ALLOC           = new Color(0, 250, 250);
    static final Color SHARED_ALLOC_BORDER    = new Color(0, 191, 190);
    static final Color TLAB_ALLOC           = new Color(0, 200, 0);
    static final Color TLAB_ALLOC_BORDER    = new Color(0, 100, 0);
    static final Color GCLAB_ALLOC          = new Color(185, 0, 250);
    static final Color GCLAB_ALLOC_BORDER   = new Color(118, 0, 160);

    static final Color USED                 = new Color(200, 200, 200);

    static final Color LIVE_COMMITTED       = new Color(150, 150, 150);
    static final Color LIVE_REGULAR         = new Color(0, 200, 0);
    static final Color LIVE_HUMONGOUS       = new Color(250, 100, 0);
    static final Color LIVE_PINNED_HUMONGOUS = new Color(255, 0, 0);
    static final Color LIVE_CSET            = new Color(250, 250, 0);
    static final Color LIVE_TRASH           = new Color(100, 100, 100);
    static final Color LIVE_PINNED          = new Color(255, 0, 0);
    static final Color LIVE_PINNED_CSET     = new Color(255, 120, 0);
    static final Color LIVE_EMPTY           = new Color(255, 255, 255);

    static final Color LIVE_BORDER          = new Color(0, 100, 0);
    static final Color BORDER               = new Color(150, 150, 150);
}
