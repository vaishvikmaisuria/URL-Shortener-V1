package proxy.db;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import db.DBConstants.Response;

public class DBThread extends Thread {
    private Socket client;
    private DBHostHandler hostHandler;
    private DBRepartitionHandler repartitionHandler;

    public DBThread(Socket socket, DBHostHandler hostHandler, DBRepartitionHandler repartitionHandler) {
        this.client = socket;
        this.hostHandler = hostHandler;
        this.repartitionHandler = repartitionHandler;
    }

    public void run() {
        Socket dbServer = null;
        DataInputStream streamFromClient = null;
        int requestType = -1;

        try {
            streamFromClient = new DataInputStream(this.client.getInputStream());
            String input = streamFromClient.readUTF();

            Pattern prepartitionput = Pattern.compile("^PUT\\s+/repartition/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mrepartitionput = prepartitionput.matcher(input);

            if (mrepartitionput.matches()) {
                partitionPutRequest(input, streamFromClient);
                streamFromClient.close();
                this.client.close();
                return;
            }

            Pattern pdone = Pattern.compile("^DONE$");
            Matcher mdone = pdone.matcher(input);

            if (repartitionHandler.getRepartitionFlag() && !mdone.matches()) {
                // SEND BACK INTERNAL SERVICE ERROR OR SOMETHING
                System.out.println("Sending request while repartitioning. Exiting.");
                streamFromClient.close();
                this.client.close();
                return;
            }

            if (mdone.matches()) {
                this.repartitionHandler.incrementRepartitionsDone();
                this.repartitionHandler.isRepartitionsDone();
                streamFromClient.close();
                this.client.close();
                return;
            }

            Pattern pput = Pattern.compile("^PUT\\s+\\/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mput = pput.matcher(input);

            Pattern pget = Pattern.compile("^GET\\s+\\/(\\S+)\\s+(\\S+)$");
            Matcher mget = pget.matcher(input);

            String shortResource = null;

            // Matches either PUT or GET request
            if (mput.matches()) {
                shortResource = mput.group(1);
                requestType = 1;
            } else if (mget.matches()) {
                shortResource = mget.group(1);
                requestType = 0;
            } else {
                return;
            }

            String output = "";
            if (requestType == 1){
                output = putRequest(shortResource, input);
            } else {
                output = getRequest(shortResource, input);
            }

            DataOutputStream streamToClient = new DataOutputStream(this.client.getOutputStream());
            streamToClient.writeUTF(output);

            System.out.println("DB Response: " + output);
            streamToClient.close();
            streamFromClient.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            try {
                if (dbServer != null && this.client != null) {
                    this.client.close();
                    dbServer.close();
                }
            } catch (Exception e){
                System.err.println(e);
            }
        }
    }

    private String getRequest(String shortResource, String input) throws Exception {
        Socket dbServer = null;
        String output = "";
        String backupHost = this.hostHandler.getBackupHost(shortResource);
        int backupPort = this.hostHandler.getBackupPort(shortResource);
        String host = this.hostHandler.getHost(shortResource);
        int port = this.hostHandler.getPort(shortResource);
        
        try {
            // Sending request to MasterDB
            try {
                // Send request to the Master dbServer
                dbServer = new Socket(host, port);
            } catch (Exception e) {
                // Try connecting to the backup DB
                try {
                    System.out.println("Could not connect to " + host + ":" + port);
                    System.out.println("Using backup host " + backupHost + ":" + backupPort + " for short=" + shortResource);
                    dbServer = new Socket(backupHost, backupPort);
                } catch (IOException err) {
                    System.out.println("DB proxy server cannot connect to " + backupPort + ":" + backupPort + ":\n" + err + "\n");
                }
            }
            // Send the request to a DB
            DataOutputStream streamToServer = new DataOutputStream(dbServer.getOutputStream());
            streamToServer.writeUTF(input);
            
            // Read the DB's response
            DataInputStream streamFromServer = new DataInputStream(dbServer.getInputStream());
            try {
                output = streamFromServer.readUTF();
            } catch (EOFException e) {}
            
            streamToServer.close();
            streamFromServer.close();
        
        } catch (Exception e) {
            System.out.println(host + ":" + port + "=DOWN " + backupHost + ':' + backupPort + "=DOWN");
            output = Response.BAD.toString();
            System.out.println(e);
        } finally {
            if (dbServer != null){
                dbServer.close();
            }
        }

        return output;
    }

    private String putRequest(String shortResource, String input) throws Exception {
        Socket dbServer = null;
        String output = "";
        String backupHost = this.hostHandler.getBackupHost(shortResource);
        int backupPort = this.hostHandler.getBackupPort(shortResource);
        String host = this.hostHandler.getHost(shortResource);
        int port = this.hostHandler.getPort(shortResource);

        try {
            // Sending request to MasterDB and BackUPDB
            dbServer = new Socket(host, port);
            // Send the request to a DB
            DataOutputStream streamToServer = new DataOutputStream(dbServer.getOutputStream());
            streamToServer.writeUTF(input);

            // Read the DB's response
            DataInputStream streamFromServer = new DataInputStream(dbServer.getInputStream());
            try {
                output = streamFromServer.readUTF();
            } catch (EOFException e) {}

            streamToServer.close();
            streamFromServer.close();
            dbServer.close();

            // Also send the request to the backup db
            dbServer = new Socket(backupHost, backupPort);
            streamToServer = new DataOutputStream(dbServer.getOutputStream());
            streamToServer.writeUTF(input);

            streamToServer.close();
            dbServer.close();

        } catch (Exception e) {

            // Master DB is down, So connect to the backUP db
            try {
                System.out.println("DB at " + host + ":" + port + " is down. Contacting backDP" );
                dbServer = new Socket(backupHost, backupPort);
                // Send the request to a DB
                DataOutputStream streamToServer = new DataOutputStream(dbServer.getOutputStream());
                streamToServer.writeUTF(input);
                // Read the DB's response
                DataInputStream streamFromServer = new DataInputStream(dbServer.getInputStream());
                try {
                    output = streamFromServer.readUTF();
                } catch (EOFException f) {}

                streamToServer.close();

            } catch (Exception g) {
                System.out.println("BackDB is also down");
                output = Response.BAD.toString();
                //TODO: handle exception
            } finally {
                if (dbServer != null){
                    dbServer.close();
                }
            }
        } finally {
            if (dbServer != null){
                dbServer.close();
            }
        }

        return output;
    }

    private void partitionPutRequest(String input, DataInputStream streamFromClient) throws Exception {
        ArrayList<String> shortResources = new ArrayList<String>();
        ArrayList<String> putRequests = new ArrayList<String>();

        while (!input.equals("DONE")) {
            Pattern prepartitionput = Pattern.compile("^PUT\\s+/repartition/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mrepartitionput = prepartitionput.matcher(input);

            if (!mrepartitionput.matches()) {
                break;
            }

            String shortResource = mrepartitionput.group(1);
            String longResource = mrepartitionput.group(2);
            String putRequest = String.format("PUT /?short=%s&long=%s HTML/1.1", shortResource, longResource);

            shortResources.add(shortResource);
            putRequests.add(putRequest);

            input = streamFromClient.readUTF();
        }

        this.repartitionHandler.incrementRepartitionsDone();
        this.repartitionHandler.isRepartitionsDone();

        // Wait until all hosts send their info (gets rid of race condition)
        while (this.repartitionHandler.getRepartitionFlag());

        for (int i = 0; i < putRequests.size(); i++) {
            String shortResource = shortResources.get(i);
            String putRequest = putRequests.get(i);

            Socket dbServer = null;
            String backupHost = this.hostHandler.getBackupHost(shortResource);
            int backupPort = this.hostHandler.getBackupPort(shortResource);
            String host = this.hostHandler.getHost(shortResource);
            int port = this.hostHandler.getPort(shortResource);

            try {
                // Sending request to MasterDB and BackUPDB
                dbServer = new Socket(host, port);
                // Send the request to a DB
                DataOutputStream streamToServer = new DataOutputStream(dbServer.getOutputStream());
                streamToServer.writeUTF(putRequest);

                streamToServer.close();
                dbServer.close();

                // Also send the request to the backup db
                dbServer = new Socket(backupHost, backupPort);
                streamToServer = new DataOutputStream(dbServer.getOutputStream());
                streamToServer.writeUTF(putRequest);

                streamToServer.close();
                dbServer.close();

            } catch (Exception e) {
                // Master DB is down, So connect to the backUP db
                try {
                    System.out.println("DB at " + host + ":" + port + " is down. Contacting backDP" );
                    dbServer = new Socket(backupHost, backupPort);
                    // Send the request to a DB
                    DataOutputStream streamToServer = new DataOutputStream(dbServer.getOutputStream());
                    streamToServer.writeUTF(putRequest);
                    streamToServer.close();

                } catch (Exception g) {
                    System.out.println("BackDB is also down");
                    //TODO: handle exception
                } finally {
                    if (dbServer != null){
                        dbServer.close();
                    }
                }
            } finally {
                if (dbServer != null){
                    dbServer.close();
                }
            }
        }
    }
}
