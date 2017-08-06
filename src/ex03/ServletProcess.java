package ex03;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandler;

import javax.servlet.Servlet;

import ex03.connector.Http.Constants;
import ex03.connector.Http.HttpRequest;
import ex03.connector.Http.HttpRequestFacade;
import ex03.connector.Http.HttpResponse;
import ex03.connector.Http.HttpResponseFacade;

public class ServletProcess {
	public void process(HttpRequest request,HttpResponse response){
		//采用URLClassLoader加载器加载class文件
		String uri=request.getRequestURI();
		int postion=uri.lastIndexOf("/");
		String servletName=uri.substring(postion+1);
		URL[] urls=new URL[1];
		URLStreamHandler ush=null;
		File classPath=new File(Constants.WEB_ROOT);
		URLClassLoader classLoader=null;
		try {
			String repository=new URL("file",null,classPath.getCanonicalPath()+File.separator).toString();
			urls[0]=new URL(null,repository,ush);
			classLoader=URLClassLoader.newInstance(urls);
		} catch (Exception e) {
			e.printStackTrace();
		}
		Class<?> clazz=null;
		try {
			clazz=classLoader.loadClass(servletName);
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		Servlet servlet=null;
		try {
			servlet=(Servlet) clazz.newInstance();
			HttpRequestFacade hrf=new HttpRequestFacade(request);
			HttpResponseFacade hr=new HttpResponseFacade(response);
			response.sendHeaders();
			servlet.service(hrf, hr);
			response.finishResponse();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
