package webCrawler.server;

/** 
 * <h1> Crawling Threads  </h1>
 * <p> This class is the crawling part 
 * of the server, dedicated to the Server/Explorer communication </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import webCrawler.clientExplorer.ExplorerClient;
import webCrawler.serialisableObjects.SrializableClientAnswer;
import webCrawler.serialisableObjects.SrializableCrawlingAnswer;
import webCrawler.serialisableObjects.SrializableMediaFile;
import webCrawler.serialisableObjects.SrializableTextField;
import webCrawler.xmlManager.XmlGenerator;
import webCrawler.xmlManager.XmlParser;

public class CrawlingThreads implements Runnable {

	private static final int BUFFER_SIZE = 500000;
	private static final int MAX_THREAD_NB = 10;
	private static final int MAX_LINKS = 20499;

	private SrializableTextField startingUrl;
	private static int port;
	BlockingQueue<SrializableCrawlingAnswer> bq;

	public CrawlingThreads(SrializableTextField startingUrl, int port, BlockingQueue<SrializableCrawlingAnswer> bq) {
		super();
		this.startingUrl = startingUrl;
		CrawlingThreads.port = port;
		this.bq = bq;
	}

	@Override
	public void run() {
		SrializableCrawlingAnswer sca = null;

		// boolean starting = true;
		boolean exit = false;

		Selector selector = null;
		ServerSocketChannel ssc = null;

		// Index
		Map<String, List<String>> index = new HashMap<String, List<String>>();

		// Media List
		List<SrializableMediaFile> MediaFileList = new ArrayList<SrializableMediaFile>();

		// URL marking lists
		List<String> urlList_visited = new LinkedList<String>(); // The list to return to user
		List<String> urlList_toVisit = new LinkedList<String>();
		List<String> urlList_current = new LinkedList<String>();
		List<String> urlList_unvalid = new LinkedList<String>();

		// Create and launch the first client
		ExecutorService executor = Executors.newFixedThreadPool(MAX_THREAD_NB);

		try {
			selector = Selector.open();
			ssc = LaunchServer(selector, ssc);
			System.out.println("Crawling Server Launched for " + startingUrl.getText());

			// Verify the if the URL Exists in the history (crawl if not)
			if ((sca = (SrializableCrawlingAnswer) XMLCaller("check", MediaFileList, urlList_unvalid, index,
					startingUrl)) == null) {

				urlList_toVisit.add(startingUrl.getText());
				System.out.println("processing crawling...");

				executor.execute(new ExplorerClient(new Random().nextInt(1000), port));

				while (!exit) {

					selector.select();
					Set<SelectionKey> selectedKeys = selector.selectedKeys();
					Iterator<SelectionKey> i = selectedKeys.iterator();

					while (i.hasNext()) {

						SelectionKey key = i.next();
						if (key.isAcceptable()) {

							// Accept a new explorer client
							acceptExploratorClient(ssc, selector);

						} else if (key.isWritable()) {

							// Extract the next URL to crawl
							String newUrl = extractNextLiveUrl(urlList_toVisit, urlList_visited, urlList_unvalid,
									urlList_current);

							// If not null send the URL
							if (newUrl != "NULL") {
								sendUrl(key, newUrl, selector);
							}

							// Create and launch another ExplorerClient and leave it to wait for data
							executor.execute(new ExplorerClient(new Random().nextInt(1000), port));

						} else if (key.isReadable()) {

							// Receive answer from Explorer client once crawling finished
							readAnswer(key, urlList_toVisit, urlList_visited, urlList_current, index, urlList_unvalid,
									MediaFileList);

//							viewLists(MediaFileList, urlList_unvalid, urlList_toVisit, urlList_visited,
//									urlList_current);

							// All the crawling is finished  
							if ((urlList_toVisit.isEmpty() && urlList_current.isEmpty())
									|| urlList_visited.size() > MAX_LINKS) {
								
								viewLists(MediaFileList, urlList_unvalid, urlList_toVisit, urlList_visited,
										urlList_current);

								// Write in record file
								XMLCaller("push", MediaFileList, urlList_visited, index, startingUrl);

								// Create answer to send to client
								sca = new SrializableCrawlingAnswer(startingUrl.getText(), urlList_visited,
										MediaFileList, index);
								
								exit = true;
								ssc.close();
								bq.put(sca);
								break;
							}
						}
						i.remove();
					}
				}
			} else {

				// The URL requested has already been crawled
				System.out.println("already crawled");
				exit = true;
				bq.put(sca);
				ssc.close();

			}

		} catch (Exception e) {

			e.printStackTrace();
		}

	}

	// ********************************* view

	/**
	 * <h1>viewLists</h1>
	 * <p>
	 * This function displays the status of the marking lists and their content.
	 * </p>
	 * 
	 * @param MediaFileList   -> The list of media files found till now
	 * @param urlList_unvalid -> The list of links removed from crawling list till
	 *                        now
	 * @param urlList_visited -> The list of visited links till now
	 * @param urlList_current -> The list of links in process till now
	 */
	public static void viewLists(List<SrializableMediaFile> MediaFileList, List<String> urlList_unvalid,
			List<String> urlList_toVisit, List<String> urlList_visited, List<String> urlList_current) {
		// System.out.println("Index: [" + index.size() + "] : " + index);
		System.out.println("Lis des medias [" + MediaFileList.size() + "] : " + MediaFileList);
		System.out.println("Lis des liens invalides [" + urlList_unvalid.size() + "] : " + urlList_unvalid);
		System.out.println("Lis des liens à visiter [" + urlList_toVisit.size() + "] : " + urlList_toVisit);
		System.out.println("Lis des liens [" + urlList_visited.size() + "] : " + urlList_visited);
		System.out.println("List des liens in process [" + urlList_current.size() + "] : " + urlList_current);
	}

	// ********************************* XML Managing

	/**
	 * <h1>XMLCaller</h1>
	 * <p>
	 * This function calls XML managing functions with a specific action (push or
	 * check) for either write or read on the XML record file
	 * </p>
	 * 
	 * @param action          -> the action requested (push or check)
	 * @param MediaFileList   -> The list of media files found in the site
	 * @param urlList_visited -> The list of visited links found in the site
	 * @param index           -> The index of words found in the site
	 * @param startingUrl     -> The URL crawled
	 * @return returns an object which is either a string with "file created" (push)
	 *         or a serializableCrawlingAnswer (check) casted Later;
	 * @throws Exception
	 */
	public static Object XMLCaller(String action, List<SrializableMediaFile> MediaFileList,
			List<String> urlList_visited, Map<String, List<String>> index, SrializableTextField startingUrl)
			throws Exception {

		switch (action) {
		case "push":
			new XmlGenerator().writeInXmlFile(startingUrl.getText(), MediaFileList, index, urlList_visited);
			System.out.println("file created");
			return "file created";
		case "check":
			return new XmlParser().getCrawlingAnswerFromXML(startingUrl.getText());
		default:
			return "Unknown action";
		}
	}

	// ********************************* Verify URL

	/**
	 * <h1>extractNextLiveUrl</h1>
	 * <p>
	 * This function provides the next URL in *urlList_toVisit* list:
	 * <ul>
	 * <li>Removes URL from *urlList_toVisit*</li>
	 * <li>If not valid adds it to *urlList_unvalid*</li>
	 * <li>If valid returns is and adds it to *urlList_current*</li>
	 * </ul>
	 * </p>
	 * 
	 * @param urlList_toVisit -> The list of URLs that remain to visit
	 * @param urlList_visited -> The list of visited links
	 * @param urlList_unvalid -> The list of links removed from crawling list
	 * @param urlList_current -> The list of links in process
	 * @return the next URL to crawl, it's guaranteed to be valid and "live".
	 * @throws IOException
	 */
	private static String extractNextLiveUrl(List<String> urlList_toVisit, List<String> urlList_visited,
			List<String> urlList_unvalid, List<String> urlList_current) throws IOException {
		String result;

		while (true) {
			if ((urlList_toVisit.isEmpty()) || urlList_visited.size() > MAX_LINKS)
				return "NULL";

			result = urlList_toVisit.stream().findFirst().get();
			urlList_toVisit.remove(result);
			URL toVerify;
			try {
				toVerify = new URL(result);
			} catch (MalformedURLException e) {
				urlList_unvalid.add(result);
				urlList_unvalid.add("malformedURL");
				continue;
			}
			HttpURLConnection huc = (HttpURLConnection) toVerify.openConnection();
			huc.setRequestMethod("HEAD");
			huc.setInstanceFollowRedirects(true);
			int response = huc.getResponseCode();
			if (response == HttpURLConnection.HTTP_OK)
				break;
			if (response == 429 /* too much requests */ ) {
				urlList_toVisit.add(result);
				continue;
			}

			urlList_unvalid.add(result);
			urlList_unvalid.add(Integer.toString(response));
		}
		urlList_current.add(result);
		return result;
	}

	// ********************************* Sorting

	/**
	 * <h1>sortKeyWordsIntoList</h1>
	 * <p>
	 * This function adds each word from the crawling answer list to the index if
	 * not exists.
	 * </p>
	 * 
	 * @param answer -> An object containing the 3 parts of a crawling answer
	 *               (links, media, and words)
	 * @param index  -> The index of words to return to user
	 */
	private static void sortKeyWordsIntoList(SrializableClientAnswer answer, Map<String, List<String>> index) {
		for (String word : answer.getKeyWordsList()) {
			if (!index.containsKey(word))
				index.put(word, new ArrayList<String>());
			if (!index.get(word).contains(answer.getOrigin()))
				index.get(word).add(answer.getOrigin());
		}
	}

	/**
	 * <h1>sortUrlsIntoList</h1>
	 * <p>
	 * This function adds each URL returned by the explorer into marking lists
	 * </p>
	 * 
	 * @param answer          An object containing the 3 parts of a crawling answer
	 *                        (links, media, and words)
	 * @param urlList_visited -> The list of visited links till now
	 * @param urlList_current -> The list of links in process till now
	 * @param urlList_toVisit -> The list of URLs that remain to visit
	 * @param urlList_unvalid -> The list of links removed from crawling list till
	 *                        now
	 */
	private static void sortUrlsIntoList(SrializableClientAnswer answer, List<String> urlList_visited,
			List<String> urlList_current, List<String> urlList_toVisit, List<String> urlList_unvalid) {
		for (String element : answer.getUrlList()) {
			if (!urlList_visited.contains(element) && !urlList_toVisit.contains(element)
					&& !urlList_current.contains(element) && !urlList_unvalid.contains(element)) {
				urlList_toVisit.add(element);

			}
		}
	}

	/**
	 * <h1>sortMediasIntoList</h1>
	 * <p>
	 * This function adds the media file returned by the explorer into Media file
	 * list
	 * </p>
	 * 
	 * @param smf           -> Media file object returned by the explorer
	 * @param MediaFileList -> The list of media files found till now
	 * @return The updated MediaFile List
	 */
	private static boolean sortMediasIntoList(SrializableMediaFile smf, List<SrializableMediaFile> MediaFileList) {
		for (SrializableMediaFile srializableMediaFile : MediaFileList) {
			if (!srializableMediaFile.getName().equals(smf.getName()))
				continue;
			else
				return false;
		}
		MediaFileList.add(smf);
		return true;
	}

	// ********************************* Server-Explorer Protocol

	/**
	 * <h1>LaunchServer</h1>
	 * <p>
	 * This function bind the server socket channel to the Port created in
	 * constructor.
	 * </p>
	 * <p>
	 * It registers the ACCEPT option for the SSC.
	 * </p>
	 * 
	 * @param selector -> The explorers selector
	 * @param ssc      -> The server socket in which accept explorer clients
	 * @return The updated server socket channel
	 * @throws IOException
	 */
	public static ServerSocketChannel LaunchServer(Selector selector, ServerSocketChannel ssc) throws IOException {

		ssc = ServerSocketChannel.open();
		ssc.configureBlocking(false);
		ssc.socket().bind(new InetSocketAddress(port));
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		return ssc;
	}

	/**
	 * <h1>acceptExploratorClient</h1>
	 * <p>
	 * This function accepts an explorer client (in a non blocking socket)
	 * </p>
	 * <p>
	 * It registers the WRITE option for the SSC.
	 * </p>
	 * 
	 * @param ssc      -> The server socket in which accept explorer clients
	 * @param selector -> The explorers selector
	 * @throws IOException
	 */
	private static void acceptExploratorClient(ServerSocketChannel ssc, Selector selector) throws IOException {
		SocketChannel sc = ssc.accept();
		sc.configureBlocking(false);
		sc.register(selector, SelectionKey.OP_WRITE);
	}

	/**
	 * <h1>sendUrl</h1>
	 * <p>
	 * This function sends an URL to the selected Client
	 * </p>
	 * 
	 * @param key      -> The selected key
	 * @param url      -> The URL to send to explorer
	 * @param selector -> The explorers selector
	 * @throws IOException
	 */
	public static void sendUrl(SelectionKey key, String url, Selector selector) throws IOException {
		SocketChannel sc = (SocketChannel) key.channel();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(new SrializableTextField(url));
		oos.flush();
		sc.write(ByteBuffer.wrap(baos.toByteArray()));
		sc.register(selector, SelectionKey.OP_READ);
	}

	/**
	 * <h1>readAnswer</h1>
	 * <p>
	 * This function reads the answer from explorer client
	 * </p>
	 * 
	 * @param key           -> The selected key
	 * @param toVisit       -> The list of URLs that remain to visit
	 * @param visited       -> The list of visited links
	 * @param current       -> The list of links in process till now
	 * @param index         -> The index of words to return to user
	 * @param unvalid       -> The list of links removed from crawling list till now
	 * @param mediaFileList -> The list of media files found till now
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	private static void readAnswer(SelectionKey key, List<String> toVisit, List<String> visited, List<String> current,
			Map<String, List<String>> index, List<String> unvalid, List<SrializableMediaFile> mediaFileList)
			throws IOException, ClassNotFoundException {

		SocketChannel sc = (SocketChannel) key.channel();
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		sc.read(buffer);
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
		ObjectInputStream ois = new ObjectInputStream(bais);

		if (ReceiveHint(ois).getText().equals("lists")) {
			ReceiveClientAnswer(ois, visited, current, toVisit, unvalid, index);
		} else {
			ReceiveMediaFile(ois, current, mediaFileList);
		}

		sc.close();

	}

	/**
	 * <h1>ReceiveHint</h1>
	 * <p>
	 * This function receives a hint to select which Type of object The server is
	 * about to receive (SrializableMediaFile or SrializableClientAnswer
	 * </p>
	 * 
	 * @param ois -> ObjectInputStream
	 * @return The hint (lists or media)
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static SrializableTextField ReceiveHint(ObjectInputStream ois) throws IOException, ClassNotFoundException {
		SrializableTextField result = (SrializableTextField) ois.readObject();
		return result;
	}

	/**
	 * <h1>ReceiveClientAnswer</h1>
	 * <p>
	 * This function receive the client answer in the case of HTML page
	 * </p>
	 * 
	 * @param ois             -> ObjectInputStream
	 * @param urlList_visited -> The list of visited links
	 * @param urlList_current -> The list of links in process
	 * @param urlList_toVisit -> The list of URLs that remain to visit
	 * @param unvalid         -> The list of links removed from crawling list
	 * @param index           -> The index of words to return to user
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void ReceiveClientAnswer(ObjectInputStream ois, List<String> urlList_visited,
			List<String> urlList_current, List<String> urlList_toVisit, List<String> unvalid,
			Map<String, List<String>> index) throws IOException, ClassNotFoundException {
		SrializableClientAnswer answer = (SrializableClientAnswer) ois.readObject();
		urlList_visited.add(answer.getOrigin());
		urlList_current.remove(answer.getOrigin());
		sortUrlsIntoList(answer, urlList_visited, urlList_current, urlList_toVisit, unvalid);
		sortKeyWordsIntoList(answer, index);

	}

	/**
	 * <h1>ReceiveMediaFile</h1>
	 * <p>
	 * This function receive the client answer in the case of media file
	 * </p>
	 * 
	 * @param ois             -> ObjectInputStream
	 * @param urlList_current -> The list of links in process
	 * @param mediaFileList   -> The list of media files found
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static void ReceiveMediaFile(ObjectInputStream ois, List<String> urlList_current,
			List<SrializableMediaFile> mediaFileList) throws IOException, ClassNotFoundException {
		SrializableMediaFile smf = (SrializableMediaFile) ois.readObject();
		sortMediasIntoList(smf, mediaFileList);
		urlList_current.remove(smf.getName());

	}

}
