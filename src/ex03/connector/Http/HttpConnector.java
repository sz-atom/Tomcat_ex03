package ex03.connector.Http;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//该类主要负责accept连接
public class HttpConnector implements Runnable{
	public void start(){
		Thread thread=new Thread(this);
		thread.run();
	}
	public void run() {
		ServerSocket ss=null;
		try {
			ss=new ServerSocket(8080);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(1);
		}
		while(true){
			Socket socket=null;
			try {
				socket = ss.accept();
			} catch (IOException e) {
				continue;
			}
			HttpProcesser hp=new HttpProcesser(this);
			hp.process(socket);
		}
	}
}
