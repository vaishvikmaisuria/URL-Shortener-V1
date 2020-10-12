package urlShortener;

import db.DBConstants.Response;
import cache.Cache;

import java.io.*;
import java.net.Socket;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class URLShortenerThread extends Thread {
    static final File WEB_ROOT = new File("./html");
    static final String FILE_NOT_FOUND = "404.html";
    static final String INTERNAL_ERROR = "500.html";
    static final String REDIRECT_RECORDED = "redirect_recorded.html";
    static final String REDIRECT = "redirect.html";

    private Socket client;
    private String dbProxyHost;
    private int dbProxyPort;
    private Cache cache;

    // verbose mode
    static final boolean verbose = false;

    public URLShortenerThread(Socket socket, String dbProxyHost, int dbProxyPort, Cache cache) {
        this.client = socket;
        this.dbProxyHost = dbProxyHost;
        this.dbProxyPort = dbProxyPort;
        this.cache = cache;
    }

    public void run() {
        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;

        try {
            in = new BufferedReader(new InputStreamReader(this.client.getInputStream()));
            out = new PrintWriter(this.client.getOutputStream());
            dataOut = new BufferedOutputStream(this.client.getOutputStream());

            String input = in.readLine();

            if (verbose) System.out.println("first line: " + input);
            Pattern pput = Pattern.compile("^PUT\\s+/\\?short=(\\S+)&long=(\\S+)\\s+(\\S+)$");
            Matcher mput = pput.matcher(input);
            if (mput.matches()) {
                String response = save(input);

                System.out.println("Response: " + response);

                // If the PUT failed
                if (response.contains(Response.BAD.toString()) && !response.contains(Response.GOOD.toString())) {
                    File file = new File(WEB_ROOT, INTERNAL_ERROR);
                    int fileLength = (int) file.length();
                    String contentMimeType = "text/html";
                    //read content to return to client
                    byte[] fileData = readFileData(file, fileLength);

                    out.println("HTTP/1.1 500 OK");
                    out.println("Server: Java HTTP Server/Shortener : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + contentMimeType);
                    out.println("Content-length: " + fileLength);
                    out.println();
                    out.flush();

                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                } else {
                    File file = new File(WEB_ROOT, REDIRECT_RECORDED);
                    int fileLength = (int) file.length();
                    String contentMimeType = "text/html";
                    //read content to return to client
                    byte[] fileData = readFileData(file, fileLength);

                    out.println("HTTP/1.1 200 OK");
                    out.println("Server: Java HTTP Server/Shortener : 1.0");
                    out.println("Date: " + new Date());
                    out.println("Content-type: " + contentMimeType);
                    out.println("Content-length: " + fileLength);
                    out.println();
                    out.flush();

                    dataOut.write(fileData, 0, fileLength);
                    dataOut.flush();
                }
            } else {
                Pattern pget = Pattern.compile("^GET\\s+/(\\S+)\\s+.*");
                Matcher mget = pget.matcher(input);

                if (mget.matches()) {
                    String shortResource = mget.group(1);
                    String cacheResponse = this.cache.get(shortResource);
                    String response;

                    // Check cache first
                    if (cacheResponse != null) {
                        response = String.format("%s %s", Response.GOOD, cacheResponse);
                        System.out.println("ALREADY IN CACHE. response: " + response);
                    } else {
                        response = find(input);
                    }

                    System.out.println("Response: " + response);
                    
                    // If the long resource was not found
                    if (response.contains(Response.BAD.toString()) && !response.contains(Response.GOOD.toString())) {
                        File file = new File(WEB_ROOT, FILE_NOT_FOUND);
                        int fileLength = (int) file.length();
                        String content = "text/html";
                        byte[] fileData = readFileData(file, fileLength);

                        out.println("HTTP/1.1 404 File Not Found");
                        out.println("Server: Java HTTP Server/Shortener : 1.0");
                        out.println("Date: " + new Date());
                        out.println("Content-type: " + content);
                        out.println("Content-length: " + fileLength);
                        out.println();
                        out.flush();

                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();
                    } else {
                        String longResource = response.split(" ")[1];
                        
                        // Add to cache if it wasn't already in it
                        if (cacheResponse == null) {
                            this.cache.put(shortResource, longResource);
                            System.out.println("NOT IN CACHE. added: " + shortResource + " " + longResource);
                        }

                        File file = new File(WEB_ROOT, REDIRECT);
                        int fileLength = (int) file.length();
                        String contentMimeType = "text/html";

                        //read content to return to client
                        byte[] fileData = readFileData(file, fileLength);

                        // out.println("HTTP/1.1 301 Moved Permanently");
                        out.println("HTTP/1.1 307 Temporary Redirect");
                        out.println("Location: " + longResource);
                        out.println("Server: Java HTTP Server/Shortener : 1.0");
                        out.println("Date: " + new Date());
                        out.println("Content-type: " + contentMimeType);
                        out.println("Content-length: " + fileLength);
                        out.println();
                        out.flush();

                        dataOut.write(fileData, 0, fileLength);
                        dataOut.flush();
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Server error: " + e);
        } finally {
            try {
                in.close();
                out.close();
                this.client.close(); // we close socket connection
            } catch (Exception e) {
                System.err.println("Error closing stream : " + e.getMessage());
            }

            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }
    }

    private String find(String input) throws IOException {
        Socket dbProxyServer = new Socket(this.dbProxyHost, this.dbProxyPort);
        DataOutputStream streamToServer = new DataOutputStream(dbProxyServer.getOutputStream());
        streamToServer.writeUTF(input);

        // Read the DB Proxy's response
        DataInputStream streamFromServer = new DataInputStream(dbProxyServer.getInputStream());
        String output = "";
        try {
            output = streamFromServer.readUTF();
        } catch (EOFException e) {}
        
        System.out.println(output);

        return output;
    }

    private String save(String input) throws IOException {
        Socket dbProxyServer = new Socket(this.dbProxyHost, this.dbProxyPort);
        DataOutputStream streamToServer = new DataOutputStream(dbProxyServer.getOutputStream());
        streamToServer.writeUTF(input);

        // Read the DB Proxy's response
        DataInputStream streamFromServer = new DataInputStream(dbProxyServer.getInputStream());
        String output= "";
        try {
            output = streamFromServer.readUTF();
        } catch (EOFException e) {}

        return output;
    }

    private static byte[] readFileData(File file, int fileLength) throws IOException {
        FileInputStream fileIn = null;
        byte[] fileData = new byte[fileLength];

        try {
            fileIn = new FileInputStream(file);
            fileIn.read(fileData);
        } finally {
            if (fileIn != null)
                fileIn.close();
        }

        return fileData;
    }
}
