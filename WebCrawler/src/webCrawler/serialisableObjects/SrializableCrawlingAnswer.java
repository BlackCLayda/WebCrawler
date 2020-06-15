package webCrawler.serialisableObjects;

/** 
 * <h1> Serializable Crawling Answer </h1>
 * <p> This class creates a serializable version of a 
 * data structure that contains an URL, the list of crawled URLs, 
 * media file found in the web side, the index of words mapped as 
 * <word, list of links in which the word appears>
 * It is used by the Server to respond to the User </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.Serializable;
import java.util.List;
import java.util.Map;

public class SrializableCrawlingAnswer implements Serializable {

	private static final long serialVersionUID = 1L;

	private String name;
	private List<String> urlList;
	private List<SrializableMediaFile> mediaFileList;
	private Map<String, List<String>> index;

	// Constructor
	public SrializableCrawlingAnswer(String name, List<String> urlList, List<SrializableMediaFile> mediaFileList,
			Map<String, List<String>> index) {
		super();
		this.name = name;
		this.urlList = urlList;
		this.mediaFileList = mediaFileList;
		this.index = index;
	}

	// Getters
	public String getName() {
		return name;
	}

	public List<String> getUrlList() {
		return urlList;
	}

	public List<SrializableMediaFile> getMediaFileList() {
		return mediaFileList;
	}

	public Map<String, List<String>> getIndex() {
		return index;
	}

	// toString
	public String toString() {
		return "URL =" + name + ":\n urlList=" + urlList + "\n mediaFileList=" + mediaFileList
				+ "\n index= well formed -_-";
	}

}
