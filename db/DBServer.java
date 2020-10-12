package db;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.sql.*;
import java.util.Properties;

// USAGE: java -classpath ".:sqlite3.jar" db/DBServer 7003
// USAGE: java -classpath ".:sqlite3.jar" db/DBServer 7003 true 

public class DBServer {
    public static void main(String[] args) throws IOException {

        int port = 0;

        if (args.length < 1 || args.length > 2) {
            System.err.println("USAGE: java db.DBServer PORT BACKUPFlag(true/false) # where the BACKUPFlag is optional");
            System.err.println("Example for a Master Database: java db.DBServer 8000");
            System.err.println("Example for a BackUP Database: java db.DBServer 8000 true");
            System.exit(1);
        }

        try {
            port = Integer.parseInt(args[0]);
        } catch (Exception e) {
            System.err.println("Port must be an integer");
            System.exit(1);
        }

        Properties prop = new Properties();
        prop.load(new FileInputStream("config.properties"));
        String dbName = "";
        String dbPath = "";
        String user = "";
        String dbUser = "";

        if (args.length == 2) {
            // This is a backup DB
            try {
                dbName = getDBFilePath(prop, 2, args[0]);
                dbUser = getDBFilePath(prop, 3, args[0]);
                dbPath = getDBFilePath(prop, 4, args[0]);
                dbPath = dbPath + dbUser + "/" + dbName;

            } catch (Exception e) {
                System.err.println("Can't Extract a valid path from config.properties");
                e.printStackTrace();
                System.exit(1);
            }   
        }else{
            // This is a master DB
            try {
                dbName = getDBFilePath(prop, 1, args[0]);
                dbUser = getDBFilePath(prop, 3, args[0]);
                dbPath = getDBFilePath(prop, 4, args[0]);
                dbPath = dbPath + dbUser + "/" + dbName;
                

            } catch (Exception e) {
                System.err.println("Can't Extract a valid path from config.properties");
                e.printStackTrace();
                System.exit(1);
            } 
        }

        try {
            ServerSocket serverConnect = new ServerSocket(port);
            System.out.println("Starting database server on port " + port);
            Connection dbConn = null;
            try{
                System.out.println(dbPath);
                dbConn = getDBConnection(dbPath);
            }catch (Exception e){
                System.out.println("Connection to SQL Error Vash");
            }
            
        
            // We listen until user halts server execution
            while (true) {
                new DBThread(serverConnect.accept(), dbConn).start();
            }
        } catch (IOException e) {
            System.err.println("Server Connection error : " + e.getMessage());
        }
    }

    public static Connection getDBConnection(String path) {
        Connection conn = null;
        try {
            conn = DriverManager.getConnection("jdbc:sqlite:" + path);
        } catch (Exception e) {
            System.err.println(e);
        }
        return conn;
    }

    public static String getDBFilePath(Properties prop, int serverType, String port)throws Exception{
        String path = "";
        InetAddress inetAddress = InetAddress.getLocalHost();
        String hostName = inetAddress.getHostName();
        
        String serverDef = new String(hostName + ":" + port);

        if (serverType == 2){
            //This is a backUP server
            String[] backUPhosts = prop.getProperty("db.backupHostsAndPorts").split(",");
            int i = 0;
            while (i < backUPhosts.length){
                String host = new String(backUPhosts[i]);
                if (host.equals(serverDef)){
                    break;
                }
                i ++;
            }
            path = prop.getProperty("db.backUPPaths").split(",")[i];
        } else if (serverType == 3){
            path = prop.getProperty("db.user");
        } else if (serverType == 4){
            path = prop.getProperty("db.path");

        } else {
            //This is a Master Server
            String[] ServerHosts = prop.getProperty("db.hostsAndPorts").split(",");
            int i = 0;
            while (i < ServerHosts.length){
                String host = new String(ServerHosts[i]);

                if (host.equals(serverDef)){
                    break;
                }
                i ++;
            }

            path = prop.getProperty("db.masterDBPaths").split(",")[i];
        }

        return path;
    }
}


