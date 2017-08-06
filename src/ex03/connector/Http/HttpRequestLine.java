package ex03.connector.Http;

public class HttpRequestLine {
	private static final int INITIAL_METHOD_SIZE=8;
	private static final int INITIAL_URI_SIZE=64;
	private static final int INITIAL_PROTOCOL_SIZE=8;
	public static final int MAX_METHOD_SIZE=1024;
	public static final int MAX_URI_SIZE=32768;
	public static final int MAX_PROTOC0L_SIZE=1024;
	public char[] method;
	public int methodEnd;
	public char[] uri;
	public int uriEnd;
	public char[] protocol;
	public int protocolEnd;
	public HttpRequestLine(){
		this(new char[INITIAL_METHOD_SIZE],0,new char[INITIAL_URI_SIZE],0
				,new char[INITIAL_PROTOCOL_SIZE],0);
	}
	  public HttpRequestLine(char[] method, int methodEnd,
              char[] uri, int uriEnd,
              char[] protocol, int protocolEnd) {

		this.method = method;
		this.methodEnd = methodEnd;
		this.uri = uri;
		this.uriEnd = uriEnd;
		this.protocol = protocol;
		this.protocolEnd = protocolEnd;
	  }
	  public int indexOf(String str){
		 return new String(uri).indexOf(str);
	  }
}
