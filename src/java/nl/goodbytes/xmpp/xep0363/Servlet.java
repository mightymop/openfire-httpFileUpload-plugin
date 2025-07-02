/*
 * Copyright (c) 2017-2025 Guus der Kinderen. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.goodbytes.xmpp.xep0363;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.IQ;
import org.xmpp.packet.JID;
import org.xmpp.packet.PacketError;

import nl.goodbytes.xmpp.xep0363.repository.DatabaseRepository;


/**
 * Responsible for the HTTP(s) processing as defined in XEP-0363.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class Servlet extends HttpServlet
{
    private static final Logger Log = LoggerFactory.getLogger( Servlet.class );

    public static SecureUniqueId uuidFromPath( String path )
    {
        if ( path == null || path.isEmpty() )
        {
            return null;
        }

        final String[] parts = path.split( "/" );
        if ( parts.length < 2 )
        {
            return null;
        }

        try
        {
        	String uuidString = parts[ parts.length - 2 ];
            return SecureUniqueIdFactory.fromString( uuidString );
        }
        catch ( IllegalArgumentException e )
        {
            return null;
        }
    }

    public static long getCountFilesInDB() {
        Repository repository = RepositoryManager.getInstance().getRepository();
        if (repository != null)
          return repository.countRepoFiles(); 
        return 0L;
      }
      
      public static double getSumOfAllFilesDB() {
        Repository repository = RepositoryManager.getInstance().getRepository();
        if (repository != null)
          return repository.getRepoSize(); 
        return 0.0D;
      }
      
      public static boolean deleteFilesOlderThenDays(int days) {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.setTimeZone(TimeZone.getDefault());
        if (days > 0)
          days *= -1; 
        cal.add(6, days);
        return deleteFiles(cal);
      }
      
      public static void deleteFile(String uuid) {
        Repository repository = RepositoryManager.getInstance().getRepository();
        if (repository != null)
			try {
				repository.delete(SecureUUID.fromString(uuid));
			} catch (IOException e) {
				Log.error(e.getMessage());
			} 
      }
      
      public static boolean deleteFiles(GregorianCalendar cal) {
    
        Repository repository = RepositoryManager.getInstance().getRepository();
        if (repository == null) {
          Log.warn("... responded with INTERNAL_SERVER_ERROR. The repository is null.");
          return false;
        } 
        repository.purge(cal);
        return true;
      }
      
    @Override
    protected void service( HttpServletRequest request, HttpServletResponse response)
        throws ServletException, IOException
    {
        if (Boolean.parseBoolean(getInitParameter("wildcardCORS"))) {
            response.setHeader("Access-Control-Allow-Origin", "*");
            response.setHeader("Access-Control-Allow-Methods", "PUT, GET, HEAD, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "Overwrite, Destination, Content-Type, Depth, User-Agent, X-File-Size, X-Requested-With, If-Modified-Since, X-File-Name, Cache-Control");
        }

        final String contentSecurityPolicy = getInitParameter("contentSecurityPolicy");
        // Explicitly setting the CSP to empty will disable it.
        if ( contentSecurityPolicy != null && !contentSecurityPolicy.isEmpty() )
        {
            response.setHeader( "Content-Security-Policy", contentSecurityPolicy );
            Log.debug( "... setting Content-Security-Policy '{}'.", contentSecurityPolicy );
        } else {
            Log.debug( "... not setting Content-Security-Policy (not configured or intentionally blank)" );
        }

        super.service(request, response);
    }

    @Override
    protected void doGet( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        Log.info( "Processing GET request... ({} requesting from {})", req.getRemoteAddr(), req.getRequestURI() );
        final Repository repository = RepositoryManager.getInstance().getRepository();
        if ( repository == null )
        {
            resp.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            Log.warn( "... responded with INTERNAL_SERVER_ERROR. The repository is null." );
            return;
        }
     
        final SecureUniqueId uuid = uuidFromPath( req.getRequestURI() );
        if ( uuid == null )
        {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
            Log.info( "... responded with NOT_FOUND. Unable to parse UUID from request URI." );
            return;
        }
     
        if ( !repository.contains( uuid ) )
        {
            resp.sendError( HttpServletResponse.SC_NOT_FOUND );
            Log.info( "... responded with NOT_FOUND. The repository does not contain a path to the UUID that is parsed from request URI: {}", uuid.toString() );
            return;
        }
     
        final String eTagRequest = req.getHeader( "If-None-Match" );
        if ( eTagRequest != null )
        {
            final String calculatedETagHash = repository.calculateETagHash( uuid );
            if ( eTagRequest.equals( calculatedETagHash ) )
            {
                resp.setStatus( HttpServletResponse.SC_NOT_MODIFIED );
                Log.info( "... responded with NOT_MODIFIED. Provided ETag value matches the hash in the repository." );
                return;
            }
        }

        final String contentType = repository.getContentType( uuid );
        if ( contentType != null && !contentType.isEmpty() )
        {
            resp.setContentType( contentType );
            Log.debug( "... setting content type '{}'.", contentType );
        }
   
        final long size = repository.getSize( uuid );
        if ( size > 0 && size <= Integer.MAX_VALUE )
        {
            resp.setContentLength( (int) size );
            Log.debug( "... setting content length '{}'.", size );
        }
     
        String filename = null;
        try {
          filename = repository.getFilename(uuid);
        } catch (Exception e) {
          resp.sendError(404);
          Log.info("... responded with NOT_FOUND. The repository does not contain a filename to the UUID that is parsed from request URI: {}", uuid
              
              .toString());
          return;
        } 
      
        resp.setHeader("Content-Disposition", "inline; filename=\"" + filename + "\"");
        resp.setHeader( "Cache-Control", "max-age=31536000" );
        final String etag = repository.calculateETagHash( uuid );
        if ( etag != null )
        {
            resp.setHeader( "ETag", etag );
            Log.debug( "... setting ETag '{}'.", etag );
        }
   
        try ( final InputStream in = new BufferedInputStream( repository.getInputStream( uuid ) );
              final OutputStream out = resp.getOutputStream() )
        {
            final byte[] buffer = new byte[ 1024 * 4 ];
            int bytesRead;
            while ( ( bytesRead = in.read( buffer ) ) != -1 )
            {
                out.write( buffer, 0, bytesRead );
            }
        }
        Log.info( "... responded with OK and included the data in the response body." );
    }

    @Override
    protected void doPut( HttpServletRequest req, HttpServletResponse resp ) throws ServletException, IOException
    {
        Log.info( "Processing PUT request... ({} submitting to {})", req.getRemoteAddr(), req.getRequestURI() );
        final Repository repository = RepositoryManager.getInstance().getRepository();
        if ( repository == null )
        {
            resp.sendError( HttpServletResponse.SC_INTERNAL_SERVER_ERROR );
            Log.warn( "... responded with INTERNAL_SERVER_ERROR. The repository is null." );
            return;
        }

        final SecureUniqueId uuid = uuidFromPath( req.getRequestURI() );
        if ( uuid == null )
        {
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "The request lacks a slot identifier on its path." );
            Log.info( "... responded with BAD_REQUEST. The request lacks a slot identifier on its path." );
            return;
        }
      
        Slot slot = SlotManager.getInstance().consumeSlotForPut(uuid);
        Boolean onlyslots = Boolean.valueOf(JiveGlobals.getBooleanProperty("plugin.httpfileupload.slots", true));
        
        if (slot == null) {
        	 if (!onlyslots.booleanValue())
        	 {
	        	Log.info("SLOT NOT FOUND > OVERRIDE > CREATE SLOT FOR UPLOAD");
	            String filename = req.getRequestURI();
	            if (filename.indexOf("/") != -1)
	              filename = filename.substring(filename.lastIndexOf("/") + 1); 
	            if (filename.indexOf("?") != -1)
	              filename = filename.substring(0, filename.indexOf("?")); 
	            String remoteuser = req.getRemoteUser();
	            if (remoteuser == null || remoteuser.trim().length() == 0)
	              remoteuser = "unbekannt"; 
	            JID user = new JID(remoteuser + "@" + JiveGlobals.getProperty("xmpp.domain", "localhost"));
	            try {
	              slot = SlotManager.getInstance().getSlot(user, filename, req.getContentLength(), uuid);
	            } catch (Exception e) {
	              slot = null;
	              resp.sendError(400, "The request lacks a slot identifier on its path.");
	              Log.warn("... responded with BAD_REQUEST. The request lacks a slot identifier on its path.");
	              return;
	            } 
	            
	            if (slot==null)
	            {
	            	 Log.debug("... Could not obtain Slot.");
	            	 resp.sendError(500, "Could not obtain Slot.");
	            	 return;
	            }
	        
	        } else {
	        	 Log.debug("... responded with BAD_REQUEST. The requested slot is not available. Either it does not exist, or has already been used.");
	        	 resp.sendError(400, "The requested slot is not available. Either it does not exist, or has already been used.");
	        	 return;
	        } 
        }
    
        if ( req.getContentLength() != slot.getSize() )
        { // This can be faked by the client, but XEP says to be brutal.            
            Log.info( "... responded with BAD_REQUEST. Content length in request ({}) does not correspond with slot size ({}).", req.getContentLength(), slot.getSize() );
            resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "Content length in request does not correspond with slot size." );
            return;
        }
    
        Log.info("Content-Length: " + String.valueOf(req.getContentLength()));
        InputStream servletInputStream = req.getInputStream();
        BufferedInputStream bin = new BufferedInputStream((InputStream)servletInputStream);
        OutputStream out = null;
        if (repository.isDataBaseRepo()) {
          Log.info("Using Database as REPO");
          out = new ByteArrayOutputStream();
        } else {
          Log.info("Using other REPO");
          out = new BufferedOutputStream(repository.getOutputStream(slot.getUuid()));
        } 
        byte[] buffer = new byte[4096];
        int bytesRead = -1;
        long bytesLen = 0L;
        boolean error = false;
        Log.info("Reading data now...");
        while ((bytesRead = bin.read(buffer, 0, buffer.length)) != -1) {
          try {
            bytesLen += bytesRead;
            Log.debug(String.valueOf(bytesRead) + "/" + String.valueOf(req.getContentLength()) + " (" + String.valueOf(bytesLen * 100L / req.getContentLength()) + "%) bytes have been read");
            out.write(buffer, 0, bytesRead);
          } catch (Exception e) {
            error = true;
            Log.error(e.getMessage());
            Log.error("last part: bytesRead = " + String.valueOf(bytesRead));
            if (bytesRead != -1) {
              Log.debug("Writing rest of buffer to ByteArrayOutputstream.");
              out.write(buffer, 0, bytesRead);
              bytesLen += bytesRead;
            } 
          } 
        } 
        
        if (!error) {
        	    
	        final MalwareScannerManager malwareScannerManager = MalwareScannerManager.getInstance();
	        if (malwareScannerManager.isEnabled()) {
	            try {
	                Log.debug("... scanning uploaded content for malware ...");
	                final MalwareScanner malwareScanner = malwareScannerManager.getMalwareScanner();
	                malwareScanner.scan(slot.getUuid());
	                Log.info("... malware scanning did not find malware ...");
	                
	                if (repository.isDataBaseRepo()) {
	                    if (!((DatabaseRepository)repository).writeToDatabase(((ByteArrayOutputStream)out).toByteArray(), uuid
	                        .toString(), slot.getFilename())) {
	                      resp.sendError(500);
	                      ((DatabaseRepository)repository).delete(slot.getUuid());
	                      Log.info("... responded with DATABASE WRITE ERROR.");	                      
	                      return;
	                    } 
	                    else
	                    {
	                    	Log.info("Data have been saved to database...:)");
	                    }
	                   
	                  } else {
	                    Log.info("Data have been saved to repo...:)");
	                  } 
	                
	                try
	                {
	                    resp.setHeader( "Location", SlotManager.getGetUrl(slot).toExternalForm() );
	                }
	                catch ( URISyntaxException | MalformedURLException e )
	                {
	                    Log.warn( "Unable to calculate GET URL for {}", slot, e );
	                }

	                resp.setStatus( HttpServletResponse.SC_CREATED );
	                Log.info( "... responded with CREATED. Stored data from the request body in the repository." );
	                
	            } catch (MalwareDetectedException e) {
	                resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "Malware detected in the upload!" );
	                repository.delete(slot.getUuid());
	                Log.warn("... responded with BAD_REQUEST. Malware detected in upload of {}.", req.getRemoteAddr(), e);
	                return;
	            } catch (Throwable t) {
	                resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "Malware scanning failed" );
	                repository.delete(slot.getUuid());
	                Log.info("... responded with BAD_REQUEST. Malware scanner execution failed.", t);
	                return;
	            }
	        }
	        else
	        {
	        	if (repository.isDataBaseRepo()) {
                    if (!((DatabaseRepository)repository).writeToDatabase(((ByteArrayOutputStream)out).toByteArray(), uuid
                        .toString(), slot.getFilename())) {
                      resp.sendError(500);
                      ((DatabaseRepository)repository).delete(slot.getUuid());
                      Log.info("... responded with DATABASE WRITE ERROR.");	                      
                      return;
                    } 
                    else
                    {
                    	Log.info("Data have been saved to database...:)");
                    }
                   
                  } else {
                    Log.info("Data have been saved to repo...:)");
                  } 
                
                try
                {
                    resp.setHeader( "Location", SlotManager.getGetUrl(slot).toExternalForm() );
                }
                catch ( URISyntaxException | MalformedURLException e )
                {
                    Log.warn( "Unable to calculate GET URL for {}", slot, e );
                }

                resp.setStatus( HttpServletResponse.SC_CREATED );
                Log.info( "... responded with CREATED. Stored data from the request body in the repository." );
	        }
        }
        else
        {
        	resp.sendError( HttpServletResponse.SC_BAD_REQUEST, "Error while reading files from stream!" );
            repository.delete(slot.getUuid());
            return;
        }
      
    }
}
