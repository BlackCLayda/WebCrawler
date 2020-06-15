package webCrawler.app;

/** 
 * <h1> App  </h1>
 * <p> This is the "main" class that 
 * launches the program</p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.IOException;

import webCrawler.server.WebThreads;


public class App {
	
	
	public static void main(String[] args) throws IOException {
		
		//Launching the Web Server. 
		WebThreads.init();
		
	}

}
