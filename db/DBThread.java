package db;

import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static db.DBConstants.*;
import static proxy.ProxyConstants.CONFIG_FILE;

public class DBThread extends Thread {
    private Socket client;
    private Connection dbConn;

    public DBThread(Socket socket, Connection dbConn) {
        this.client = socket;
        this.dbConn = dbConn;
    }

    public void run() {
        ResultSet resultSet = null;
        boolean isPutSuccessful = false;
        RequestType requestType = null;

        try {
            DataInputStream streamFromClient = new DataInputStream(this.client.getInputStream());
            String input = streamFromClient.readUTF();

            Statement statement = this.dbConn.createStatement();
            String shortResource = null;

            System.out.println("input: " + input);
            Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mput = pput.matcher(input);

            // PUT request
            if (mput.matches()) {
                requestType = RequestType.PUT;
                shortResource = mput.group(1);
                String longResource = mput.group(2);

                System.out.println("INSERT INTO URLShorteners (short, long) values('" + shortResource + "', '" + longResource + "')");
                int check = statement.executeUpdate("INSERT INTO URLShorteners (short, long) " +
                        "values('" + shortResource + "', '" + longResource + "')");
                isPutSuccessful = check > 0;
            }

            Pattern pget = Pattern.compile("^GET\\s+/(\\S+)\\s+.*");
            Matcher mget = pget.matcher(input);

            // GET request
            if (mget.matches()) {
                requestType = RequestType.GET;
                shortResource = mget.group(1);
                System.out.println("SELECT long FROM URLShorteners WHERE short='" + shortResource + "'");
                resultSet = statement.executeQuery("SELECT long FROM URLShorteners WHERE short='" + shortResource + "'");
            }

            Pattern ppost = Pattern.compile("^POST\\s+/repartition");
            Matcher mpost = ppost.matcher(input);

            // POST request (repartition)
            if (mpost.matches()) {
                requestType = RequestType.POST;
                this.handleRepartitionRequest(statement);
            }

            Pattern pdelete = Pattern.compile("^DELETE\\s+/repartition");
            Matcher mdelete = pdelete.matcher(input);

            // DELETE request (repartition)
            if (mdelete.matches()) {
                requestType = RequestType.DELETE;
                statement.executeUpdate("DELETE FROM URLShorteners");
            }

        } catch (IOException | SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (this.client != null) {
                    if (requestType == RequestType.POST || requestType == RequestType.DELETE) {
                        this.client.close();
                        return;
                    }

                    DataOutputStream outputStream = new DataOutputStream(this.client.getOutputStream());
                    String returnMessage = returnHandler(requestType, isPutSuccessful, resultSet);

                    System.out.println(returnMessage);
                    outputStream.writeUTF(returnMessage);
                    outputStream.close();
                    this.client.close();
                }

            } catch (Exception e) {
                // TODO: handle exception
                System.out.println(e);
            }
        }
    }

    private static String returnHandler(RequestType requestType, boolean isPutSuccessful, ResultSet resultSet) throws Exception {
        if (requestType == RequestType.PUT) {
            if (isPutSuccessful) {
                return Response.GOOD.toString();
            } else {
                return Response.BAD.toString();
            }
        } else {
            // Long extraction was successful
            if (resultSet.next()) {
                String longResource = resultSet.getString("long");
                return String.format("%s %s", Response.GOOD.toString(), longResource);
            }
            return Response.BAD.toString();
        }
    }

    private void handleRepartitionRequest(Statement statement) throws SQLException, IOException {
        Properties prop = new Properties();
        prop.load(new FileInputStream(CONFIG_FILE));

        int port = 0;
        try {
            port = Integer.parseInt(prop.getProperty("proxy.db.port"));
        } catch (Exception e) {
            System.err.println("Port must be an integer");
            System.exit(1);
        }

        Socket proxySocket = new Socket(prop.getProperty("proxy.db.host"), port);

        System.out.println("Repartition: getting all rows");
        ResultSet resultSet = statement.executeQuery("SELECT * FROM URLShorteners");

        DataOutputStream proxyStream = new DataOutputStream(proxySocket.getOutputStream());
        ArrayList<String> requests = new ArrayList<String>();

        while (resultSet.next()) {
            String shortVal = resultSet.getString("short");
            String longVal = resultSet.getString("long");
            String request = String.format("PUT /repartition/?short=%s&long=%s HTML/1.1", shortVal, longVal);
            requests.add(request);
        }

        System.out.println("Repartition: deleting table entries");
        statement.executeUpdate("DELETE FROM URLShorteners");

        for (String request : requests) {
            System.out.println(request);
            proxyStream.writeUTF(request);
        }

        proxyStream.writeUTF("DONE");
        System.out.println("Repartition: finished sending repartitioning requests");
        proxyStream.close();
    }
}