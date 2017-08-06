package ex03.connector;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletInputStream;

import org.apache.tomcat.util.res.StringManager;

import ex03.connector.Http.Constants;
import ex03.connector.Http.HttpRequest;

public class RequestStream extends ServletInputStream {
	private InputStream stream;
    protected boolean closed = false;
    protected int count = 0;
    protected int length = -1;
    protected static StringManager sm =
        StringManager.getManager(Constants.PACKAGE_NAME);
	public RequestStream(HttpRequest request){
		closed=false;
		count=0;
		length=request.getContentLength();
		this.stream=request.getStream();
	}
	public int read() throws IOException {
		return stream.read();
	}

}
