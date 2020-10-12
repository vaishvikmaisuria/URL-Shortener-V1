
import time
import subprocess
from subprocess import Popen, PIPE
import datetime
import logging
import configparser
import os
import threading


URLProxy = None
DBProxy = None
URLServers = None
DBServers = None
backupDBServers = None
cpuUsages = None

format = "%(asctime)s: %(message)s"
logging.basicConfig(format=format, level=logging.INFO, datefmt="%H:%M:%S")

with open('../config.properties') as f:
	file_content = '[config]\n' + f.read()
config = configparser.RawConfigParser()
config.read_string(file_content)

def clear():
    os.system( 'clear' )


def getCpuOutputs(hostLst, name):
    '''
    getCpuOutputs goes through each of the hosts in hostLst, and gets the
    cpu and memory usage of the given program (name)
    :param hostLst: list of hosts
    :param name: name of the program that is running on each host
    :return: list
    '''

    cpuUsage = []
   
    request = ['bash', 'cpuUsage.sh', hostLst, name]
    p = Popen(request, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    output = output.decode("utf-8")

    cpuUsage.append({name+ ":" + hostLst : output })

    return cpuUsage


def urlProxy(request):
    '''
    urlProxy gets the status of the urlProxy prescribed in th config
    :param request: list
    :return: None
    '''
    global URLProxy
    p = Popen(request, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    URLProxy = output


def dbProxy(request):
    '''
    dbProxy gets the status of the dbProxy prescribed in th config

    :param request: list
    :return: None
    '''
    global DBProxy
    p = Popen(request, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    DBProxy, err = p.communicate(b"input data that is passed to subprocess' stdin")


def urlServers(request):
    '''
    urlServers gets the status of the urlServers prescribed in th config

    :param request: list
    :return: None
    '''
    global  URLServers
    p = Popen(request, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    URLServers, err = p.communicate(b"input data that is passed to subprocess' stdin")


def dbServers(request):
    '''
    urlServers gets the status of the urlServers prescribed in th config

    :param request: list
    :return: None
    '''
    global  DBServers
    p = Popen(request, stdin=PIPE, stdout=PIPE, stderr=PIPE)   
    DBServers, err = p.communicate(b"input data that is passed to subprocess' stdin")


def backupDbServers(request):
    '''
    urlServers gets the status of the urlServers prescribed in th config

    :param request: list
    :return: None
    '''
    global backupDBServers
    p = Popen(request, stdin=PIPE, stdout=PIPE, stderr=PIPE)
    backupDBServers, err = p.communicate(b"input data that is passed to subprocess' stdin")

def cpuUsage(allHosts):
    '''
    cpuUsage gets the status of the cpuUsage prescribed in the config

    :param request: list
    :return: None
    '''
    global cpuUsages
    cpuUsages = []
    for item in allHosts:
        host = []
        namePort = []
        if ":" in list(item.values())[0][0]:
            for i in (list(item.values())[0]):
                # print(i)
                splitHost = i.split(':')
                host.append(splitHost[0])
                namePort.append(list(item.keys())[0] + " " +splitHost[1])
        else:
            host = list(item.values())[0]
            namePort = [list(item.keys())[0]]

        for index, value in enumerate(host):
            usage = getCpuOutputs(value, namePort[index])
            cpuUsages.append(usage)


if __name__ == '__main__':
    # Read in config file
    with open('../config.properties') as f:
        file_content = '[config]\n' + f.read()
    config = configparser.RawConfigParser()
    config.read_string(file_content)

    while (1):

        threads = []
        # URL Proxy
        urlProxyHost = config.get('config', 'proxy.url.host')
        request = ['bash', 'checkURLProxy.sh', urlProxyHost]
        t = threading.Thread(target=urlProxy, args=[request])
        threads.append(t)

        # DB Proxy
        DBProxyHost = config.get('config', 'proxy.db.host')
        request = ['bash', 'checkDBProxy.sh', DBProxyHost]
        t = threading.Thread(target=dbProxy, args=[request])
        threads.append(t)

        # URL Servers
        URLhostsAndPorts = config.get('config', 'url.hostsAndPorts').split(',')
        request = ['bash', 'checkURLshortner.sh'] + URLhostsAndPorts
        t = threading.Thread(target=urlServers, args=[request])
        threads.append(t)

        # DB Servers
        DBhostsAndPorts = config.get('config', 'db.hostsAndPorts').split(',')
        DBName = config.get('config', 'db.masterDBPaths').split(',')
        backDB = config.get('config', 'db.backupHostsAndPorts').split(',')
        backDBName = config.get('config', 'db.backUPPaths').split(',')
        final = []
        for index, value in enumerate(DBhostsAndPorts):
            final.append(value + ":" + DBName[index]+ ":" +backDB[index]+ ":"+ backDBName[index])
        
        request = ['bash', 'checkDatabaseServer.sh'] + final
        

        t = threading.Thread(target=dbServers, args=[request])
        threads.append(t)

        # DB backup Servers
        finalBackup = []
        for index, value in enumerate(backDB):
            finalBackup.append(value + ":" + backDBName[index] + ":" +DBhostsAndPorts[index]+ ":"+  DBName[index])

        request = ['bash', 'checkBackUPDatabaseServer.sh'] + finalBackup
        t = threading.Thread(target=backupDbServers, args=[request])
        threads.append(t)


        allHosts = []
        allHosts.append({ "db/DBServer" : DBhostsAndPorts})
        allHosts.append({ "urlShortener/URLShortener" : URLhostsAndPorts})
        allHosts.append({"proxy/db/DBProxyServer" : [DBProxyHost]})
        allHosts.append({"proxy/url/ProxyServer" : [urlProxyHost]})
        t = threading.Thread(target=cpuUsage, args=[allHosts])
        threads.append(t)

        for thread in threads:
            thread.start()

        for thread in threads:
            thread.join()

        # Printing Results
        clear()

        current = datetime.datetime.now()
        print(str(current) + " URLProxy Status: " + str(URLProxy.decode("utf-8")))
        print(str(current) + " DBProxy Status: " + str(DBProxy.decode("utf-8")))
        print(str(current) + " URL Servers Status:\n" + str(URLServers.decode("utf-8")))
        print(str(current) + " DB Servers Status:\n" + str(DBServers.decode("utf-8")))
        print(str(current) + " BackUP DB Servers Status:\n" + str(backupDBServers.decode("utf-8")))

        print(str(current) + " CPU USAGES\n")
        for usage in cpuUsages:
            print(str(current) + str(usage) + "\n")

        time.sleep(1)


