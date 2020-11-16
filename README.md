# Shenandoah Visualizer

Shenandoah Visualizer is a low-level tool to visualize the internal state of
[Shenandoah GC](https://wiki.openjdk.java.net/display/Shenandoah). It relies on
jvmstat interface to pull the data from the live JVM.

![Sample Shenandoah Visualizer Screenshot](images/sample-screenshot.png)

## Building

Build as any Maven-driven Java project:

    $ mvn clean verify

...or pick up the binary build [from here](https://builds.shipilev.net/shenandoah-visualizer/).

## Usage

*Step 1.* Start target JVM with with these additional flags:

    $ java -XX:+UsePerfData -XX:+UnlockExperimentalVMOptions -XX:+ShenandoahRegionSampling ...

*Step 2.* Figure out the target JVM PID:

    $ jps

*Step 3.* Attach the Visualizer:

    $ java -Xbootclasspath/p:<path-to-tools.jar> -jar visualizer.jar local://<pid>

`tools.jar` can usually be found at `$JAVA_HOME/lib`
