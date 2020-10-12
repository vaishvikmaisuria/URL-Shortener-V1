package takeDownNode;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;


import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;

// USAGE: java -classpath ".:sqlite3.jar" db/DBServer 7003
// USAGE: java -classpath ".:sqlite3.jar" db/DBServer 7003 true 

public class takeDownNodeJ {
    public static void main(String[] args) throws IOException {

        int port = 0;

        if (args.length != 2) {
            
            System.err.println("USAGE: java db.DBServer PORT BACKUPFlag(true/false) # where the BACKUPFlag is optional");
            System.err.println("Example for a Master Database: java db.DBServer 8000");
            System.err.println("Example for a BackUP Database: java db.DBServer 8000 true");
            System.exit(1);
        }

        try {
            Socket conn = new Socket(args[0],Integer.parseInt(args[1]));
            System.out.println("Connected to the Targed Database");
            DataOutputStream outputStream = new DataOutputStream(conn.getOutputStream());
            outputStream.writeUTF("POST /repartition");
            outputStream.close();
            conn.close();  

            try{
            Properties prop = new Properties();
            FileInputStream in = new FileInputStream("config.properties");

            try {
                prop.load(in);
            } catch (IOException e) {
                System.err.println("Could not load config file");
            }

            int index = 0;
            
            String[] hostsAndPort = prop.getProperty("db.hostsAndPorts").split(",");
            String[] backUPHostAndPort = prop.getProperty("db.backupHostsAndPorts").split(",");
            String[] hostsPath = prop.getProperty("db.masterDBPaths").split(",");
            String[] backUPPath = prop.getProperty("db.backUPPaths").split(",");
            


            String hostString = "";
            String backUPHostString = "";
            String hostsPathString = "";
            String backUPPathString = "";

            ArrayList<String> hosts = new ArrayList<>();
            String servDef = args[0] + ":" + args[1];

            for (int i = 0; i < hostsAndPort.length; i ++){
                if (hostsAndPort[i] == args[0] + ":" + args[1]){
                    index = i;
                }
            }

            for (int i = 0; i < hostsAndPort.length; i ++){
                if (i != index){
                    hostString = hostString + hostsAndPort[i] + ",";
                    backUPHostString = backUPHostString + backUPHostAndPort[i] + ",";
                    hostsPathString = hostsPathString + hostsPath[i] + ",";
                    backUPPathString = backUPPathString + backUPPath[i] + ",";
                    
                }
            }

            in.close();

            OutputStream output = null;
            
            prop.setProperty("db.hostsAndPorts", hostString);
            prop.setProperty("db.backupHostsAndPorts", backUPHostString);
            prop.setProperty("db.masterDBPaths", hostsPathString);
            prop.setProperty("db.backUPPaths", backUPPathString);

            try{
                output = new FileOutputStream("config.properties");
            }catch (IOException e){

                System.out.println(e);
            }
            System.out.println("Updating Config File");
            prop.store(output, null);

            output.close();
            

            }catch (IOException e){
                System.out.println("Unable to update file");
            }
        
        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }


}


