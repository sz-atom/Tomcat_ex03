package ex03;

import java.io.IOException;

import ex03.connector.Http.HttpRequest;
import ex03.connector.Http.HttpResponse;

public class StaticResourceProcess {
	public void process(HttpRequest request,HttpResponse response){
		try {
			response.sendStaticResource();
			response.finishResponse();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
