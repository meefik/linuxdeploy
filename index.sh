#!/bin/bash

find ./config -type f -name "*.conf" | sort | while read path
do
    unset DESC
    . "${path}"
    PROFILE=$(basename "${path}" '.conf')
    SIZE=$(stat -c '%s' "./rootfs/${PROFILE}.tgz" 2>/dev/null || printf 0)
    let SIZE=SIZE/1024/1024
    echo "PROFILE=${PROFILE}"
    echo "DESC=${DESC}"
    echo "SIZE=${SIZE}"
    echo
done | gzip >index.gz

