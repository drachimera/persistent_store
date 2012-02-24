#!/bin/bash

#CP=$( echo `dirname $0`/../lib/*.jar . | sed 's/ /:/g')
#CP=$CP:$( echo `dirname $0`/../ext/*.jar . | sed 's/ /:/g')

CP=$( echo `dirname $0`/../target/psrest-*-standalone/lib/*.jar . | sed 's/ /:/g')
CP=$CP:$( echo `dirname $0`/../target/psrest-*-standalone/ext/*.jar . | sed 's/ /:/g')
CP=$CP:$( echo `dirname $0`/target/psrest-*-standalone/lib/*.jar . | sed 's/ /:/g')
CP=$CP:$( echo `dirname $0`/target/psrest-*-standalone/ext/*.jar . | sed 's/ /:/g')
echo $CP

# Find Java
if [ "$JAVA_HOME" = "" ] ; then
    JAVA="java -server"
else
    JAVA="$JAVA_HOME/bin/java -server"
fi

# Set Java options
if [ "$JAVA_OPTIONS" = "" ] ; then
    JAVA_OPTIONS="-Xms32M -Xmx2000M"
fi

# Launch the application
$JAVA $JAVA_OPTIONS -cp $CP us.kbase.psrest.WebServer $@

# Return the program's exit code
exit $?
