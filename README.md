# Shenandoah Visualizer

Shenandoah Visualizer is the low-level tool to visualize the internal state of 
[Shenandoah GC](https://wiki.openjdk.java.net/display/Shenandoah). It relies on 

## Building

Build as any Maven-driven Java project:

    $ mvn clean verify

...or pick up the binary build [from here](https://builds.shipilev.net/shenandoah-visualizer/).
 
## Usage

 1. Start JVM with with these additional flags:
 
    $ java -XX:+UsePerfData -XX:+UnlockExperimentalVMOptions -XX:+ShenandoahRegionSampling ...

 2. Figure out the process PID:
 
    $ jps 

 3. Attach Visualizer:
 
    $ java -Xbootclasspath/p:<path-to-tools.jar> -jar visualizer.jar local://<pid>

tools.jar is usually at $JAVA_HOME/lib
