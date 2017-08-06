package ex03.startup;

import ex03.connector.Http.HttpConnector;

public class BootStrap {
	public static void main(String[] args) {
		HttpConnector hc=new HttpConnector();
		hc.start();
	}
}
