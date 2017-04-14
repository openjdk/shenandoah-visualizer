package org.openjdk.shenandoah;

public enum Phase {

    IDLE,

    MARKING,

    EVACUATING,

    UPDATE_REFS,

    UNKNOWN,

}