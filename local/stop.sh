#!/bin/sh
set -x

PIDFILE="apprunner.pid"

if [ -s "$PIDFILE" ]; then
	kill -9 `cat $PIDFILE`
	rm -f $PIDFILE
	echo Stopped
else
    echo There is no pid file so there is nothing to stop.
    exit 1
fi

