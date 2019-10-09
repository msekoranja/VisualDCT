#!/bin/sh
# VisualDCT editor run script

if [ -z "$VDCT_HOME" ]; then
    VDCT_HOME=`dirname \`readlink -e \\\`which $0\\\`\``
fi

echo $CLASSPATH | grep "VisualDCT\.jar" > /dev/null
if (( $? == 1 )); then
    if [ -f "$VDCT_HOME/VisualDCT.jar" ]; then
        CLASSPATH="${CLASSPATH:+${CLASSPATH}:}$VDCT_HOME/VisualDCT.jar"
    else
        echo "VisualDCT.jar not found in $VDCT_HOME."
        exit 1
    fi
fi

java -classpath "$CLASSPATH" -DEPICS_DB_INCLUDE_PATH="$EPICS_DB_INCLUDE_PATH" com.cosylab.vdct.VisualDCT "$@"
