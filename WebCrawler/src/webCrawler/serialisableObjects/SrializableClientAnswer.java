package webCrawler.serialisableObjects;

/** 
 * <h1> Serializable Client Answer </h1>
 * <p> This class creates a serializable version of a 
 * data structure that contains a list of words, URLs,
 * and the crawled URL. It is used by the Explorer client 
 * to respond to the server </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.Serializable;
import java.util.List;

public class SrializableClientAnswer implements Serializable {

	private static final long serialVersionUID = 1L;
	List<String> urlList;
	List<String> keyWordsList;
	String origin;

	// Constructor
	public SrializableClientAnswer(List<String> urlList, List<String> keyWordsList, String origin) {
		this.urlList = urlList;
		this.keyWordsList = keyWordsList;
		this.origin = origin;
	}

	// Getters
	public String getOrigin() {
		return origin;
	}

	public List<String> getUrlList() {
		return urlList;
	}

	public List<String> getKeyWordsList() {
		return keyWordsList;
	}

	// toString
	public String toString() {
		return "Data recieved from client: \n- urlList: =" + urlList + "\n- keyWordsList=" + keyWordsList;
	}

}
