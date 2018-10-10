package org.nuxeo.ai.blob;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.impl.blob.StringBlob;
import org.nuxeo.ecm.core.api.impl.blob.URLBlob;
import org.nuxeo.ecm.core.blob.BlobInfo;
import org.nuxeo.ecm.core.blob.BlobManager;
import org.nuxeo.ecm.core.blob.BlobProvider;
import org.nuxeo.runtime.api.Framework;

public class AIBlobHelper {

	public static URI getBlobURI(DocumentModel doc, String xpath) {
		Blob blob = (Blob) doc.getPropertyValue(xpath);
		return getBlobURI(blob);
	}

	public static URI getBlobURI(Blob blob) {

		BlobManager bm = Framework.getService(BlobManager.class);
		URI uri = null;
		try {
			uri = bm.getURI(blob, null, null);
		} catch (IOException e) {
		}
		if (uri == null) {
			try {
				uri = new URI("blob", blob.getDigest(), null);
			} catch (URISyntaxException e1) {
				throw new RuntimeException(e1);
			}
		}
		return uri;
	}

	public static Blob insertBlob(CoreSession session, String content) throws IOException {		
		StringBlob blob = new StringBlob(content);
		BlobManager bm = Framework.getService(BlobManager.class);
		BlobProvider bp = bm.getBlobProvider(session.getRepositoryName());
		
		String key = bp.writeBlob(blob);
		BlobInfo bi = new BlobInfo();
		bi.key=key;
		return bp.readBlob(bi);
	}
	
	public static Blob resolveBlobFromURI(CoreSession session, URI uri) throws IOException {		
		if (uri.getScheme().equals("blob")) {
			BlobManager bm = Framework.getService(BlobManager.class);
			BlobProvider bp = bm.getBlobProvider(session.getRepositoryName());
			BlobInfo bi = new BlobInfo();
			bi.key=uri.getSchemeSpecificPart();
			return bp.readBlob(bi);						
		} else {			
			return new URLBlob(uri.toURL());
		}
		
	}
}
