#!/bin/bash

var1="$(ssh $1 "ps aux | grep proxy/db/DBProxyServer | grep -v "grep"")"
stringarray1=($var1)
var= cd .. && pwd
if [ -z "$var1" ]
then     
    echo "DBProxyServer is not working on $1"
    ssh $i "cd $var && java proxy/db/DBProxyServer"
else
    if [[ ${stringarray1[0]} == $USER ]] 
    then
        echo "DBProxyServer (${stringarray1[1]}) is working on $1"
    fi
fi
