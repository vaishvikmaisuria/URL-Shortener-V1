#!/bin/bash

file=config.properties
URLPORT=()
DBPORT=()
BACKUOPORT=()
URLHOST=()
DBHOST=()
BACKUOHOST=()
DBPATH=()
BACKUOPATH=()

path=$(pwd)
DATABASELOC="~/../../virtual/$USER/"

if [ -f "$file" ]
then
    echo "$file found."

    while IFS='=' read -r key value
    do
    key=$(echo $key | tr '.' '_')
    eval ${key}=\${value} 2>/dev/null
    done < "$file"

else
  echo "$file not found."
fi

IFS=',' read -ra ITEM <<< "${url_hostsAndPorts}"
for t in ${ITEM[@]}; do
    IFS=':' read -ra PORT <<< "$t"
    URLPORT+=( ${PORT[1]} )
    URLHOST+=( ${PORT[0]} )
done

IFS=',' read -ra ITEM <<< "${db_hostsAndPorts}"
for t in ${ITEM[@]}; do
    IFS=':' read -ra PORT <<< "$t"
    DBHOST+=( ${PORT[0]} )
    DBPORT+=( ${PORT[1]} )
done

IFS=',' read -ra ITEM <<< "${db_backupHostsAndPorts}"
for t in ${ITEM[@]}; do
    IFS=':' read -ra PORT <<< "$t"
    BACKUOHOST+=( ${PORT[0]} )
    BACKUOPORT+=( ${PORT[1]} )
done

IFS=',' read -ra ITEM <<< "${db_masterDBPaths}"
for t in ${ITEM[@]}; do
    DBPATH+=( $t )
done
IFS=',' read -ra ITEM <<< "${db_backUPPaths}"
for t in ${ITEM[@]}; do
    BACKUOPATH+=( $t )
done

if [ $1 == "start" ]
then
    if [ -z "$2" ]
    then 
        echo "starting compiling .."
        javac cache/Cache.java

        javac db/DBConstants.java
        javac db/DBServer.java
        javac db/DBThread.java

        javac proxy/ProxyConstants.java

        javac proxy/db/DBHostHandler.java
        javac proxy/db/DBThread.java
        javac proxy/db/DBProxyServer.java
        javac proxy/db/DBRepartitionHandler.java

        javac proxy/url/ProxyHostHandler.java
        javac proxy/url/ProxyThread.java
        javac proxy/url/ProxyServer.java
        

        javac takeDownNode/takeDownNodeJ.java

        javac urlShortener/URLShortenerThread.java
        javac urlShortener/URLShortener.java

        
        echo "starting to running ..."
        
        for i in "${!DBHOST[@]}"; do 
            ssh ${DBHOST[$i]} "[ ! -d ${DATABASELOC} ] && mkdir ${DATABASELOC}"
            ssh ${BACKUOHOST[$i]} "[ ! -d ${DATABASELOC} ] && mkdir ${DATABASELOC}"
            ssh ${DBHOST[$i]} "sqlite3 ${DATABASELOC}${DBPATH[$i]} < ${path}/db/db.sql"
            ssh ${DBHOST[$i]} "chmod 777  ${DATABASELOC}${DBPATH[$i]}"
            ssh ${BACKUOHOST[$i]} "sqlite3 ${DATABASELOC}${BACKUOPATH[$i]} < ${path}/db/db.sql"
            ssh ${BACKUOHOST[$i]} "chmod 777 ${DATABASELOC}${BACKUOPATH[$i]}"
        done
        
        ssh ${proxy_db_host} "cd $path && java proxy/db/DBProxyServer &" &

        for i in "${!URLPORT[@]}"; do 
            ssh ${URLHOST[$i]} "cd $path && java urlShortener/URLShortener ${URLPORT[$i]} &" &
        done
        for i in "${!DBPORT[@]}"; do 
            ssh ${DBHOST[$i]} "cd $path && java -classpath '.:sqlite3.jar' db/DBServer ${DBPORT[$i]} &" &
        done
        for i in "${!BACKUOPORT[@]}"; do
            ssh ${BACKUOHOST[$i]} "cd $path && java -classpath '.:sqlite3.jar' db/DBServer ${BACKUOPORT[$i]} 'true' &" &
        done
        
        ssh ${proxy_url_host} "cd $path && java proxy/url/ProxyServer"

        echo "Start Complete"

    else
        if [ $2 == "URLShortener" ]
        then 
            for i in "${!URLPORT[@]}"; do 
                ssh ${URLHOST[$i]} "cd $path && java urlShortener/URLShortener ${URLPORT[$i]} &" &
            done
        elif [ $2 == "DBProxyServer" ]
        then 
            ssh ${proxy_db_host} "cd $path && java proxy/db/DBProxyServer &" &
        elif [ $2 == "DBServer" ]
        then 
            for i in "${!DBPORT[@]}"; do 
                ssh ${DBHOST[$i]} "cd $path && java -classpath '.:sqlite3.jar' db/DBServer ${DBPORT[$i]} &" &
            done
        elif [ $2 == "BackupDBServer" ]
        then 
            for i in "${!BACKUOPORT[@]}"; do
                ssh ${BACKUOHOST[$i]} "cd $path && java -classpath '.:sqlite3.jar' db/DBServer ${BACKUOPORT[$i]} 'true' &" &
            done
        elif [ $2 == "ProxyServer" ]
        then 
            ssh ${proxy_url_host} "cd $path && java proxy/url/ProxyServer"
        fi
    fi
elif [ $1 == "stop" ]
then
    search_terms='java'
    search_term2='python3'
    IFS=',' read -ra ITEM <<< "${all_pc}"
    for pc in ${ITEM[@]}; do
        ssh ${pc} "kill \$(ps -aux | grep ${search_terms} | awk '{print \$2}')"
	ssh ${pc} "kill \$(ps -aux | grep ${search_term2} | awk '{print \$2}')"
    done

elif [ $1 == "ProxyServer" ]
then 
    if [ -z "$2" ]
    then 
        search_terms='ProxyServer' 
        kill $(ps aux | grep "$search_terms" | grep -v 'grep' | awk '{print $2}')
    else
        ssh $2 "kill $(ps | grep 'ProxyServer' | grep -v 'grep' | awk '{print $2}')"
    fi
    echo "done"
elif [ $1 == "URLShortener" ]
then 
    if [ -z "$2" ]
    then 
        search_terms='urlShortener/URLShortener' 
        kill $(ps aux | grep "$search_terms" | grep -v 'grep' | awk '{print $2}')
    else
        ssh $2 "kill $(ps | grep 'urlShortener/URLShortener' | grep -v 'grep' | awk '{print $2}')"
    fi
    echo "done"
elif [ $1 == "DBProxyServer" ]
then
    if [ -z "$2" ]
    then 
        search_terms='proxy/db/DBProxyServer' 
        kill $(ps aux | grep "$search_terms" | grep -v 'grep' | awk '{print $2}')
    else
        ssh $2 "kill $(ps | grep 'proxy/db/DBProxyServer' | grep -v 'grep' | awk '{print $2}')"
    fi
    echo "done"
elif [ $1 == "DBServer" ]
then 
    if [ -z "$2" ]
    then 
        echo "here"
        search_terms='db/DBServer' 
        kill $(ps aux | grep "$search_terms" | grep -v 'grep' | awk '{print $2}')
    else
        ssh $2 "kill $(ps | grep 'db/DBServer' | grep -v 'grep' | awk '{print $2}')"
    fi
    echo "done"
elif [ $1 == "DeleteDB" ]
then 
    find . -type f -name "*.db" -delete

elif [ $1 == "help" ]
then 
    echo "USAGE: 'bash startup.sh start' to run everything"
    echo "USAGE: 'bash startup.sh start DBProxyServer' to run DBProxyServer"
    echo "USAGE: 'bash startup.sh start DBServer PORT' to run DBServer"
    echo "USAGE: 'bash startup.sh start URLShortener' to run URLShortener"
    echo "USAGE: 'bash startup.sh start ProxyServer' to run ProxyServer"
    echo "USAGE: 'bash startup.sh stop' to stops everything"
    echo "USAGE: 'bash startup.sh clean' to delete all .class files"
    echo "USAGE: 'bash startup.sh help' to list all arguments"
    echo "USAGE: 'bash startup.sh ProxyServer location' to close ProxyServer"
    echo "USAGE: 'bash startup.sh ProxyServer location' to close URLShortener"
    echo "USAGE: 'bash startup.sh ProxyServer locaiton' to close DBProxyServer"
    echo "USAGE: 'bash startup.sh ProxyServer location' to close DBServer "
    echo "USAGE: 'bash startup.sh DeleteDB location' to close DBServer "
elif [ $1 == "clean" ]
then 
    find . -type f -name "*.class" -delete
    find . -type f -name "*.db" -delete
else
    echo "USAGE: bash startup.sh help"
fi




# Other Useful Things 

# Usage 

# terminal 1 
# java proxy/url/ProxyServer
# terminal 2
# java urlShortener/URLShortener 7001
# terminal 3
# java proxy/db/DBProxyServer
# terminal 4
# java -classpath ".:sqlite3.jar" db/DBServer 7003
# terminal 5
# java -classpath ".:sqlite3.jar" db/DBServer 7004

# Request to System
# PUT 
# curl -X PUT 'http://localhost:7000/?short=hello&long=world'
# GET
# curl 'http://localhost:7000/hello'

# To check Database Values
# sqlite3 Storage.db
# > select * from URLShorteners ;
