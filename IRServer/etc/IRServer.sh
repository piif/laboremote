#!/bin/bash

HERE="$(cd $(dirname $0) ; /bin/pwd)"
ROOTDIR="$(dirname "$HERE")"

#TXRX="$ROOTDIR/lib/RXTXcomm.jar"
#TXRX=/usr/share/java/RXTXcomm.jar


LIBPATH=/usr/lib64/rxtx

if expr $(uname) : CYGWIN > /dev/null ; then
	CLASSPATH="$ROOTDIR/bin;$ROOTDIR/resources;$ROOTDIR/lib/RXTXcomm.jar"
	convPath() {
		echo "$1" | tr ";" "\n" | while read l ; do
			cygpath -w "$l"
		done | tr "\n" ";"
	}
	CLASSPATH="$(convPath "$CLASSPATH")"
	LIBPATH="$(convPath "$ROOTDIR/lib/ch-rxtx-2.2-20081207-win-x64")"
	
	JAVA="$(/bin/ls -d1 /cygdrive/c/Program*/Java/*/bin/java | tail -1)"
	conf=config
	opts=""
else
	CLASSPATH="$ROOTDIR/bin:$ROOTDIR/resources:$ROOTDIR/lib/RXTXcomm.jar"
	JAVA=java
	conf=configLinux
	opts="-Dgnu.io.rxtx.SerialPorts=/dev/ttyACM0"
fi

"$JAVA" -classpath "$CLASSPATH" $opts "-Djava.library.path=$LIBPATH" \
	net.atos.aw.tum.IRServer \
	-config $conf.json "$@"
