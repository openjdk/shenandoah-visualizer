package org.openjdk.shenandoah;

public enum RegionState {

    EMPTY_UNCOMMITTED,

    EMPTY_COMMITTED,

    REGULAR,

    HUMONGOUS,

    CSET,

    PINNED,

    TRASH,

    PINNED_CSET,

    PINNED_HUMONGOUS,

    ;

    static RegionState fromOrdinal(int idx) {
        switch (idx) {
            case 0: return EMPTY_UNCOMMITTED;
            case 1: return EMPTY_COMMITTED;
            case 2: return REGULAR;
            case 3: return HUMONGOUS;
            case 4: return HUMONGOUS;
            case 5: return CSET;
            case 6: return PINNED;
            case 7: return TRASH;
            case 8: return PINNED_CSET;
            case 9: return PINNED_HUMONGOUS;
            default:
                throw new IllegalStateException("Unhandled ordinal: " + idx);
        }
    }

}
