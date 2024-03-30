# Java Web Server

A lightweight Java web server. Suitable for running local files and hosting websites. This server supports various file types including HTML, CSS, XML, media files, etc...



## Key Features

- **HTTP Request Handling:** Accepts client HTTP GET and HEAD requests.
- **Static File Serving:** Serves static files such as HTML, CSS, images, and media files.
- **Conditional GET:** Supports conditional GET requests using the `If-Modified-Since` header for caching.
- **Persistent Connections:** Supports persistent connections for improved performance.
- **Error Handling:** Provides appropriate error responses for bad requests (400), not found (404), and unsupported operations (501).
- **Simple Configuration:** Easy to set up and run with command-line arguments for specifying port number and website path.

## Usage

To run the server, use the following command:

```bash
java MyWebServer.java <Port Number> <Website Path>
```

Server hosted at LocalHost:\<Port Number\>

## Sample Website
![Sample Website](Website.gif)


## Requirements 
- JDK 8 or later
  
- Sense of curiosity
