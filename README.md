# System Configuration
# For each Database Server create the corresponding database file
# and add its location in the config.properties file.

# When spinning up a backup Database server, run it like this:
#   java -classpath ".:sqlite3.jar" db/DBServer 7004 true
# otherwise
#   java -classpath ".:sqlite3.jar" db/DBServer 7004


# IMPORTANT
# Replaces the host names in the entire config.properties file with the corrseponding hostnames
# if you are are running the server on the pc your are currently logged into, use the following
# terminal command to find out the current hostname: 'hostname'


# Usage 

# terminal 1  (Proxy Server)
# java proxy/url/ProxyServer
# terminal 2 (URLShortner Server)
# java urlShortener/URLShortener 7001
# terminal 3 (Database Proxy Server)
# java proxy/db/DBProxyServer
# terminal 4 (Database Server 1)
# java -classpath ".:sqlite3.jar" db/DBServer 7003
# terminal 5 (Backup database server for databaseServer 1)
# java -classpath ".:sqlite3.jar" db/DBServer 7004 true

# Request to System
# PUT 
# curl -X PUT 'http://localhost:7000/?short=hello&long=world'
# GET
# curl 'http://localhost:7000/hello'

# To check Database Values
# sqlite3 Storage.db
# > select * from URLShorteners ;
