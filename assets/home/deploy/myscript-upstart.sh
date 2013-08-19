#! /bin/bash

OPTS=--user

if [ "$TRACE_MODE" = "y" ]
then
    set -x
fi

. /lib/lsb/init-functions

ACTION="$1"
case "$ACTION" in
  start)
	log_daemon_msg "Starting Upstart daemon in user mode" "init" || true
	if start-stop-daemon --start --background \
            --pidfile /var/run/init.pid --make-pidfile \
            --exec /sbin/init -- $OPTS; then
	    log_end_msg 0 || true
	else
	    log_end_msg 1 || true
	fi
        sleep 1

        . /etc/profile.upstart
	log_daemon_msg "Entering runlevel $RUNLEVEL" "telinit" || true
        initctl emit runlevel RUNLEVEL=$RUNLEVEL
        ;;
  stop)
        . /etc/profile.upstart
        export PREVLEVEL=$RUNLEVEL
        export RUNLEVEL=0
	log_daemon_msg "Entering runlevel $RUNLEVEL" "telinit" || true
        initctl emit runlevel RUNLEVEL=$RUNLEVEL

	log_daemon_msg "Stopping user mode Upstart daemon" "init" || true
	if start-stop-daemon --stop -R 10 --pidfile /var/run/init.pid; then
	    log_end_msg 0 || true
	else
	    log_end_msg 1 || true
	fi
        ;;
esac
