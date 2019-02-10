/**
* @author Catalina Sanchez-Maes
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

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;
import java.util.Scanner;

public class WebWorker implements Runnable
{
   private Socket socket;

/**
* Constructor: must have a valid open socket
* @param s of object Socket
**/
   public WebWorker(Socket s)
   {
      socket = s;
   }

/**
* Worker thread starting point. Each worker handles just one HTTP 
* request and then returns, which destroys the thread. This method
* assumes that whoever created the worker created it with a valid
* open socket object.
**/
   public void run()
   {
      System.out.println("Connection incoming.");
      try {
         InputStream  is = socket.getInputStream();
         OutputStream os = socket.getOutputStream();
      
         String location = readHTTPRequest(is).trim();
         if(fileExists(location)){
            writeHTTPHeader(os, "text/html");
            writeContent(os, location);
         }else if (location.equals("") || location.equals(null)){
            writeHTTPHeader(os, "text/html");
            os.write("<html><head></head><body>\n".getBytes());
            os.write("<h3>You are connected to the server</h3>\n".getBytes());
            os.write("</body></html>\n".getBytes());
         }else{
            write404Error(os, "text/html", location);
         }

         os.flush();
         socket.close();
      
      } catch (Exception e) {
         System.err.println("Output error: "+e);
      }
      System.err.println("Done handling connection.");
      return;
   }

/**
* Checks existance of file within user's directory
* @param location the string of the file path
**/

   private boolean fileExists(String location)
   {
      String fName;
      if(location.length() > 4){
         if ((location.substring(location.length()-5, location.length()).equals(".html"))){
            fName = (System.getProperty("user.dir")+(location)).trim();
         }else{
            fName = (System.getProperty("user.dir")+(location)).trim()+".html";
         }  
      }else{
         fName = (System.getProperty("user.dir")+(location)).trim()+".html";
      }
      File file = new File (fName);
      return file.exists() && !file.isDirectory() && file.isFile();
   }
      
/**
* Read the HTTP request header.
**/

   private String readHTTPRequest(InputStream is)
   {
      String line, result = "";
      BufferedReader r = new BufferedReader(new InputStreamReader(is));
      while (true) {
         try {
            while (!r.ready()){
               Thread.sleep(1);
            }
            line = r.readLine();
            if(line.contains("GET") && !line.contains("GET / HTTP/1.1")){
               result = line.substring(4, line.length()-8);
            }
            System.out.println("Request line: ("+line+")");
            if (line.length()==0){
               break;
            }
         } catch (Exception e) {
            System.err.println("Request error: "+e);
            break;
         }
      }
      return result;
   }

/**
* Write the HTTP header on the client
* @param os the OutputStream object
* @param contentType the string types read (html/text)
**/

   private void writeHTTPHeader(OutputStream os, String contentType) throws Exception
   {  
      Date date = new Date();
      DateFormat dform = DateFormat.getDateTimeInstance();
      dform.setTimeZone(TimeZone.getTimeZone("GMT"));
      System.out.println("response HTTP OK");
      os.write("HTTP/1.1 200 OK\n".getBytes());
      os.write("Date: ".getBytes());
      os.write((dform.format(date)).getBytes());
      os.write("\n".getBytes());
      os.write("Server: Catalina's very own server\n".getBytes());
      //os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
      //os.write("Content-Length: 438\n".getBytes()); 
      os.write("Connection: close\n".getBytes());
      os.write("Content-Type: ".getBytes());
      os.write(contentType.getBytes());
      os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
      return;
   }

/**
* Write 404 Not Found Error Status & message
* @param os the OutputStream object
* @param contentType the string types read (html/text)
* @param location the string of the file path
**/

   private void write404Error(OutputStream os, String contentType, String location) throws Exception
   {
      Date date = new Date();
      DateFormat dform = DateFormat.getDateTimeInstance();
      dform.setTimeZone( TimeZone.getTimeZone( "GMT" ) );
      os.write( "HTTP/1.1 404 Not Found\n".getBytes() );
      os.write( "Date: ".getBytes() );
      os.write( ( dform.format(date)).getBytes() );
      os.write( "\n".getBytes() );
      os.write( "Server: Catalina's very own server\n".getBytes() );
      //os.write( "Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes() );
      //os.write( "Content-Length: 438\n".getBytes() );
      os.write( "Connection: close\n".getBytes() );
      os.write( "Content-Type: ".getBytes() );
      os.write( contentType.getBytes() );
      os.write( "\n\n".getBytes() ); // HTTP header ends with 2 newlines
      os.write("<html><head></head><body>\n".getBytes());
      os.write("<h3>404 Not Found</h3>\n".getBytes());
      os.write("<p>The requested URL ".getBytes());
      os.write(location.getBytes());
      os.write(" was not found on this server.</p>".getBytes());
      os.write("<p>Please contact the server's admin for assistance</p>".getBytes());
      os.write("<p>Server admin: Catalina, learner@learn.edu</p>".getBytes());
      os.write("</body></html>\n".getBytes());
      return;
   }

/**
* Write file content to the client 
* @precondition HTTPHeader must be called first 
* @param os the OutputStream object
* @param the string of the file location
**/
   private void writeContent(OutputStream os, String location) throws Exception
   {
      String fName;
      if(location.length() > 4){
         if ((location.substring(location.length()-5, location.length()).equals(".html"))){
            fName = (System.getProperty("user.dir")+(location)).trim();
         }else{
            fName = (System.getProperty("user.dir")+(location)).trim()+".html";
         }
      }else{
         fName = (System.getProperty("user.dir")+(location)).trim() + ".html";
      }
      Date date = new Date();
      DateFormat dform = DateFormat.getDateTimeInstance();
      dform.setTimeZone(TimeZone.getTimeZone("GMT"));
      
      Scanner input = new Scanner(new File(fName));
      while(input.hasNextLine()){
         os.write((input.nextLine().replace("<cs371date>", dform.format(date)).replace("<cs371server>", "User's workstation")).getBytes());
      }
   }
} // end class