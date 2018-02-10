/**
* Web worker: an object of this class executes in its own new thread
* to receive and respond to a single HTTP request. After the constructor
* the object executes on its "run" method, and leaves when it is done.
*
* One WebWorker object is only responsible for one client connection. 
* This code uses Java threads to parallelize the handling of clients:
* each WebWorker runs in its own thread. This means that you can essentially
* just think about what is happening on one client at a time, ignoring 
* the fact that the entirety of the webserver execution might be handling
* other clients, too. 
*
* This WebWorker class (i.e., an object of this class) is where all the
* client interaction is done. The "run()" method is the beginning -- think
* of it as the "main()" for a client interaction. It does three things in
* a row, invoking three methods in this class: it reads the incoming HTTP
* request; it writes out an HTTP header to begin its response, and then it
* writes out some HTML content for the response content. HTTP requests and
* responses are just lines of text (in a very particular format). 
*
**/

/** CS 371 Software Development
 * Program 2 - Java web server
 * Jacob Espinoza
 * v2.5 - 2018 February 09
 */

import java.net.Socket;
import java.lang.Runnable;
import java.nio.file.*;
import java.io.*;
import java.util.Date;
import java.text.*;
import java.util.TimeZone;

public class WebWorker implements Runnable
{

private Socket socket;

// Added string for the path of the file
private String path = "";
private String fileName = "";
// StringBuilder for the path string
private StringBuilder sb = new StringBuilder();

// Boolean flag for a file could not be found error
private boolean fileError = false;

// a Path object for the get request
Path source;
String MIMEString="";

private FileInputStream fileIn = null;

/**
* Constructor: must have a valid open socket
**/
public WebWorker(Socket s)
{
   socket = s;
}

/**
*  -- READ INCOMING HTTP REQUEST --
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
public void run()
{
   System.err.println("Handling connection...");
   try {
      InputStream  is = socket.getInputStream();
      OutputStream os = socket.getOutputStream();
      readHTTPRequest(is);

      // Added method that attempts to access the file
      queryFile();

   // Auto MIME Typing:
   //  MIMEString is generated in the readHTTPRequest function
      writeHTTPHeader(os, MIMEString);

      writeContent(os);
      os.flush();
      socket.close();
   } catch (Exception e) {
      System.err.println("Output error: "+e);
   }
   System.err.println("Done handling connection.");
   return;
} /**** END MAIN RUN FUNCTION
*****/

/**
* Read the HTTP request header.
**/
private void readHTTPRequest(InputStream is)
{
   String line;
   BufferedReader r = new BufferedReader(new InputStreamReader(is));
   while (true) {
      try {
         while (!r.ready()) Thread.sleep(1);
         line = r.readLine();
 /***** Added code to get the path for the file that is being requested *****/
         if ( line.contains("GET") ) {
    		// start at index 4, the first character after the space
		int lineInd = 4;
		sb.append(".");

		// continue building the string until there is a space
		while ( Character.isWhitespace( line.charAt( lineInd ) ) == false ) {
		  sb.append( line.charAt( lineInd ) );
		  lineInd++;
		} // end while there is no whitespace

		// convert the stringBuilder to a string
		path = sb.toString();
		
		// set a Path object named source
		source = Paths.get(path);

		 // clear the stringBuilder
		 sb.setLength(0);
	     // Print the path that is being requested
	     System.out.println(" ***** PATH = " + source );
         } // End if line contains GET
         
 /***** END added code  *****/
         System.err.println("Request line: ("+line+")");
         if (line.length() == 0) break;
      } catch (Exception e) {
         System.err.println("Request error: "+e);
         break;
      }
   }
   return;
}

/** Added function to check if the file can be opened
 *  If a file not found error occurs, set the boolean error flag to true
 *   and print an error message in the web server terminal (debug msg).
 */

private void queryFile() throws IOException {
  File file = new File( path );
  
  // use the probeContentType function to get a MIME Type string
  MIMEString = Files.probeContentType(source);
  
  System.out.print("*-- Auto MIME Type = ");
  System.out.println(Files.probeContentType(source));
  
  // Get the exact file name from the path
  fileName = trimPath ( path );
  System.err.println(" --- fileName = " + fileName );
  
  // boolean flag fileError is the negation of the return value of file.exists()
  fileError = !(file.exists());
  if (fileError == true ) System.err.println("***** Error - file not found!!");
  else System.out.println("+++++ File was found :)");
}


/* trimPath
 *** a recursive function that trims the path string to just the file name
*/
private String trimPath( String original ) {
  if ( original.substring(0,1).equals(".") ) {
     return trimPath ( original.substring(1) );
  }
  if ( original.contains("/") ) {
     return trimPath ( original.substring(1) );
  }
  return original;
}

/**
* Write the HTTP header lines to the client network connection.
* @param os is the OutputStream object to write to
* @param contentType is the string MIME content type (e.g. "text/html")
**/
private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
{
   Date d = new Date();
   DateFormat df = new SimpleDateFormat("yyyy-MMM-dd 'at' HH:mm:ss z");
  // set the time zone to UTC
   df.setTimeZone(TimeZone.getTimeZone("UTC"));
  // Check the file error flag
   if ( fileError == false ) {
      os.write("HTTP/1.1 200 OK\n".getBytes());
   }
   else {
      os.write("HTTP/1.1 404 Not Found\n".getBytes());
   }

   os.write("Date: ".getBytes());
   os.write((df.format(d)).getBytes());
   os.write("\n".getBytes());
   os.write("Server: Jacob's Program 2 server\n".getBytes());
   //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
   //os.write("Content-Length: 438\n".getBytes()); 
   os.write("Connection: close\n".getBytes());
   os.write("Content-Type: ".getBytes());
   os.write(contentType.getBytes());
   os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
   return;
}

/**
* Write the data content to the client network connection. This MUST
* be done after the HTTP header has been written out.
* @param os is the OutputStream object to write to
**/
private void writeContent(OutputStream os) throws Exception
{
  // the image file is valid, so serve the IMAGE
  if ( fileError == false && MIMEString.contains("image") ) {
	File file = new File( path );
	fileIn = new FileInputStream ( file );
	
	while ( fileIn.available() > 0 ) {
		os.write( fileIn.read());
	}
  }

  // the file is valid, so serve the plain text file
  else if ( fileError == false && MIMEString.contains("plain") ) {
      File file = new File( path );
	fileIn = new FileInputStream ( file );
	
	os.write("\nText file name: ".getBytes());
	os.write(fileName.getBytes());
      os.write("\n\n".getBytes());
	
	// Serve the plain text file byte-by-byte
	while ( fileIn.available() > 0 ) {
		os.write( fileIn.read());
	}
  }

  // the file is valid, so serve the HTML file
   else if ( fileError == false && !(MIMEString.contains("plain")) ) {
      os.write("<html><head></head><body>\n".getBytes());

      File file = new File( path );
      BufferedReader brPrint = new BufferedReader( new FileReader( file ));
      String lineOfFile;
      
      // Serve the HTML file line-by-line
      while ( (lineOfFile = brPrint.readLine()) != null ) {
         // Look for the tag <cs371date> and replace with the current time/date
         if ( lineOfFile.contains("<cs371date>") ) {
            Date d2 = new Date();
            DateFormat df2 = new SimpleDateFormat("yyyy-MMM-dd 'at' HH:mm:ss zzzz");
            os.write("Date & time: ".getBytes());
            os.write((df2.format(d2)).getBytes());
            }
         // Look for the tag <cs371server> and replace with an identifying string for the server
         if ( lineOfFile.contains("<cs371server>") ) {
           os.write("Server: Jacob's Program 2 server\n".getBytes());
           }
         
         // if tags are not found, just write one line of the file
         os.write(lineOfFile.getBytes());
      } // file finished being read

      // close the HTML document
      os.write("</body></html>\n".getBytes());
   }
   
   // serve any other file types byte by byte
   else if ( fileError == false ) {
       File otherFile = new File( path );
	   fileIn = new FileInputStream ( otherFile );
	
	   while ( fileIn.available() > 0 ) {
		   os.write( fileIn.read());
	      }
   } 
  /** else the file is invalid, 
   * print a 404 error message that the user can see (HTML body)
   */
   else {
      os.write("<html><head></head><body>\n".getBytes());
      os.write("<h3><font color =\"red\">404 Error</font> - The file could not be found!</h3>\n".getBytes());
      os.write("<br>Requested file: ".getBytes());
      os.write(fileName.getBytes());
      os.write("</body></html>\n".getBytes());
   }
   
} // end writeContent function

} // end class
