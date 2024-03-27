/*****************************
 Java Web Server Project
 Connor MacKinnon
 ****************************/

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.ParseException;
import java.text.SimpleDateFormat;


public class MyWebServer {


    private static int PORT_NUMBER = 80;
    private static ServerSocket serverSocket = null;
    private static final String SERVER_NAME = "Connor's Web Server";

    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Input should match: java MyWebServer <port_number> <~/evaluationWeb>");
            return;
        }
        PORT_NUMBER = Integer.parseInt(args[0]);
        String website_path = args[1];

        try {
            serverSocket = new ServerSocket(PORT_NUMBER);
            System.out.println("Server started, visit localhost:" + PORT_NUMBER);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                handleConnection(clientSocket, website_path);
            }
        } catch (Exception e) {
            System.out.println("Error with input arguments");
        }
    }

    //Connection handler that manages overall http request
    private static void handleConnection(Socket clientSocket, String website_path) {
        try (BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
             OutputStream outputStream = clientSocket.getOutputStream();) {

            // read, parse, and finally respond to client request
            List<String> request = readRequest(inFromClient);
            Request httpRequest = parseRequest(request);
            Respond(httpRequest, outputStream, website_path);
        } catch (Exception e) {
            System.out.println("Error with connection: " + e.getMessage());
            // close the socket after an error
            try {
                clientSocket.close();
            } catch (IOException eClose) {
                System.out.println("Error closing client socket: " + eClose.getMessage());
            }
        }
    }

    // Turn the http request into a list
    private static List<String> readRequest(BufferedReader in) throws IOException {
        List<String> requestLines = new ArrayList<>();
        String line;
        // Add each line of the request to the list
        while ((line = in.readLine()) != null) {
            if (line.isEmpty()) {
                break;
            }
            requestLines.add(line);
        }
        return requestLines;
    }

    // Function to parse through the http request and create a Request object
    private static Request parseRequest(List<String> requestLines) {
        // Extract and split the first request line
        String requestLine = requestLines.get(0);
        String[] requestParts = requestLine.split(" ");
        // [HTTP verb, path]
        String verb = requestParts[0];
        String path = requestParts[1];
        // Find which line has the If-Modified-Since tag
        Date ifModifiedSince = null;
        for (int i = 1; i < requestLines.size(); i++) {
            String line = requestLines.get(i);
            if (line.startsWith("If-Modified-Since")) {
                String dateString = line.substring("If-Modified-Since:".length()).trim();
                SimpleDateFormat dateFormat = new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy", Locale.US); // Input expected as: Sat Jan 1 12:57:06 EST 2023
                dateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
                try {
                    ifModifiedSince = dateFormat.parse(dateString);
                    System.out.print("");
                } catch (ParseException e) {
                    return new Request("BAD_REQUEST", null, null, false, false);
                }
                break;
            }
        }
        boolean persistent = requestLines.get(1).contains("Connection: keep-alive");
        boolean closeRequested = requestLines.get(1).contains("Connection: close");
        //Return a request object combining all the fields
        return new Request(verb, path, ifModifiedSince, persistent, closeRequested);
    }


    // Method to handle HTTP request and generate appropriate response
    private static void Respond(Request httpRequest, OutputStream out, String website_path) {
        try {
            if ("HEAD".equals(httpRequest.getVerb())) {
                Head(httpRequest, out, website_path);
            } else if ("GET".equals(httpRequest.getVerb())) {
                Get(httpRequest, out, website_path);
            } else if ("BAD_REQUEST".equals(httpRequest.getVerb())) {
                Bad_Request(httpRequest, out, website_path);
            } else {
                // Unimplemented request type
                String notImplementedResponse = "HTTP/1.1 501 Not Implemented\r\nContent-Type: text/html\r\n\r\n501 Not Implemented \n";
                out.write(notImplementedResponse.getBytes());
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Error with request: " + e.getMessage());
            try {
                out.close();
            } catch (IOException e2) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Process the correct output for head requests accordingly
    private static void Head(Request httpRequest, OutputStream out, String website_path) {
        String filePath = website_path + httpRequest.getPath();
        File requestedFile = new File(filePath);
        // Set default landing page
        if (requestedFile.isDirectory()) {
            filePath = filePath + File.separator + "index.html/";
        }
        File file = new File(filePath);

        try {
            if (file.exists() && file.isFile()) {
                // Check if modified
                Date currentDate = new Date();
                Date ifModifiedSince = httpRequest.getIfModifiedSince();
                if (ifModifiedSince != null && file.lastModified() <= ifModifiedSince.getTime()) {
                    String error304 = "HTTP/1.1 304 NOT MODIFIED\r\nDate: " + currentDate + "\r\nContent-Type: text/html\r\n\r\n304 Not Modified \n";
                    out.write(error304.getBytes());
                    out.flush();
                } else {
                    String response = "HTTP/1.1 200 OK\r\nDate: " + currentDate + "\r\nServer: " + SERVER_NAME +
                            "\r\nLast-Modified: " + new Date(file.lastModified()) + "\r\nContent-Length: "
                            + file.length() + "\r\n\r\n";
                    out.write(response.getBytes());
                    out.flush();
                }
            } else {
                // File not found
                String errorMessage = "HTTP/1.1 404 NOT FOUND \r\n Content-Type: text/html\r\n\r\n 404 Not Found \n";
                out.write(errorMessage.getBytes());
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Error with request: " + e.getMessage());
            try {
                out.close();
            } catch (IOException e2) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Process the correct output for GET requests accordingly
    private static void Get(Request httpRequest, OutputStream out, String website_path) {
        try {
            // Locate file
            String filePath = website_path + File.separator + httpRequest.getPath();
            File requestedFile = new File(filePath);
            if (requestedFile.isDirectory()) {
                filePath = filePath + File.separator + "index.html";
            }
            File file = new File(filePath);

            if (file.exists() && file.isFile()) {
                // add the file to the output stream
                FileInputStream useroutput = new FileInputStream(file);
                byte[] fileBits = new byte[1024] ;
                int remainingBytes;
                while ((remainingBytes = useroutput.read(fileBits)) != -1) {
                    out.write(fileBits, 0, remainingBytes);
                }
                out.flush();
                useroutput.close();
            } else {
                // File not found
                String errorMessage = "HTTP/1.1 404 NOT FOUND \r\nContent-Type: text/html\r\n\r\n 404 Not Found \n";
                out.write(errorMessage.getBytes());
                out.flush();
            }
        } catch (IOException e) {
            System.out.println("Error with request: " + e.getMessage());
            try {
                out.close();
            } catch (IOException e2) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }

    // Process the correct output for Bad Requests
    private static void Bad_Request(Request httpRequest, OutputStream out, String website_path) {
        try {
            String error400 = "HTTP/1.1 400 Bad Request\r\nContent-Type: text/html\r\n\r\n 400 Bad Request\n";
            out.write(error400.getBytes());
            out.flush();
        } catch (IOException e) {
            System.out.println("Error handling request: " + e.getMessage());
            try {
                out.close();
            } catch (IOException e2) {
                System.out.println("Error closing connection: " + e.getMessage());
            }
        }
    }
}

class Request {
    private String verb;
    private String path;
    private boolean persistent;
    private boolean closeRequested;
    private Date ifModifiedSince;

    public Request(String verb, String path, Date ifModifiedSince, boolean persistent, boolean closeRequested) {
        this.verb = verb;
        this.path = path;
        this.ifModifiedSince = ifModifiedSince;
        this.persistent = persistent;
        this.closeRequested = closeRequested;
    }

    public String getVerb() {
        return verb;
    }

    public String getPath() {
        return path;
    }

    public Date getIfModifiedSince() {
        return ifModifiedSince;
    }
}
