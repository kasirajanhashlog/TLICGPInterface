#!/bin/sh
prg="TLICGPInterface.jar"
rm pid.out
ps -ef | grep "$prg" | grep -v grep |awk '{print $2}' > pid.out


file_name=tdump.txt
current_time=$(date "+%Y.%m.%d-%H.%M.%S")
echo "Current Time : $current_time"
new_fileName=$file_name.$current_time
echo "New FileName: " "$new_fileName"
jstack -l `cat  pid.out` >> "$new_fileName"

kill -9 `cat  pid.out`
