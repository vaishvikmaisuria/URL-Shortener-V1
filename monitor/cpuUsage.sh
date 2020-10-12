#!/bin/bash

# echo $1
# echo $2
var1="$(ssh $1 "ps aux | grep '$2' | grep -v "grep"")"

stringarray1=($var1)
if [[ ${stringarray1[0]} == $USER ]]
then
  var2="$(ssh $1 "ps -p ${stringarray1[1]} -o %cpu,%mem")"
  echo $var2
fi