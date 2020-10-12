#!/bin/bash

## declare an array variable
declare -a arr=($@)
var= cd .. && pwd
## now loop through the above array
for i in "${arr[@]}"
do
    IFS=':' read -ra ITEM <<< "$i"

    var1="$(ssh ${ITEM[0]} "ps aux | grep 'urlShortener/URLShortener ${ITEM[1]}' | grep -v 'grep'")"
    stringarray1=($var1)

    if [ -z "$var1" ]
    then
        echo "URLshortner:${ITEM[1]} is not working on ${ITEM[0]}"
        ssh ${ITEM[0]} "cd $var && java urlShortener/URLShortener ${ITEM[1]}"
    else
        if [[ ${stringarray1[0]} == $USER ]] 
        then
             echo "URLshortner:${ITEM[1]}(${stringarray1[1]}) is working on ${ITEM[0]}"
        fi
    fi    
done