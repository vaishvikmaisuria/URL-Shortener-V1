#!/bin/bash

declare -a arr=($@)
var=$(pwd)
DBPATH="~/../../virtual/$USER/"
## now loop through the above array
for i in "${arr[@]}"
do
    IFS=':' read -ra ITEM <<< "$i"
    # echo ${ITEM[2]}
    # echo ${ITEM[5]}
 
    var1="$(ssh ${ITEM[0]} "ps aux | grep 'db/DBServer ${ITEM[1]}' | grep -v 'grep'")"
    stringarray1=($var1)
    if [ -z "$var1" ]
    then
        echo "Database Server:${ITEM[1]} is not working on ${ITEM[0]}"
        var6= ssh ${ITEM[3]} "cd ${DBPATH} && sqlite3 ${ITEM[5]} 'PRAGMA integrity_check;'"
        original=$(ssh ${ITEM[1]} "cd ${DBPATH} &&  ls -l --b=K  ${ITEM[2]}")
        new=( $(ssh ${ITEM[1]} "cd ${DBPATH} && echo 'ls -l --b=K  ${ITEM[2]} | cut -d " " -f5'") )

        echo ${original}
        echo "Hell"
        echo ${new}
        
        # $(ssh ${ITEM[1]} "cd ${DBPATH} && echo 'ls -l --b=K  ${ITEM[2]} | cut -d " " -f5'")
        # $(ssh ${ITEM[3]} "cd ${DBPATH} && echo 'ls -l --b=K  ${ITEM[5]} | cut -d " " -f5'")
        
        ssh ${ITEM[3]} "cd ${DBPATH} && sqlite3 ${ITEM[5]} ".recover" | sqlite3 ${ITEM[2]}'"
        scp $2:"${DBPATH}${ITEM[2]}" ./../../virtual/$USER/
        ssh ${ITEM[0]} "cd ${var} && cd .. && nohup sh ./runDBServer.sh ${ITEM[1]} > foo.out 2> foo.err < /dev/null & "
    else
        if [[ ${stringarray1[0]} == $USER ]]
        then
            echo "Database Server:${ITEM[1]}(${stringarray1[1]}) is working on ${ITEM[0]}"
        fi
    fi
done
