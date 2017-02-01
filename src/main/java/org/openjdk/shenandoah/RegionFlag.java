package org.openjdk.shenandoah;

public enum RegionFlag {

    UNUSED,

    HUMONGOUS,

    IN_COLLECTION_SET,

    RECENTLY_ALLOCATED,

    PINNED,

}
