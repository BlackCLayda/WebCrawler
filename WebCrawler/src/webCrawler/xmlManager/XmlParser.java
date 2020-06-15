/* 
 * <h1> XML Parser </h1>
 * <p> This class parse an XML file containing already crawled lists
 * and get back all data structures from it </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */
package webCrawler.xmlManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import webCrawler.serialisableObjects.SrializableCrawlingAnswer;
import webCrawler.serialisableObjects.SrializableMediaFile;

public class XmlParser {

	private static DocumentBuilder builder;
	private static File recordXml = new File("crawlingHistory.xml");
	private static Document doc;
	
	/**
	 * <h1>getCrawlingAnswerFromXML</h1>
	 * <p>
	 * This function initiate the XML parsing of the record file and
	 * call all others functions. first search for the giving URL:  
	 * <ul>
	 * <li> if URL has never been crawled returns null</li>
	 * <li> else, it construct SrializableCrawlingAnswer from URL's Data</li>
	 * </ul>
	 * </p>
	 * @param url -> entered by the user to verify
	 * @return SrializableCrawlingAnswer from URL's Data if exists
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public SrializableCrawlingAnswer getCrawlingAnswerFromXML(String url) throws ParserConfigurationException, SAXException, IOException {
		if(!recordXml.exists()) {
			return null;
		}
		builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		doc = builder.parse(recordXml);
		Element urlNode;
		if ((urlNode = getNodeFromUrl(url)) ==null) {
			return null; //The URL don't exist in the file and should be crawled
		}	
		
		//The URL've already been crawled return data
		return new SrializableCrawlingAnswer(url, getUrlList(urlNode), getMediaFileList(urlNode), getIndex(urlNode));
	}
	

	/**
	 * <h1>getNodeFromUrl</h1>
	 * <p>
	 * This function search for an URL in the crawled history
	 * returns the Node, (casted as Element) named after this URL
	 * returns null if not exists
	 * </p>
	 * 
	 * @param url -> the URL to search in the crawled history
	 * @return The Node (casted as Element) named after this URL
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Element getNodeFromUrl(String url) throws ParserConfigurationException, SAXException, IOException {
		String urlToCheck = new URL(url).getHost();
		NodeList crawledUrlNodeList = doc.getDocumentElement().getChildNodes();
	
		for (int i = 0; i < crawledUrlNodeList.getLength(); i++) {
			Node urlNode = crawledUrlNodeList.item(i);
			String urlcrawled = new URL(urlNode.getAttributes().getNamedItem("name").getNodeValue()).getHost();
			if (urlcrawled.equals(urlToCheck)) {
				return ((Element) urlNode);
			}
		}
		
		return null;
	}
	
	
	/**
	 * <h1>getUrlList</h1>
	 * <p> This function gets the URLs list crawled from the root URL
	 * </p>
	 * 
	 * @param crawledUrl -> This is the crawled URL contained in crawling history
	 * @return The list crawled from the root URL
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<String> getUrlList(Element crawledUrl) throws ParserConfigurationException, SAXException, IOException {
		List<String> result = new LinkedList<String>();
		NodeList urlList2 = crawledUrl.getChildNodes().item(0).getChildNodes();
		for (int i = 0; i < urlList2.getLength(); i++) {
			String url = urlList2.item(i).getTextContent();
			result.add(url);
		}
		return result;
	}

	/**
	 * <h1>getMediaFileList</h1>
	 * <p> This function gets the media File list from the site
	 * </p>
	 * 
	 * @param url  -> This is the crawled URL contained in crawling history
	 * @return the list of media files contained in the root URL
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static List<SrializableMediaFile> getMediaFileList(Element url)
			throws ParserConfigurationException, SAXException, IOException {
		List<SrializableMediaFile> result = new ArrayList<SrializableMediaFile>();
		NodeList mediaFileList = url.getChildNodes().item(1).getChildNodes();
		for (int i = 0; i < mediaFileList.getLength(); i++) {
			Node mediaFile = mediaFileList.item(i);
			String name = mediaFile.getAttributes().getNamedItem("name").getNodeValue();
			String type = mediaFile.getChildNodes().item(0).getTextContent();
			String sizeString = mediaFile.getChildNodes().item(1).getTextContent();
			int sizeInt = (int) Double.parseDouble(sizeString);

			result.add(new SrializableMediaFile(name, type, sizeInt));
		}
		return result;
	}

	/**
	 * <h1>getIndex</h1>
	 * <p> This function gets the index of words in the site mapped as 
	 * <word, list of links in which the word appears>
	 * </p>
	 * 
	 * @param url -> This is the crawled URL contained in crawling history
	 * @return the index of words in the site
	 * @throws ParserConfigurationException
	 * @throws SAXException
	 * @throws IOException
	 */
	public static Map<String, List<String>> getIndex(Element url)
			throws ParserConfigurationException, SAXException, IOException {	
		Map<String, List<String>> result = new HashMap<String, List<String>>();
		
		NodeList index = url.getChildNodes().item(2).getChildNodes();
		for (int i = 0; i < index.getLength(); i++) {
			Node mot = index.item(i);
			String name = mot.getAttributes().getNamedItem("name").getNodeValue();
			NodeList urlList = mot.getChildNodes();
			result.put(name, new ArrayList<String>());
			for (int j = 0; j < urlList.getLength(); j++) {
				String link = urlList.item(j).getTextContent();
				result.get(name).add(link);
			}
		}
		return result;
	}

	/**
	 * <h1>SearchWord</h1>
	 * <p>  This function gets search for a given word in a given index
	 * returns the list of URLs in which the word present. 
	 * </p>
	 * 
	 * @param word ->  The word to search
	 * @param index -> The Index in which the function  search  
	 * @return The list of links in which the word has been found
	 */
	public List<String> SearchWord(String word, Map<String, List<String>> index) {
		return index.get(word);
	}
	

}