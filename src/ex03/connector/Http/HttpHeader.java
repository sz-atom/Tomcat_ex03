package ex03.connector.Http;

public class HttpHeader {
	private static final int INITIAL_KEY_SIZE = 32;
	public static final int MAX_KEY_SIZE = 128;
	private static final int INITIAL_VALUE_SIZE = 64;
	public static final int MAX_VALUE_SIZE = 4096;
	public char[] key;
	public char[] value;
	public int keyEnd;
	public int valueEnd;

	public HttpHeader() {
		this(new char[INITIAL_KEY_SIZE], 0, new char[INITIAL_VALUE_SIZE], 0);
	}

	public HttpHeader(char[] key, int keyEnd, char[] value, int valueEnd) {
		this.key = key;
		this.keyEnd = keyEnd;
		this.value = value;
		this.valueEnd = valueEnd;
	}

	public void recycle() {
		keyEnd = 0;
		valueEnd = 0;
	}
}
