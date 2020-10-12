import configparser
import sys
from subprocess import Popen, PIPE
import subprocess
import os.path


with open('config.properties') as f:
		file_content = '[config]\n' + f.read()
config = configparser.RawConfigParser()
config.read_string(file_content)


dbServerRequest = "java -classpath .:sqlite3.jar db/DBServer "

if len(sys.argv) != 2:
    print("Enter a running DB node index to close")
    print("Example: if config contains this: db.hostsAndPorts=dh2020pc25:7003")
    print("\t and we want to shutdown the DBServer dh2020pc25:7003, then we will run")
    print("python3 takedownNode.py 0")
    exit(1)

class DBServer:
    def __init__(self, port,host):
        super().__init__()
        self.port = port
        self.host = host

def compile_java(java_file):
    print(java_file)
    subprocess.check_call(['javac', java_file])

def execute_java(java_file, serv):
    java_class, ext = os.path.splitext(java_file)
    cmd = ['java', "takeDownNode." + java_class, serv.host, serv.port]
    print(cmd)
    process = subprocess.Popen(cmd, stdin=PIPE, stdout=PIPE, stderr=PIPE, universal_newlines=True)
    stdout, stderr = process.communicate()
    print(stderr)
    print(stdout)

compile_java(os.path.join('takeDownNode', 'takeDownNodeJ.java'))

def getHostandPort(property):
    dbServer = config.get('config', property).split(',')[int(sys.argv[1])]
    dbHost = dbServer.split(':')[0]
    dbPort = dbServer.split(':')[1]

    return DBServer(dbPort, dbHost)

def combineValue(lst, dmt):
    val = ""
    for item in lst:
        val = val + item + dmt
    
    return val

   
def cleanUPFile():
    f = open('config.properties', 'r')
    lines = f.readlines()

    i = 0
    while i < len(lines):
        
        if '\\' in lines[i]:
            lines[i] = lines[i].replace('\\', '')
        i += 1

    f.close()
    f = open('config.properties', 'w')
    i = 0
    while i < len(lines):
        f.write(lines[i])
        i += 1
    f.close()
    
    

# Check if the mainDB is UP
server = getHostandPort('db.hostsAndPorts')


request = "ps aux | grep 'db/DBServer " + server.port + "' | grep -v 'grep'"
p = Popen(['ssh',server.host, request], stdin=PIPE, stdout=PIPE, stderr=PIPE)

output, err = p.communicate(b"input data that is passed to subprocess' stdin")
output = output.decode("utf-8")

if ((dbServerRequest + server.port) in output ):
    # Main server is running
    compile_java(os.path.join('takeDownNode', 'takeDownNodeJ.java'))
    execute_java('takeDownNodeJ', server)
    cleanUPFile()
    
    # deleteNode(config, int(sys.argv[1]))
else:
    # Main server is not running
    server = getHostandPort('db.backupHostsAndPorts')

    request = "ps aux | grep 'db/DBServer " + server.port + " true' | grep -v 'grep'"
    p = Popen(['ssh',server.host, request], stdin=PIPE, stdout=PIPE, stderr=PIPE)
    
    output, err = p.communicate(b"input data that is passed to subprocess' stdin")
    output = output.decode("utf-8")
    if (dbServerRequest + server.port + " true" ) in output:
        compile_java(os.path.join('takeDownNode', 'takeDownNodeJ.java'))
        execute_java('takeDownNodeJ', server)
        cleanUPFile()
    else:
        print("Both servers are Down")






