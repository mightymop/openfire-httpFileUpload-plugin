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
 *
 */

package nl.goodbytes.xmpp.xep0363;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xmpp.packet.JID;

import javax.annotation.Nonnull;
import nl.goodbytes.xmpp.xep0363.*;
import nl.goodbytes.xmpp.xep0363.repository.AbstractFileSystemRepository;

/**
 * Representation of a ticket that is allows a single file upload.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class Slot implements Serializable
{

    private SecureUniqueId uuid = null; // This is cryptographically 'strong'.
    private final Date creationDate = new Date();
    private final String filename;
    private final JID creator;
    private final long size;
    private static final Logger Log = LoggerFactory.getLogger(SlotManager.class);

    public Slot(JID creator, String filename, long size) {
      this.creator = creator;
      this.filename = filename;
      this.size = size;
      this.uuid = SecureUUID.generate();
      
      Log.debug("create slot: "+this.creator.toString()+" filename: "+this.filename+" size: "+String.valueOf(this.size)+" uuid: "+this.uuid.toString());
    }
    
    public Slot(JID creator, String filename, long size, SecureUniqueId uuid) {
      this.creator = creator;
      this.filename = filename;
      this.size = size;
      this.uuid = uuid;
      
      Log.debug("create slot: "+this.creator.toString()+" filename: "+this.filename+" size: "+String.valueOf(this.size)+" uuid: "+this.uuid.toString());
    }

    public Date getCreationDate()
    {
        return creationDate;
    }

    public JID getCreator()
    {
        return creator;
    }

    public long getSize()
    {
        return size;
    }

    public SecureUniqueId getUuid()
    {
        return uuid;
    }

    public String getFilename() {
        return filename;
    }
    
    public URL getPutUrl() throws URISyntaxException, MalformedURLException {
        return getURL();
      }
      
      public URL getGetUrl() throws URISyntaxException, MalformedURLException {
        return getURL();
      }
    
    private URL getURL() throws URISyntaxException, MalformedURLException {
        String path;
        if (SlotManager.getInstance().getWebContextRoot().endsWith("/")) {
          path = SlotManager.getInstance().getWebContextRoot() + this.uuid.toString() + "/" + this.filename;
        } else {
          path = SlotManager.getInstance().getWebContextRoot() + "/" + this.uuid.toString() + "/" + this.filename;
        } 
        URI uri = new URI(SlotManager.getInstance().getWebProtocol(), null, SlotManager.getInstance().getWebHost(), SlotManager.getInstance().getWebPort(), path, null, null);
        String usascii = uri.toASCIIString();
        Log.info("SLOT URL: " + usascii);
        return new URL(usascii);
      }

    @Override
    public String toString()
    {
    	return "{\"uuid\":\"" + this.uuid + "\", \"creationDate\":\"" + this.creationDate + "\", \"filename\":\"" + this.filename + "\", \"creator\":\"" + this.creator.toString() + "\", \"size\":\"" + this.size + "\"" + '}';
    }
}
