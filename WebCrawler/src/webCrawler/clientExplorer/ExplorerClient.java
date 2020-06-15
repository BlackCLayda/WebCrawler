package webCrawler.clientExplorer;

/** 
 * <h1> Explorer Client </h1>
 * <p> This class communicates and receive orders (URLs) 
 * From server to explore and returs Data related the the given link </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import webCrawler.serialisableObjects.SrializableClientAnswer;
import webCrawler.serialisableObjects.SrializableMediaFile;
import webCrawler.serialisableObjects.SrializableTextField;

public class ExplorerClient implements Runnable {

	private int id;
	private int port;
	private static final int BUFFER_SIZE = 4024;

	//Constructor
	public ExplorerClient(int id, int port) {

		this.id = id;
		this.port = port;
	}
	
	@Override 
	public void run() {
		SocketChannel sc;
		try {
			sc = SocketChannel.open();
			sc.configureBlocking(true);
			if (sc.connect(new InetSocketAddress("localhost", port))) {

				// Receive the URL to crawl
				SrializableTextField url = ReceiveURL(sc);

				// Call the explorer to crawl and get the answer
				Explorer ex = new Explorer();

				if (ex.getUrlType(url.getText()).startsWith("text/html")) {
					String html = ex.getSourceCode(url.getText(), id);
					sendData(new SrializableClientAnswer(ex.getLinks(html, url), ex.getkeyWords(html), url.getText()),
							sc);
				} else
					SendData(new SrializableMediaFile(url.getText(), ex.getUrlType(url.getText()),
							ex.getMediaSize(url.getText())), sc);

			}

			sc.close();
		} catch (IOException | ClassNotFoundException | InterruptedException e) {
			e.printStackTrace();
		}

	}

	//************************* Implement Server/Explorer communication methods
	
	/**
	 * <h1> ReceiveURL </h1>
	 * <p> This function receives an URL (order) from server to crawl </p> 
	 * 
	 * @param sc -> The socketChannel in which the client if connected to server 
	 * @return The URL received from Server 
	 * @throws IOException
	 * @throws ClassNotFoundException
	 */
	public static SrializableTextField ReceiveURL(SocketChannel sc) throws IOException, ClassNotFoundException {
		ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
		sc.read(buffer);
		ByteArrayInputStream bais = new ByteArrayInputStream(buffer.array());
		ObjectInputStream ois = new ObjectInputStream(bais);
		SrializableTextField url = (SrializableTextField) ois.readObject();
		ois.close();
		return url;
	}

	/**
	 * <h1> sendData </h1>
	 * <p> This function send back the result to server
	 * In the case of HTML page </p> 
	 * 
	 * @param answer -> The answer to send to server 
	 * in case of HTML page (contains the list of links and words)
	 * @param sc -> The socketChannel in which the client if connected to server 
	 * @throws IOException
	 */
	
	public static void sendData(SrializableClientAnswer answer, SocketChannel sc) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(new SrializableTextField("lists"));
		oos.writeObject(answer);
		oos.flush();
		sc.write(ByteBuffer.wrap(baos.toByteArray()));
		oos.close();
	}

	/**
	 * <h1> SendData </h1>
	 * <p> This function send back the result to server
	 * In the case of media file </p> 
	 * 
	 * @param smf -> The answer to send to server
	 * in the case of media file (contains the media URL, type and size)
	 * @param sc -> The socketChannel in which the client if connected to server 
	 * @throws IOException
	 */
	public static void SendData(SrializableMediaFile smf, SocketChannel sc) throws IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(baos);
		oos.writeObject(new SrializableTextField("Media"));
		oos.writeObject(smf);
		oos.flush();
		sc.write(ByteBuffer.wrap(baos.toByteArray()));
		oos.close();
	}

}
