#!/usr/bin/python3

import random, string, subprocess
from subprocess import check_output
from subprocess import Popen, PIPE
import random
import time
import logging
import threading
import os
import datetime
import configparser


mainProxyHost =  "localhost"
mainProxyPort =  7000

shortList = []
# Letter range
for i in range(25):
	s = ""
	for j in range(4):
		s = s + chr(random.randint(97, 122))
	shortList.append(s)

def getMillSeconds(start, end):
	time_diff = (end - start)
	execution_time = time_diff.total_seconds() * 1000
	return execution_time
#  ^ ------- UTILITY FUNCTIONS ------




def singlePutRequestTest():
	print("\nSINGLE REQUEST TIME FOR PUT")
	print("\tAssuming this is a valid request and will go through")
	shortResource = "hello2"
	longResource = "world"
	request = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "?short="+ shortResource+"&long="+longResource
	p = Popen(['curl', '-X', "PUT", request], stdin=PIPE, stdout=PIPE, stderr=PIPE)

	start = datetime.datetime.now()
	output, err = p.communicate(b"input data that is passed to subprocess' stdin")
	end = datetime.datetime.now()

	logging.info("singlePutRequest Execution time: %d", getMillSeconds(start, end))
	print(output)



def singleGetRequestTest():
	print("\nSINGLE REQUEST TIME FOR GET")
	print("\tAssuming this is a valid request and will go through")

	shortResource = "hello"
	request = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + shortResource
	p = Popen(['curl', request], stdin=PIPE, stdout=PIPE, stderr=PIPE)

	start = datetime.datetime.now()
	output, err = p.communicate(b"input data that is passed to subprocess' stdin")
	end = datetime.datetime.now()

	logging.info("singleGetRequestTest Execution Time: %d", getMillSeconds(start, end))



def stressPutRequest():
	print("\nSTRESS REQUEST TIME FOR PUT SINGLE THEREAD: " + str(len(shortList)) + " REQUESTS" )
	print("\tAssuming these are valid request and will go through")

	start = datetime.datetime.now()
	for short in shortList:
		longResource = "world"
		url = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "?short=" + short + "&long=" + longResource
		request = ['curl', '-X', 'PUT', url, "/dev/null"]
		subprocess.run(request)

		# p = Popen(['curl', '-X', "PUT", request],  stdout=FNULL, stderr=PIPE)
		# output, err = p.communicate(b"input data that is passed to subprocess' stdin")
		# retcode = subprocess.call(['echo', 'foo'], stdout=FNULL, stderr=subprocess.STDOUT)

	end = datetime.datetime.now()

	logging.info("StressPutRequest Test Execution Time: %d", getMillSeconds(start, end))
	print(end - start)


def stressGetRequest():
	logging.info("\nSTRESS REQUEST TIME FOR PUT SINGLE THEREAD: " + str(len(shortList)) + " REQUESTS" )
	logging.info("\tAssuming these are valid request and will go through")

	start = datetime.datetime.now()
	for short in shortList:
		longResource = "world"
		url = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + short
		request = ['curl', url, "/dev/null"]
		subprocess.run(request)

		# p = Popen(['curl', '-X', "PUT", request],  stdout=FNULL, stderr=PIPE)
		# output, err = p.communicate(b"input data that is passed to subprocess' stdin")
		# retcode = subprocess.call(['echo', 'foo'], stdout=FNULL, stderr=subprocess.STDOUT)

	end = datetime.datetime.now()
	logging.info("Total time: %d", getMillSeconds(start, end))


# ------ Threaded Put Test -------------
def thread_function_Put(start, end):
	# start = args[0]
	# end = args[1]
	longResource = "world"
	i = start
	while i < end:
		url = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + "?short=" + shortList[int(i)] + "&long=" + longResource

		request = ['curl', '-X', 'PUT', url, "/dev/null"]
		subprocess.run(request)
		i += 1


def threadedPutTest(threads):
	format = "%(asctime)s: %(message)s"
	logging.basicConfig(format=format, level=logging.INFO,
						datefmt="%H:%M:%S")

	threadsList = []
	requestsPerThread = len(shortList)/ threads
	i = 0
	while i < len(shortList):
		start = i
		end = i + requestsPerThread
		print("start " + str(start) + " end " + str(end))
		t = threading.Thread(target=thread_function_Put, args=[start, end])
		threadsList.append(t)
		i += requestsPerThread

	start = datetime.datetime.now()
	for t in threadsList:
		t.start()


	for t in threadsList:
		t.join()

	end = datetime.datetime.now()

	logging.info("Total taken %d", getMillSeconds(start,end))


# threadedPutTest(5)


# ------ Threaded Get Test -------------
def thread_function_Get(start, end):
	# start = args[0]
	# end = args[1]
	longResource = "world"
	i = start
	while i < end:
		url = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + shortList[int(i)]

		request = ['curl', url]
		subprocess.run(request, stdout="/dev/null")
		i += 1


def threadedGetTest(threads):
	format = "%(asctime)s: %(message)s"
	logging.basicConfig(format=format, level=logging.INFO,
						datefmt="%H:%M:%S")

	def insertShortList(longR, index):
		url = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + "?short=" + shortList[int(index)] + "&long=" + longR
		request = ['curl', '-X', 'PUT', url]
		subprocess.run(request, stdout="/dev/null")


	threadsList = []
	requestsPerThread = len(shortList)/ threads
	i = 0
	while i < len(shortList):

		insertShortList("world", i)

		start = i
		end = i + requestsPerThread
		t = threading.Thread(target=thread_function_Get, args=[start, end])
		threadsList.append(t)
		i += requestsPerThread

	start = datetime.datetime.now()
	for t in threadsList:
		t.start()

	for t in threadsList:
		t.join()

	end = datetime.datetime.now()

	logging.info("Total taken %d", getMillSeconds(start, end))


# ------ Threaded Mixed Test ----------

allRequests = []


def buildGetandPutRequests():
	gets = []
	puts = []
	longResource = 'world'
	for short in shortList:
		get = ['curl', "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + shortList[int(i)] ]
		put = ['curl', '-X', 'PUT',
				"http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + "?short=" + shortList[int(i)] + "&long=" + longResource]

		gets.append(get)
		puts.append(put)

	return [gets, puts]


preReq = buildGetandPutRequests()
allRequests = preReq[0] + preReq[1]
random.shuffle(allRequests)


def threadMixTest(start, end):

	# def getShort(short):
	# 	url = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + short
	# 	request = ['curl', url]
	# 	subprocess.run(request, stdout="/dev/null")
	#
	# def insertShortList(longR, index):
	# 	url = "http://" + mainProxyHost + ":" + str(mainProxyPort) + "/" + "?short=" + allRequests[int(index)] + "&long=" + longR
	# 	request = ['curl', '-X', 'PUT', url]
	# 	subprocess.run(request, stdout="/dev/null")

	i = start
	while i < end:
		subprocess.run(allRequests[i], stdout="/dev/null")
		i += 1


def threadedMixTest(threads):
	format = "%(asctime)s: %(message)s"
	logging.basicConfig(format=format, level=logging.INFO,
						datefmt="%H:%M:%S")


	threadsList = []
	requestsPerThread = len(allRequests)/ threads
	i = 0
	while i < len(allRequests):

		start = i
		end = i + requestsPerThread
		t = threading.Thread(target=threadMixTest, args=[start, end])
		threadsList.append(t)
		i += requestsPerThread


	start = datetime.datetime.now()
	for t in threadsList:
		t.start()

	for t in threadsList:
		t.join()

	end = datetime.datetime.now()

	totalTime = getMillSeconds(start, end)
	througPut = (end - start) / (end - start).total_seconds()
	logging.info("throughPut: %f, time(MS): %d", start, end, througPut, totalTime)


# ------------------ Avalability/Faliure Tests ------------

def urlFailureTest():
	with open('../config.properties') as f:
		file_content = '[config]\n' + f.read()
	config = configparser.RawConfigParser()
	config.read_string(file_content)

	urlHosts = config.get('config', 'url.hostsAndPorts').split(',')
	urlProxyHost = config.get('config', 'proxy.url.host')
	urlProxyPort = config.get('config', 'proxy.url.port')

	request = ['curl', 'http://' + urlProxyHost + ":" + urlProxyPort + '/hello']
	subprocess.run(request)

	p = Popen(['echo', "$?"], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate(b"input data that is passed to subprocess' stdin")
	if output == 0:
		logging.info("First Request is successful")

	logging.info("Killing the second URLServer at %s", urlHosts[1])
	targetURLHost = urlHosts[1].split(':')[0]
	targetURLPort = urlHosts[1].split(':')[1]
	subprocess.run(['ssh', targetURLHost, 'fuser -k' + targetURLPort + '/tcp'])

	subprocess.run(request)

	p = Popen(['echo', "$?"], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate(b"input data that is passed to subprocess' stdin")
	if output == 0:
		logging.info("First Request is successful")




def dbFailureTest():
	with open('../config.properties') as f:
		file_content = '[config]\n' + f.read()
	config = configparser.RawConfigParser()
	config.read_string(file_content)

	urlProxyHost = config.get('config', 'proxy.url.host')
	urlProxyPort = config.get('config', 'proxy.url.port')
	dbhosts = config.get('config', 'db.hostsAndPorts').split(',')


	request = ['curl', 'http://' + urlProxyHost + ":" + urlProxyPort + '/hello']
	subprocess.run(request)

	p = Popen(['echo', "$?"], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate(b"input data that is passed to subprocess' stdin")
	if output == 0:
		logging.info("First Request is successful")

	logging.info("Killing the second DBServer at %s", dbhosts[1])

	targetDBHost = dbhosts[1].split(':')[0]
	targetDBPort = dbhosts[1].split(':')[1]
	subprocess.run(['ssh', targetDBHost, 'fuser -k' + targetDBPort + '/tcp'])

	logging.info("Running request again")
	subprocess.run(request)

	p = Popen(['echo', "$?"], stdin=PIPE, stdout=PIPE, stderr=PIPE)
	output, err = p.communicate(b"input data that is passed to subprocess' stdin")
	if output == 0:
		logging.info("First Request is successful")







