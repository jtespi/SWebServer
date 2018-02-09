/**
* CS371 Software Development
* Jacob Espinoza
* 2018 Feb 08
*
* A simple web server: it creates a new WebWorker for each new client
* connection, so all the WebServer object does is listen on the port
* for incoming client connection requests.
*
* This class contains the application "main()" (see below). At startup, 
* main() creates an object of this class (WebServer) and invokes its
* runsServer() method. Since servers run continually, the runServer() method 
* never returns. It uses socket programming to listen for client network
* connection requests. When one happens, it creates a new object of
* the WebWorker class and hands that client connection off to the WebWorker
* object. The WebServer object then just keeps listening for new client
* connections. See the WebWorker source for more information about it.
* 
**/

import java.net.*;
import java.util.*;

public class WebServer
{
Scanner termInput = new Scanner(System.in);
private String input="";
private String inputLo="";

private ServerSocket socket;
private boolean running;

/**
* Constructor
**/
private WebServer()
{
   running = false;
}

/**
* Web server starting point. This method does not return until the server is finished.
* @param port is the TCP port number to accept connections on
**/
private boolean runServer(int port)
{
   Socket workerSocket;
   WebWorker worker;
   try {
      socket = new ServerSocket(port);
      System.out.println("Server running...");
   } catch (Exception e) {
      System.err.println("Error binding to port "+port+": "+e);
      return false;
   }
   while (true) {
    /*  // process user input, if needed
      if ( termInput.hasNextLine() == true ) {
          input = termInput.nextLine();
          inputLo = input.toLowerCase();
          }
         
      // Stop the server if the user insists
      if ( input.contains("halt") ) {
         System.out.println("**** HALTING SERVER ****");
         break;
      }
      */
      try {
         // otherwise,
         // wait and listen for new client connection
         workerSocket = socket.accept();
         
      } catch (Exception e) {
         System.err.println("No longer accepting: "+e);
         break;
      }
      // have new client connection, so fire off a worker on it
      worker = new WebWorker(workerSocket);
      new Thread(worker).start();
   }
   return true;
} // end runServer

/**
* Application main: process command line and start web server; default
* port number is 8080 if not given on command line.
**/
public static void main(String args[])
{
   int port = 8080;
   if (args.length > 1) {
      System.err.println("Usage: java Webserver <portNumber>");
      return;
   } else if (args.length == 1) {
      try {
         port = Integer.parseInt(args[0]);
      } catch (Exception e) {
         System.err.println("Argument must be an int ("+e+")");
         return;
      }
   }
   WebServer server = new WebServer();
   if (!server.runServer(port)) {
      System.err.println("Execution failed!");
   }
} // end main

} // end class

