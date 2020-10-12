#!/bin/bash

nohup java -classpath '.:sqlite3.jar' db/DBServer $1 > foo.out 2> foo.err < /dev/null & 