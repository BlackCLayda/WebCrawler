package webCrawler.server;

/** 
 * <h1> Web Threads </h1>
 * <p> This class is the web part of the server, 
 * dedicated to the Server/User communication </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import webCrawler.clientExplorer.Explorer;
import webCrawler.serialisableObjects.SrializableCrawlingAnswer;
import webCrawler.serialisableObjects.SrializableMediaFile;
import webCrawler.serialisableObjects.SrializableTextField;
import webCrawler.xmlManager.XmlParser;

public class WebThreads implements Runnable {
	static final String DEFAULT_FILE = "mainPage.html";
	static final String FILE_NOT_FOUND = "404.html";
	static final String ANSWER_FILE_URL = ".urlResponsePage.html";
	static final String ANSWER_FILE_WORD = ".wordResponsePage.html";

	static final int PORT = 8080; // for the web part of the server
	static int validPort = PORT; // for the crawling part of the server (will be calculated every time)

	private static Socket s;
	BlockingQueue<SrializableCrawlingAnswer> bq;

	public WebThreads(Socket socket, BlockingQueue<SrializableCrawlingAnswer> blockq) {
		s = socket;
		bq = blockq;
	}

	/**
	 * <h1> init </h1>
	 * <p>
	 * This function initiate the web Server, accepts users, 
	 * and open an new thread for each user 
	 * </p>
	 * 
	 * @throws IOException
	 */
	public static void init() throws IOException {

		BlockingQueue<SrializableCrawlingAnswer> bq = new LinkedBlockingQueue<SrializableCrawlingAnswer>();
		@SuppressWarnings("resource")
		ServerSocket ss = new ServerSocket(PORT);

		System.out.println("Web Crawler Server started.\n- The serve is listening on port " + PORT);
		while (true) {

			// A new client is accepted
			Socket client = ss.accept();
			System.out.println("client accepted on " + client);

			// Start new Thread for the client.
			Thread t = new Thread(new WebThreads(client, bq));
			t.start();
		}
	}

	@Override
	public void run() {

		Socket mySocket = s;
		String httpQandA = null;
		Boolean anUrlHasBeenCrawled = false;
		SrializableCrawlingAnswer sca = null;
		String specific_ANSWER_FILE_URL = null;
		String specific_ANSWER_FILE_WORD = null;

		while (true) {

			try {

				//Get the request from client 
				List<String> request;
				request = getHttpQ(mySocket);

				//Pass if empty
				if (request == null)
					continue;

				httpQandA = request.get(1);

				//Test if the User asked for a URL crawling
				if (httpQandA.contains("?url=")) {
					String urlToCrawl = treatUrl(httpQandA);
					if (verifyLiveUrl(urlToCrawl)) {
						specific_ANSWER_FILE_URL = createSpecificAnswerFiles(urlToCrawl, specific_ANSWER_FILE_URL,
								ANSWER_FILE_URL);
						specific_ANSWER_FILE_WORD = createSpecificAnswerFiles(urlToCrawl, specific_ANSWER_FILE_WORD,
								ANSWER_FILE_WORD);
						sca = crawlUrl(urlToCrawl);
						httpQandA = createPageToReturn(sca, specific_ANSWER_FILE_URL);
						anUrlHasBeenCrawled = true;
					} else
						httpQandA = createPageToReturn("This URL is unvalid", "UrlNotValid.html", 1);

				}
				
				//Test if the User asked for a word to search
				if (httpQandA.contains("?wordtosearch=")) {

					httpQandA = treatWordRequest(httpQandA, sca, specific_ANSWER_FILE_URL, specific_ANSWER_FILE_WORD,
							anUrlHasBeenCrawled);

				}
				
				
				//Test if the User pressed the reset button
				if (httpQandA.contains("?reset")) {

					httpQandA = DEFAULT_FILE;
					anUrlHasBeenCrawled = false;
				}

				//Test if the method is not a get method
				if (!request.get(0).equals("GET")) {
					httpQandA = FILE_NOT_FOUND;
				} else {
					
					//Test if the user requested the main page
					if (httpQandA.endsWith("/")) {
						httpQandA = DEFAULT_FILE;
					}
					
					//Here httpQandA contains the right page to return
					sendPage(httpQandA, mySocket);
				}

			} catch (IOException | InterruptedException e) {
				try {
					sendPage(FILE_NOT_FOUND, mySocket);
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			} finally {
				try {
//					bufferReader.close();
//					printWriter.close();
//					mySocket.close();
				} catch (Exception e) {
				}

			}

		}

	}

	// ************* Function Related to User/Server communication

	/**
	 * <h1> sendPage </h1>
	 * <p>
	 * This function send an HTML page to the User.
	 * </p>
	 * @param answerFile -> The html page to send
	 * @param mySocket -> The socket in which the user is connected
	 * @throws IOException
	 */
	private void sendPage(String answerFile, Socket mySocket) throws IOException {
		BufferedOutputStream dataOut = new BufferedOutputStream(mySocket.getOutputStream());

		PrintWriter out = new PrintWriter(mySocket.getOutputStream());
		File file = new File(answerFile);
		int fileLength = (int) file.length();
		String content = "text/html";
		byte[] fileData = readFileData(file, fileLength);

		out.println("HTTP/1.1");
		out.println("Server: Java HTTP Lilia / Naima : 1.0");
		out.println("Date: " + new Date());
		out.println("Content-type: " + content);
		out.println("Content-length:" + fileLength);
		out.println(); // Never remove this line !
		out.flush();

		dataOut.write(fileData, 0, fileLength);
		dataOut.flush();

		System.out.println("File " + answerFile + " returnd");

	}

	/**
	 * <h1> updateFileContent </h1>
	 * <p>
	 * This function update a file with added modifications
	 * </p>
	 * 
	 * @param doc -> The document created from the HTML previous page
	 * @param outputFile -> The HTML file to send back
	 * @throws IOException
	 */
	private void updateFileContent(Element doc, String outputFile) throws IOException {
		String newhtml = doc.html();
		BufferedWriter htmlWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outputFile)));
		htmlWriter.write(newhtml);
		htmlWriter.close();

	}

	/**
	 * <h1> getHttpQ </h1>
	 * <p>
	 * This function gets user requests
	 * </p>
	 * 
	 * @param mySocket -> The socket in which the user is connected
	 * @return the user request as an array containing [0] the method, [1] parameters 
	 * @throws IOException
	 */
	private List<String> getHttpQ(Socket mySocket) throws IOException {
		BufferedReader bufferReader = new BufferedReader(new InputStreamReader(mySocket.getInputStream()));
		String input = bufferReader.readLine();
		if (input == null) {
			return null;
		}
		StringTokenizer stringTokenizer = new StringTokenizer(input);
		List<String> str = new ArrayList<String>();
		str.add(stringTokenizer.nextToken().toUpperCase());
		str.add(stringTokenizer.nextToken().toLowerCase());

		return str;

	}

	// ************* Function Related to URL Crawling

	/**
	 * <h1> createPageToReturn </h1>
	 * <p>
	 * This function modifies the HTML page and adds URL crawling answer.
	 * It calls the functions addUrlsToDoc, addfilesToDoc and updateFileContent.
	 * </p>
	 * 
	 * @param sca -> The Crawling answer for the URL requested 
	 * @param specific_ANSWER_FILE_URL -> The specific answer file to return with url answer
	 * @return the page to be sent back to user
	 * @throws IOException
	 */
	private String createPageToReturn(SrializableCrawlingAnswer sca, String specific_ANSWER_FILE_URL)
			throws IOException {
		String newContentHtml = new Explorer().readFile(DEFAULT_FILE);
		Document doc = Jsoup.parse(newContentHtml);
		addUrlsToDoc(doc, sca);
		addfilesToDoc(doc, sca);
		updateFileContent(doc, specific_ANSWER_FILE_URL);
		return specific_ANSWER_FILE_URL;

	}

	/**
	 * <h1> addfilesToDoc </h1>
	 * <p>
	 * This function appends Media file to the anwser URL page
	 * </p>
	 * 
	 * @param doc -> The document created from the previous.  
	 * @param sca -> The Crawling answer for the URL requested
	 */
	private void addfilesToDoc(Document doc, SrializableCrawlingAnswer sca) {

		Element mediaResponseElmt = doc.body().select("div").get(2).getElementById("table");

		doc.createElement("h3").text("List of media files found:" + sca.getMediaFileList().size())
				.appendTo(mediaResponseElmt);

		Element TableHeaderElement = doc.createElement("tr").appendTo(mediaResponseElmt);
		doc.createElement("td").text("Link").appendTo(TableHeaderElement);
		doc.createElement("td").text("Type").appendTo(TableHeaderElement);
		doc.createElement("td").text("Size").appendTo(TableHeaderElement);

		for (SrializableMediaFile media : sca.getMediaFileList()) {
			Element mediaElement = doc.createElement("tr").appendTo(mediaResponseElmt);

			Element mediaLink = doc.createElement("td").appendTo(mediaElement);
			doc.createElement("a").text(media.getName()).attr("href", media.getName()).appendTo(mediaLink);
			if (media.getType().indexOf(";") != -1)
				doc.createElement("td")
						.text(media.getType().substring(media.getType().indexOf("/") + 1, media.getType().indexOf(";")))
						.appendTo(mediaElement);
			else
				doc.createElement("td").text(media.getType().substring(media.getType().indexOf("/") + 1))
						.appendTo(mediaElement);

			doc.createElement("td").text(Convert((int) Math.round(media.getSize()))).appendTo(mediaElement);

		}

	}

	/**
	 * <h1> addUrlsToDoc </h1>
	 * <p>
	 * This function appends URLs to the anwser URL page
	 * </p>
	 * 
	 * @param doc -> The document created from the HTML page to send back 
	 * @param sca -> The Crawling answer for the URL requested
	 */
	private void addUrlsToDoc(Document doc, SrializableCrawlingAnswer sca) {
		Element urlsResponseElmt = doc.body().select("div").get(1);
		doc.createElement("h3").text("List of URL found for [" + sca.getName() + "] : " + sca.getUrlList().size())
				.appendTo(urlsResponseElmt);
		for (String url : sca.getUrlList()) {
			doc.createElement("a").text(url).attr("href", url).appendTo(urlsResponseElmt);
			doc.createElement("br").appendTo(urlsResponseElmt);
		}

	}

	/**
	 * <h1> verifyLiveUrl </h1>
	 * <p>
	 * This function check if the URL entered by user is a live URL, 
	 * by testing the HTTPUrlConnexion 
	 * </p>
	 * 
	 * @param urlToCrawl -> The URL requested
	 * @return true, if the requested URL is valid, or false otherwise
	 * @throws MalformedURLException
	 */
	private boolean verifyLiveUrl(String urlToCrawl) throws MalformedURLException {
		URL url = new URL(urlToCrawl);
		try {
			HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
			if (httpURLConnection.getResponseCode() == HttpURLConnection.HTTP_OK)
				return true;
		} catch (IOException e) {
			return false;
		}

		return false;
	}

	/**
	 * <h1> treatUrl </h1>
	 * <p>
	 * This function treat the user request and extract the url to crawl
	 * </p>
	 * 
	 * @param httpQandA -> the user request
	 * @return The requested URL
	 * @throws UnsupportedEncodingException
	 */
	private String treatUrl(String httpQandA) throws UnsupportedEncodingException {
		String urltoCrawl = httpQandA.substring(httpQandA.indexOf("?url=") + 5);
		String url = URLDecoder.decode(urltoCrawl, StandardCharsets.UTF_8.toString());
		return url;
	}

	/**
	 * <h1> crawlUrl </h1>
	 * <p>
	 * This function calls the Crawling threads 
	 * with a valid port and the URL requested
	 * </p>
	 * 
	 * @param urlToCrawl -> the URL requested
	 * @return The crawling answer for the requested URL
	 * @throws InterruptedException
	 */
	private SrializableCrawlingAnswer crawlUrl(String urlToCrawl) throws InterruptedException {
		// Find a valid port
		validPort = nextValidPort(validPort + 1);
		System.out.println(validPort);

		// OpenTheServer with this port
		CrawlingThreads server = new CrawlingThreads(new SrializableTextField(urlToCrawl), validPort, bq);
		Thread thread = new Thread(server);
		thread.start();

		SrializableCrawlingAnswer sca;
		while (true) {
			sca = bq.take();
			if (sca.getName().equals(urlToCrawl))
				break;
			else
				bq.put(sca);
		}

		System.out.println("Server finnished crawling " + urlToCrawl);
		// Get and return server ANSWER
		return sca;
	}
	
	// ************* Function Related to word Searching


	/**
	 * <h1> treatWordRequest </h1>
	 * <p>
	 * This function search for a given word in the crawled site 
	 * </p>
	 * 
	 * @param httpQandA -> the URL requested
	 * @param sca -> The Crawling answer for the URL requested
	 * @param specific_ANSWER_FILE_URL -> The specific answer file to return with url answer
	 * @param specific_ANSWER_FILE_WORD -> The specific answer file to return with word answer
	 * @param anUrlHasBeenCrawled -> A boolean variable which says if the user already crawled 
	 * an url before searching for a word. 
	 * @return the page to be sent back to user
	 * @throws IOException
	 */
	private String treatWordRequest(String httpQandA, SrializableCrawlingAnswer sca, String specific_ANSWER_FILE_URL,
			String specific_ANSWER_FILE_WORD, boolean anUrlHasBeenCrawled) throws IOException {
		if (!anUrlHasBeenCrawled) {
			return createPageToReturn("You have to crawl an URL first", "UrlNotSearched.html", 4);
		} else {
			String wordToSearch = treatWord(httpQandA);
			List<String> linksOfWord = new XmlParser().SearchWord(wordToSearch, sca.getIndex());
			return createPageToReturn(wordToSearch, linksOfWord, specific_ANSWER_FILE_WORD, specific_ANSWER_FILE_URL);
		}
	}

	/**
	 * <h1> createPageToReturn </h1>
	 * <p>
	 * This function modifies the HTML page and adds word answer.
	 * It calls the function updateFileContent.
	 * </p>
	 * 
	 * @param wordToSearch -> the requested word
	 * @param linksOfWord -> The list of links in which the word has been found
	 * @param outputFile -> The file to read an modify
	 * @param inputFile -> The file in which write modifications
	 * @return the page to be sent back to user
	 * @throws IOException
	 */
	private String createPageToReturn(String wordToSearch, List<String> linksOfWord, String outputFile,
			String inputFile) throws IOException {
		String newContentHtml = new Explorer().readFile(inputFile);
		Document doc = Jsoup.parse(newContentHtml);
		Element wordResponseElmt = doc.body().select("div").get(4);

		if (linksOfWord != null) {
			doc.createElement("h3").text("Result for [" + wordToSearch + "] :").appendTo(wordResponseElmt);
			for (String url : linksOfWord) {
				Element urlElement = doc.createElement("a");
				urlElement.text(url);
				urlElement.attr("href", url);
				Element newLine = doc.createElement("br");
				urlElement.appendTo(wordResponseElmt);
				newLine.appendTo(wordResponseElmt);
			}
		} else {
			Element error = doc.createElement("h3");
			error.text("Word not found");
			error.appendTo(wordResponseElmt);
		}
		updateFileContent(doc, outputFile);
		return outputFile;
	}

	/**
	 * <h1> createPageToReturn </h1>
	 * <p>
	 * This function modifies the HTML page and adds a message.
	 * It calls the function updateFileContent.
	 * </p>
	 * 
	 * @param message -> The message to add to the HTML page to return
	 * @param outputFile -> The file to read an modify
	 * @param number ->  The DIV position in which perform changes
	 * @return the page to be sent back to user
	 * @throws IOException
	 */
	private String createPageToReturn(String message, String outputFile, int number) throws IOException {
		String newContentHtml = new Explorer().readFile(DEFAULT_FILE);
		Document doc = Jsoup.parse(newContentHtml);
		Element urlsResponseElmt = doc.body().select("div").get(number);
		doc.createElement("h3").text(message).appendTo(urlsResponseElmt);

		updateFileContent(doc, outputFile);
		return outputFile;
	}

	/**
	 * <h1> createSpecificAnswerFiles </h1>
	 * <p>
	 * This function creates answer files that are specific
	 * to the URL requested, to avoid conflicts
	 * </p>
	 * 
	 * @param urlToCrawl -> The URL requested
	 * @param specificFile -> The spesific file to create
	 * @param globalFile -> The template to add to the specific file
	 * @return the specific file named after the requested URL
	 * @throws IOException
	 */
	private String createSpecificAnswerFiles(String urlToCrawl, String specificFile, String globalFile)
			throws IOException {
		String urlToCrawl_host = new URL(urlToCrawl).getHost();

		int index;
		String webSiteName = urlToCrawl_host;
		if ((index = urlToCrawl_host.indexOf("www.")) != -1) {
			webSiteName = urlToCrawl_host.substring(index + 4);
		}

		webSiteName = webSiteName.substring(0, webSiteName.lastIndexOf("."));

		// , urlToCrawl_host.lastIndexOf(".")
		return specificFile = webSiteName + globalFile;

	}

	/**
	 * <h1> treatWord </h1>
	 * <p>
	 * This function treat the user request and extract the word to search
	 * </p>
	 * 
	 * @param httpQandA -> the user request
	 * @return The requested word
	 */
	private String treatWord(String httpQandA) {
		int start = httpQandA.indexOf("?wordtosearch=") + 14;
		return httpQandA.substring(start);
	}

	// ************* Other useful functions

	/**
	 * <h1> readFileData </h1>
	 * <p>
	 * This function reads a file and return a Byte Array ready to send to user. 
	 * </p>
	 * 
	 * @param file -> The file to read
	 * @param fileLength -> The size of the file to read
	 * @return A byte array containing the file to return 
	 * @throws IOException
	 */
	private byte[] readFileData(File file, int fileLength) throws IOException {
		FileInputStream fileIn = null;
		byte[] fileData = new byte[fileLength];

		try {
			fileIn = new FileInputStream(file);
			fileIn.read(fileData);
		} finally {
			if (fileIn != null)
				fileIn.close();
		}

		return fileData;
	}

	/**
	 * <h1> nextValidPort </h1>
	 * <p>
	 * This function test ports to get a valid port to create 
	 * the crawlingThread ServerSocketChannel
	 * </p>
	 * 
	 * @param port -> The port to start with
	 * @return A valid port (unused by another app) 
	 */
	private int nextValidPort(int port) {
		ServerSocket test = null;

		while (true) {
			try {
				test = new ServerSocket(port);
			} catch (Exception e) {
				port++;
				continue;
			}

			// Here the port is unused
			try {
				test.close();
				return port;
			} catch (IOException e2) {
			}

		}

	}

	/**
	 * <h1> Convert </h1>
	 * <p>
	 * This function converts file size to Ko, Mo or Go
	 * </p>
	 * 
	 * @param size -> The file size
	 * @return A string containing the converted size with the right unit
	 */
	public static String Convert(int size) {
		String[] unit = { "o", "Ko", "Mo", "Go" };

		if (size == -1)
			return "Size Not Found";

		String str = "";
		int[] detail = new int[4];
		int i = 0;
		do {
			detail[i] = size % 1024;
			size /= 1024;
			i++;
		} while (size >= 1024 && i < 4);
		detail[i] = size;
		for (int k = detail.length - 1; k >= 0; k--) {
			if (detail[k] != 0) {
				str += (detail[k] + "," + detail[k - 1] + " " + unit[k]);
				break;
			}
		}
		return str;
	}

}
