JAVA_FILES=ConcurrentListMain.java List.java ListThread.java Node.java
ASPECTLOCKS=AspectLocks.java
ASPECTSYNCHRONIZED=AspectSynchronized.java

ASPECTJRT=/usr/share/java/aspectjrt.jar

compileLocks: ${JAVA_FILES} ${ASPECTLOCKS}
	@ajc -source 1.8 ${JAVA_FILES} ${ASPECTLOCKS}

compileSynchronized: ${JAVA_FILES} ${ASPECTSYNCHRONIZED}
	@ajc -source 1.8 ${JAVA_FILES} ${ASPECTSYNCHRONIZED}

locks: compileLocks
	@java  -cp ${ASPECTJRT}:. ConcurrentListMain

synchronized: compileSynchronized
	@java  -cp ${ASPECTJRT}:. ConcurrentListMain
