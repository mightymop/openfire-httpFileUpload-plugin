package nl.goodbytes.xmpp.xep0363.repository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.json.JSONArray;

import nl.goodbytes.xmpp.xep0363.SecureUniqueId;

/**
 * A repository of files, backed by a regular directory.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
public class DirectoryRepository extends AbstractFileSystemRepository
{
    private final Path path;
    public DirectoryRepository( final Path path )
    {
        super();

        this.path = path;
    }

    @Override
    protected Path initializeRepository() throws IOException
    {
    	File ff = new File(this.path.toString());
	    	if (!ff.exists()) {
		    ff.mkdirs();
    	}
	    return this.path;
    }
}
