package ex03.connector.Http;

import java.io.IOException;
import java.io.OutputStream;
import java.net.Socket;
import org.apache.catalina.util.RequestUtil;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;

import org.apache.tomcat.util.res.StringManager;

import ex03.ServletProcess;
import ex03.StaticResourceProcess;

public class HttpProcesser {
	// 没什么用
	private HttpConnector connector = null;
	// HttpRequestLine 报文第一行只需要读一次所以可以用一个对象
	private HttpRequestLine requestLine = new HttpRequestLine();
	private HttpRequest request;
	private HttpResponse response;
	private StringManager sm = StringManager.getManager(Constants.PACKAGE_NAME);

	public HttpProcesser(HttpConnector connector) {
		this.connector = connector;
	}

	public void process(Socket socket) {
		try {
			ServletInputStream input = new ServletInputStream(socket.getInputStream(), 2048);
			OutputStream output = socket.getOutputStream();
			request = new HttpRequest(input);
			parseRequest(input);
			parseHeaders(input);
			response = new HttpResponse(output);
			response.setRequest(request);
			if(request.getRequestURI().startsWith("/servlet/")){
				ServletProcess sp=new ServletProcess();
				sp.process(request, response);
			}else{
				StaticResourceProcess spr=new StaticResourceProcess();
				spr.process(request, response);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	// 调用包装readrequestLine的方法封装第一行到HttprequetLine中之后
	private void parseRequest(ServletInputStream input) throws IOException, ServletException {
		input.readRequestLine(requestLine);
		String method = new String(requestLine.method, 0, requestLine.methodEnd);
		String protocol = new String(requestLine.protocol, 0, requestLine.protocolEnd);
		String uri = null;
		if (method.length() < 1) {
			throw new ServletException("Missing HTTP request method");
		} else if (requestLine.uriEnd < 1) {
			throw new ServletException("Missing HTTP request URI");
		}
		// 首先解析uri，绝对uri的情况，带有？参数的情况，带有;jsessionid的情况
		// Parse any query parameters out of the request URI
		int question = requestLine.indexOf("?");
		if (question >= 0) {
			request.setQueryString(new String(requestLine.uri, question + 1, requestLine.uriEnd - question - 1));
			uri = new String(requestLine.uri, 0, question);
		} else {
			request.setQueryString(null);
			uri = new String(requestLine.uri, 0, requestLine.uriEnd);
		}

		// Checking for an absolute URI (with the HTTP protocol)
		if (!uri.startsWith("/")) {
			int pos = uri.indexOf("://");
			// Parsing out protocol and host name
			if (pos != -1) {
				pos = uri.indexOf('/', pos + 3);
				if (pos == -1) {
					uri = "";
				} else {
					uri = uri.substring(pos);
				}
			}
		}

		// Parse any requested session ID out of the request URI
		String match = ";jsessionid=";
		int semicolon = uri.indexOf(match);
		if (semicolon >= 0) {
			String rest = uri.substring(semicolon + match.length());
			int semicolon2 = rest.indexOf(';');
			if (semicolon2 >= 0) {
				request.setRequestedSessionId(rest.substring(0, semicolon2));
				rest = rest.substring(semicolon2);
			} else {
				request.setRequestedSessionId(rest);
				rest = "";
			}
			request.setRequestedSessionURL(true);
			uri = uri.substring(0, semicolon) + rest;
		} else {
			request.setRequestedSessionId(null);
			request.setRequestedSessionURL(false);
		}

		// Normalize URI (using String operations at the moment)
		String normalizedUri = normalize(uri);

		// Set the corresponding request properties
		((HttpRequest) request).setMethod(method);
		request.setProtocol(protocol);
		if (normalizedUri != null) {
			((HttpRequest) request).setRequestURI(normalizedUri);
		} else {
			((HttpRequest) request).setRequestURI(uri);
		}

		if (normalizedUri == null) {
			throw new ServletException("Invalid URI: " + uri + "'");
		}

	}

	private void parseHeaders(ServletInputStream input) throws IOException, ServletException {
		// 掉这个方法前先判断是否还有参数要读取
		while (true) {
			HttpHeader header = new HttpHeader();
			input.readHeader(header);
			if (header.keyEnd == 0) {
				if (header.valueEnd == 0) {
					return;
				} else {
					throw new ServletException(sm.getString("httpProcessor.parseHeaders.colon"));
				}
			}
			// 需要特别设置下几个特殊请求首部:cookie,content-type，content-length
			String key = new String(header.key, 0, header.keyEnd);
			String value = new String(header.value, 0, header.valueEnd);
			request.addHeader(key, value);
			if (key.equals("cookie")) {
				// sessionId可能在cookie中所以要特别设置下key值为session的情况
				Cookie[] cookies = RequestUtil.parseCookieHeader(value);
				for (Cookie cookie : cookies) {
					if (cookie.getName().equals("jsessionid")) {
						request.setRequestedSessionId(value);
						request.setRequestedSessionURL(false);
						request.setRequestedSessionCookie(true);
					}
					request.addCookie(cookie);
				}
			} else if (key.equals("content-type")) {
				request.setContentType(value);
			} else if (key.equals("content-length")) {
				int n = -1;
				try {
					n = Integer.parseInt(value);
				} catch (Exception e) {
					throw new ServletException(sm.getString("httpProcessor.parseHeaders.contentLength"));
				}
				request.setContentLength(n);
			}
		}
	}

	private String normalize(String path) {
		if (path == null)
			return null;
		// Create a place for the normalized path
		String normalized = path;

		// Normalize "/%7E" and "/%7e" at the beginning to "/~"
		if (normalized.startsWith("/%7E") || normalized.startsWith("/%7e"))
			normalized = "/~" + normalized.substring(4);

		// Prevent encoding '%', '/', '.' and '\', which are special reserved
		// characters
		if ((normalized.indexOf("%25") >= 0) || (normalized.indexOf("%2F") >= 0) || (normalized.indexOf("%2E") >= 0)
				|| (normalized.indexOf("%5C") >= 0) || (normalized.indexOf("%2f") >= 0)
				|| (normalized.indexOf("%2e") >= 0) || (normalized.indexOf("%5c") >= 0)) {
			return null;
		}

		if (normalized.equals("/."))
			return "/";

		// Normalize the slashes and add leading slash if necessary
		if (normalized.indexOf('\\') >= 0)
			normalized = normalized.replace('\\', '/');
		if (!normalized.startsWith("/"))
			normalized = "/" + normalized;

		// Resolve occurrences of "//" in the normalized path
		while (true) {
			int index = normalized.indexOf("//");
			if (index < 0)
				break;
			normalized = normalized.substring(0, index) + normalized.substring(index + 1);
		}

		// Resolve occurrences of "/./" in the normalized path
		while (true) {
			int index = normalized.indexOf("/./");
			if (index < 0)
				break;
			normalized = normalized.substring(0, index) + normalized.substring(index + 2);
		}

		// Resolve occurrences of "/../" in the normalized path
		while (true) {
			int index = normalized.indexOf("/../");
			if (index < 0)
				break;
			if (index == 0)
				return (null); // Trying to go outside our context
			int index2 = normalized.lastIndexOf('/', index - 1);
			normalized = normalized.substring(0, index2) + normalized.substring(index + 3);
		}

		// Declare occurrences of "/..." (three or more dots) to be invalid
		// (on some Windows platforms this walks the directory tree!!!)
		if (normalized.indexOf("/...") >= 0)
			return (null);

		// Return the normalized path that we have completed
		return (normalized);

	}
}
