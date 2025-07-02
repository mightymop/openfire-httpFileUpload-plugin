/*
 * Copyright (c) 2017-2022 Guus der Kinderen. All rights reserved.
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
 */

package nl.goodbytes.xmpp.xep0363;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.util.JiveGlobals;
import org.xmpp.packet.JID;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import nl.goodbytes.xmpp.xep0363.repository.DatabaseRepository;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.jivesoftware.util.JiveGlobals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.annotation.Nonnull;

/**
 * A manager of HTTP slots.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class SlotManager
{
	private static final Logger Log = LoggerFactory.getLogger(SlotManager.class);
	
	public static final long DEFAULT_MAX_FILE_SIZE = Long.parseLong(JiveGlobals.getProperty("plugin.httpfileupload.maxFileSize", String.valueOf(50 * 1024 * 1024)));
	
    private long maxFileSize = DEFAULT_MAX_FILE_SIZE;

    private static SlotManager INSTANCE = null;

    public synchronized static SlotManager getInstance()
    {
        if ( INSTANCE == null )
        {
            INSTANCE = new SlotManager();
        }

        return INSTANCE;
    }
    
    private SlotProvider slotProvider;

    public void initialize( final SlotProvider slotProvider )
    {
        if ( this.slotProvider != null )
        {
            throw new IllegalArgumentException( "Already initialized." );
        }
        this.slotProvider = slotProvider;
    }

    private String webProtocol;
    private String webHost;
    private Integer webPort;
    private String webContextRoot;

    public Long getMaxFileSize()
    {
        return maxFileSize;
    }

    public void setMaxFileSize( Long maxFileSize )
    {
        this.maxFileSize = maxFileSize;
    }
    
    
    public Slot consumeSlotForPut( SecureUniqueId uuid )
    {
    	Slot slot = null;
    	
    	Repository repository = RepositoryManager.getInstance().getRepository();
    	if (repository != null && repository.isDataBaseRepo()) {
    		
    	    slot = ((DatabaseRepository)repository).readSlotFromDB(uuid);
    	} 
    	else
    	{    		
    		slot = slotProvider.consume(uuid);
    	}
    	
    	return slot;
    }


    public static URL getPutUrl(@Nonnull final Slot slot) throws URISyntaxException, MalformedURLException
    {
        return getURL(slot);
    }

    public static URL getGetUrl(@Nonnull final Slot slot) throws URISyntaxException, MalformedURLException
    {
        return getURL(slot);
    }

    private static URL getURL(@Nonnull final Slot slot) throws URISyntaxException, MalformedURLException
    {
        final String path;
        if ( SlotManager.getInstance().getWebContextRoot().endsWith( "/" ) )
        {
            path = SlotManager.getInstance().getWebContextRoot() + slot.getUuid() + "/" + slot.getFilename();
        }
        else
        {
            path = SlotManager.getInstance().getWebContextRoot() + "/" + slot.getUuid() + "/" + slot.getFilename();
        }

        // First, use URI to properly encode all components.
        final URI uri = new URI(
            SlotManager.getInstance().getWebProtocol(),
            null, // userinfo
            SlotManager.getInstance().getWebHost(),
            SlotManager.getInstance().getWebPort(),
            path,
            null, // query
            null // fragment
        );

        // Then, ensure that the URL contains US-ASCII characters only, to prevent issues with some clients.
        final String usascii = uri.toASCIIString();

        // Finally, transform the result into an URL.
        return new URL( usascii );
    }
    
    public Slot getSlot(JID from, String fileName, long fileSize) throws TooLargeException {
        return getSlot(from, fileName, fileSize, null);
      }

    public Slot getSlot( JID from, String fileName, long fileSize, SecureUniqueId uuid ) throws TooLargeException
    {
          if ( maxFileSize > 0 && fileSize > maxFileSize )
          {
              throw new TooLargeException( fileSize, maxFileSize );
          }

          if (uuid == null)
              uuid = SecureUUID.generate(); 
          
          final Slot slot = new Slot( from, fileName, fileSize , uuid);

          Repository repository = RepositoryManager.getInstance().getRepository();
          if (repository != null && repository.isDataBaseRepo()) {
            if (((DatabaseRepository)repository).writeSlotToDB(slot))
            {
            	Log.debug("Slot written to database");
            	return slot;
            }
            else
            {
            	Log.error("Error while obtaining slot.");
                throw new TooLargeException( fileSize, maxFileSize );
            }
          } 
          else        	  
          {
        	  slotProvider.create(slot);
          }
          
          return slot;
      }

    public void setWebProtocol( final String webProtocol )
    {
        this.webProtocol = webProtocol;
    }

    public String getWebProtocol()
    {
        return webProtocol;
    }

    public void setWebHost( final String webHost )
    {
        this.webHost = webHost;
    }

    public String getWebHost()
    {
        return webHost;
    }

    public void setWebPort( final int webPort )
    {
        this.webPort = webPort;
    }

    public Integer getWebPort()
    {
        return webPort;
    }

    public String getWebContextRoot()
    {
        return webContextRoot;
    }

    public void setWebContextRoot( final String webContextRoot )
    {
        this.webContextRoot = webContextRoot;
    }
}
