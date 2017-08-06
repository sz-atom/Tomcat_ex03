package ex03.connector.Http;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletInputStream;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.apache.catalina.util.Enumerator;
import org.apache.catalina.util.ParameterMap;
import org.apache.catalina.util.RequestUtil;

import ex03.connector.RequestStream;

public class HttpRequest implements HttpServletRequest {
	private InputStream input;
	private int contentLength;
	private String requestedSessionId;
	private boolean requestedSessionURI;
	private String method;
	private String protocol;
	private String requestURI;
	private String queryString;
	private boolean requestedSessionCookie;
	private String contentType;
	private ParameterMap parameterMap=null;
	private boolean hasRead=false;
	private ArrayList<Cookie> arrayList=new ArrayList<Cookie>();
	private HashMap<String,ArrayList<String>> header=new HashMap<String,ArrayList<String>>();
	public void setQueryString(String queryString) {
		this.queryString = queryString;
	}
	public void addCookie(Cookie cookie){
		synchronized (arrayList) {
			arrayList.add(cookie);
		}
	}
	private void parseParameters(){
		if(hasRead){
			return;
		}
		ParameterMap results=parameterMap;
		if(results==null){
			results=new ParameterMap();
		}
		//可以修改数据
		results.setLocked(false);
		//先设置编码
		String encoding=getCharacterEncoding();
		if(encoding==null){
			encoding="ISO-8859-1";
		}
		//先解析uri中带的参数(post表单中也有的话)
		String queryString=getQueryString();
		if(queryString!=null){
			try {
				RequestUtil.parseParameters(results, queryString, encoding);
			} catch (UnsupportedEncodingException e) {
			}
		}
		String type=getContentType();
		if(type==null){
			type="";
		}
		int postion=type.indexOf(";");
		if(postion>=0){
			type=type.substring(0, postion).trim();
		}else{
			type=type.trim();
		}
		if(getMethod().equals("POST")&&getContentLength()>0&&type.equals("application/x-www-form-urlencoded")){
			//从ServletInputStream中读取
			int maxRead=getContentLength();
			byte[] buffer=new byte[maxRead];
			int len=0;
			try {
				while(len<=maxRead){
					int read=input.read(buffer, 0, maxRead-len);
					if(read<0){
						break;
					}
					len+=read;
				}
				RequestUtil.parseParameters(results, buffer, encoding);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		hasRead=true;
		results.setLocked(true);
		parameterMap=results;
	}
	public void setContentType(String contentType){
		this.contentType=contentType;
	}
	public void setRequestedSessionCookie(boolean flag){
		this.requestedSessionCookie=flag;
	}
	public void setRequestURI(String requestURI) {
		this.requestURI = requestURI;
	}

	public void setMethod(String method) {
		this.method = method;
	}

	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}

	public HttpRequest(InputStream input) {
		this.input = input;
	}

	public void setRequestedSessionId(String requestedSessionId) {
		this.requestedSessionId = requestedSessionId;
	}

	public void setRequestedSessionURL(boolean flag) {
		this.requestedSessionURI = flag;
	}

	public InputStream getStream() {
		return input;
	}

	public void addHeader(String key, String value) {
		key=key.toLowerCase();
		synchronized (header) {
			ArrayList<String> al=header.get(key);
			if(al==null){
				al=new ArrayList<String>();
			}
			al.add(value);
			header.put(key, al);
		}
	}
	public String getHeader(String key){
		synchronized (header) {
			ArrayList<String> al=header.get(key);
			if(al!=null){
				return al.get(0);
			}else{
				return null;
			}
		}
	}
	public Enumeration getHeaderNames() {
		 synchronized (header) {
		      return (new Enumerator(header.keySet()));
		    }
	}

	public Enumeration getHeaders(String key) {
		 key = key.toLowerCase();
		    synchronized (header) {
		      ArrayList values = (ArrayList) header.get(key);
		      if (values != null)
		        return (new Enumerator(values));
		      else
		        return (new Enumerator(values));
		    }
	}

	public Object getAttribute(String arg0) {

		return null;
	}

	public Enumeration getAttributeNames() {

		return null;
	}

	public String getCharacterEncoding() {

		return null;
	}

	public void setContentLength(int contentLength) {
		this.contentLength = contentLength;
	}

	public int getContentLength() {
		return contentLength;
	}

	public String getContentType() {

		return this.contentType;
	}

	public ServletInputStream getInputStream() throws IOException {
		return new RequestStream(this);
	}

	public Locale getLocale() {

		return null;
	}

	public Enumeration getLocales() {

		return null;
	}

	public String getParameter(String name) {
		parseParameters();
		String[] value=(String[]) parameterMap.get(name);
		if(value!=null){
			return value[0];
		}else{
			return null;
		}
	}

	public Map getParameterMap() {
		parseParameters();
		return this.parameterMap;
	}

	public Enumeration getParameterNames() {
		parseParameters();
		return new Enumerator(parameterMap.keySet());
	}

	public String[] getParameterValues(String name) {
		parseParameters();
		String[] values=(String[]) parameterMap.get(name);
		return values;
	}

	public String getProtocol() {

		return this.protocol;
	}

	public BufferedReader getReader() throws IOException {

		return null;
	}

	public String getRealPath(String arg0) {

		return null;
	}

	public String getRemoteAddr() {

		return null;
	}

	public String getRemoteHost() {

		return null;
	}

	public RequestDispatcher getRequestDispatcher(String arg0) {

		return null;
	}

	public String getScheme() {

		return null;
	}

	public String getServerName() {

		return null;
	}

	public int getServerPort() {

		return 0;
	}

	public boolean isSecure() {

		return false;
	}

	public void removeAttribute(String arg0) {

	}

	public void setAttribute(String arg0, Object arg1) {

	}

	public void setCharacterEncoding(String arg0) throws UnsupportedEncodingException {

	}

	public String getAuthType() {

		return null;
	}

	public String getContextPath() {

		return null;
	}

	public Cookie[] getCookies() {
		return (Cookie[]) this.arrayList.toArray();
	}

	public long getDateHeader(String arg0) {

		return 0;
	}

	public int getIntHeader(String arg0) {

		return 0;
	}

	public String getMethod() {

		return this.method;
	}

	public String getPathInfo() {

		return null;
	}

	public String getPathTranslated() {

		return null;
	}

	public String getQueryString() {

		return this.queryString;
	}

	public String getRemoteUser() {
		return null;
	}

	public String getRequestURI() {
		return this.requestURI;
	}

	public StringBuffer getRequestURL() {
		return null;
	}

	public String getRequestedSessionId() {
		return this.requestedSessionId;
	}

	public String getServletPath() {
		return null;
	}

	public HttpSession getSession() {
		return null;
	}

	public HttpSession getSession(boolean arg0) {
		return null;
	}

	public Principal getUserPrincipal() {
		return null;
	}

	public boolean isRequestedSessionIdFromCookie() {
		return false;
	}

	public boolean isRequestedSessionIdFromURL() {

		return false;
	}

	public boolean isRequestedSessionIdFromUrl() {

		return false;
	}

	public boolean isRequestedSessionIdValid() {
		return false;
	}

	public boolean isUserInRole(String arg0) {
		return false;
	}

}
