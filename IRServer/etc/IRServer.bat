set CP=..\bin;..\resources;..\lib\RXTXcomm.jar
set LIB=..\lib\ch-rxtx-2.2-20081207-win-x86
set JAVA="c:\Program Files (x86)\Java\jre7\bin\java"
%JAVA% -classpath %CP% -Djava.library.path=%LIB% net.atos.aw.tum.IRServer %1
