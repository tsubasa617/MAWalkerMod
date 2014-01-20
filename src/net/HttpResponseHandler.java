package net;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

public final class HttpResponseHandler implements ResponseHandler<byte[]> {
	public byte[] handleResponse(HttpResponse hr)
			throws ClientProtocolException, IOException {
		if (hr.getStatusLine().getStatusCode() == 200) {
			if (hr.getEntity().getContentEncoding() == null) {
				return EntityUtils.toByteArray(hr.getEntity());
			}

			if (hr.getEntity().getContentEncoding().getValue().contains("gzip")) {
				GZIPInputStream gis = null;
				try {
					gis = new GZIPInputStream(hr.getEntity().getContent());
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					byte[] b = new byte[1024];
					int n;
					while ((n = gis.read(b)) != -1)
						baos.write(b, 0, n);
					System.out.println("Gzipped");
					return baos.toByteArray();
				} catch (IOException e) {
					throw e;
				} finally {
					try {
						if (gis != null)
							gis.close();
					} catch (IOException ex) {
						throw ex;
					}

				}
			}
			return EntityUtils.toByteArray(hr.getEntity());
		}

		if (hr.getStatusLine().getStatusCode() == 302) {
			throw new IOException("302");
		}
		return null;
	}
}
