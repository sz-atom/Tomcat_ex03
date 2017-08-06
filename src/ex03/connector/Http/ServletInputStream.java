package ex03.connector.Http;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;

import org.apache.tomcat.util.res.StringManager;

public class ServletInputStream extends InputStream {
	private InputStream input;
	private byte[] buffer;
	private int pos;
	private int count;
	private StringManager sm = StringManager.getManager(Constants.PACKAGE_NAME);
	/**
	 * CR.
	 */
	private static final byte CR = (byte) '\r';
	/**
	 * LF.
	 */
	private static final byte LF = (byte) '\n';
	/**
	 * SP.
	 */
	private static final byte SP = (byte) ' ';
	/**
	 * HT.
	 */
	private static final byte HT = (byte) '\t';
	/**
	 * COLON.
	 */
	private static final byte COLON = (byte) ':';
	/**
	 * Lower case offset.
	 */
	private static final int LC_OFFSET = 'A' - 'a';

	public ServletInputStream(InputStream input, int maxSize) {
		this.input = input;
		this.buffer = new byte[maxSize];
	}

	// 解析请求行封装到requestLine
	public void readRequestLine(HttpRequestLine requestLine) throws IOException {
		// 去除CRLF
		int chr = 0;
		do {
			chr = read();
		} while (chr == CR || chr == LF);
		if (chr == -1) {
			throw new EOFException(sm.getString("RrequestStream.readline.error"));
		}
		pos--;
		// 截取method
		int maxRead = requestLine.method.length;
		int readCount = 0;
		boolean space = false;
		while (!space) {
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpRequestLine.MAX_METHOD_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(requestLine.method, 0, newBuffer, 0, maxRead);
					requestLine.method = newBuffer;
					maxRead = requestLine.method.length;
				} else {
					throw new IOException(sm.getString("requestStream.readline.toolong"));
				}
			}
			if (pos >= count) {
				int val = read();
				if (val == -1) {
					throw new IOException(sm.getString("requestStream.readline.error"));
				}
			}
			if (buffer[pos] == SP) {
				space = true;
			}
			requestLine.method[readCount++] = (char) buffer[pos++];
		}
		requestLine.methodEnd = readCount-1;
		// Reading URI
		maxRead = requestLine.uri.length;
		readCount = 0;
		space = false;
		boolean eol = false;
		while (!space) {
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpRequestLine.MAX_URI_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(requestLine.uri, 0, newBuffer, 0, maxRead);
					requestLine.uri = newBuffer;
					maxRead = requestLine.uri.length;
				} else {
					throw new IOException(sm.getString("requestStream.readline.toolong"));
				}
			}
			if (pos >= count) {
				int val = read();
				if (val == -1)
					throw new IOException(sm.getString("requestStream.readline.error"));
			}
			if (buffer[pos] == SP) {
				space = true;
			} else if (buffer[pos] == CR) {
				// HTTP/0.9 style request
				eol = true;
				space = true;
			}
			requestLine.uri[readCount++] = (char) buffer[pos++];
		}
		requestLine.uriEnd = readCount - 1;
		// Reading protocol
		maxRead = requestLine.protocol.length;
		readCount = 0;
		while (!eol) {
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpRequestLine.MAX_PROTOC0L_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(requestLine.protocol, 0, newBuffer, 0, maxRead);
					requestLine.protocol = newBuffer;
					maxRead = requestLine.protocol.length;
				} else {
					throw new IOException(sm.getString("requestStream.readline.toolong"));
				}
			}
			if (pos >= count) {
				int val = read();
				if (val == -1)
					throw new IOException(sm.getString("requestStream.readline.error"));
			}
			if (buffer[pos] == CR) {
			} else if (buffer[pos] == LF) {
				eol = true;
			} else {
				requestLine.protocol[readCount++] = (char) buffer[pos];
			}
			pos++;
		}
		requestLine.protocolEnd = readCount;
	}

	public void readHeader(HttpHeader header) throws IOException {
		if (header.keyEnd != 0) {
			header.recycle();
		}
		int chr = read();
		// 该方法会一直调用所以需要在每次调用的时候确认是否已经读完
		if (chr == CR || chr == LF) {
			if (chr == CR) {
				pos++;
			}
			header.keyEnd = 0;
			header.valueEnd = 0;
			return;
		} else if (chr == -1) {
			throw new EOFException(sm.getString("RequestStream.readLine.error"));
		}
		pos--;
		// 可以读取key了
		int maxRead = header.key.length;
		int readCount = 0;
		boolean colon = false;
		while (!colon) {
			if (readCount >= maxRead) {
				if ((2 * maxRead) <= HttpHeader.MAX_KEY_SIZE) {
					char[] newBuffer = new char[2 * maxRead];
					System.arraycopy(header.key, 0, newBuffer, 0, maxRead);
					header.key = newBuffer;
					maxRead = header.key.length;
				} else {
					throw new IOException(sm.getString("requestStream.readline.toolong"));
				}
			}
			if (pos >= count) {
				int val = read();
				if (val == -1) {
					throw new IOException(sm.getString("requestStream.readline.error"));
				}
			}
			if (buffer[pos] == COLON) {
				colon = true;
			}
			char val = (char) buffer[pos++];
			if ((val >= 'A') && (val <= 'Z')) {
				val = (char) (val - LC_OFFSET);
			}
			header.key[readCount++] = val;
		}
		header.keyEnd = readCount - 1;
		// 可以读取value了，其中value的读取要考虑几个问题,请求首部形如下面的格式，参数中带换行
		// Server: value VALUE value/r/n value value 参数换行后会接上一个SP或者HT这种情况要继续读
		// 取参数并把/r/n换行换成SP替换到HttpHeader中
		maxRead = header.value.length;
		readCount = 0;
		boolean eol = false;
		boolean validLine = true;
		while (validLine) {
			boolean space = true;
			while (space) {
				if (pos >= count) {
					int val = read();
					if (val == -1)
						throw new IOException(sm.getString("requestStream.readline.error"));
				}
				if ((buffer[pos] == SP) || (buffer[pos] == HT)) {
					pos++;
				} else {
					space = false;
				}
			}
			while (!eol) {
				if (readCount >= maxRead) {
					if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
						char[] newBuffer = new char[2 * maxRead];
						System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
						header.value = newBuffer;
						maxRead = header.value.length;
					} else {
						throw new IOException(sm.getString("requestStream.readline.toolong"));
					}
				}
				if (pos >= count) {
					int val = read();
					if (val == -1)
						throw new IOException(sm.getString("requestStream.readline.error"));
				}
				if (buffer[pos] == CR) {
				} else if (buffer[pos] == LF) {
					eol = true;
				} else {
					int ch = buffer[pos] & 0xff;
					header.value[readCount++] = (char) ch;
				}
				pos++;
			}

			int nextChr = read();
			if ((nextChr != SP) && (nextChr != HT)) {
				pos--;
				validLine = false;
			} else {
				eol = false;
				if (readCount >= maxRead) {
					if ((2 * maxRead) <= HttpHeader.MAX_VALUE_SIZE) {
						char[] newBuffer = new char[2 * maxRead];
						System.arraycopy(header.value, 0, newBuffer, 0, maxRead);
						header.value = newBuffer;
						maxRead = header.value.length;
					} else {
						throw new IOException(sm.getString("requestStream.readline.toolong"));
					}
				}
				header.value[readCount++] = ' ';
			}
		}
		header.valueEnd = readCount;
	}

	public int read() throws IOException {
		if (pos >= count) {
			fill();
			if (pos >= count) {
				return -1;
			}
		}
		return buffer[pos++] & 0xff;
	}

	private void fill() throws IOException {
		pos = 0;
		count = 0;
		int read = input.read(buffer, 0, buffer.length);
		if (read > 0) {
			count = read;
		}
	}
}
