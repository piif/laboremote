#!/bin/bash

HERE=$(cd $(dirname $0) ; /bin/pwd)
ROOTDIR=$(dirname $HERE)

if expr $(uname) : CYGWIN > /dev/null ; then
	JAVA="$(/bin/ls -d1 /cygdrive/c/Program*/Java/*/bin/java | tail -1)"
else
	JAVA=java
fi

"$JAVA" -classpath $ROOTDIR/bin net.atos.aw.tum.utils.CalcConvert "$@"
