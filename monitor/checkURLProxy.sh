#!/bin/bash
var1="$(ssh $1 "ps aux | grep proxy/url/ProxyServer | grep -v "grep"")"
stringarray1=($var1)
var= cd .. && pwd
if [ -z "$var1" ]
then     
    echo "ProxyServer is not working on $1"
    ssh $i "cd $var && java proxy/url/ProxyServer"
else
    if [[ ${stringarray1[0]} == $USER ]] 
    then
        echo "ProxyServer (${stringarray1[1]}) is working on $1"
    fi
fi