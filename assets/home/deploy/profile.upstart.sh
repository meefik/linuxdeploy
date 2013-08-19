export UPSTART_SESSION=unix:abstract=/com/ubuntu/upstart-session/0/$(
    cat /var/run/init.pid)
export PREVLEVEL=N
export RUNLEVEL=2
