/** 
 * <h1> XML Generator </h1>
 * <p> This class generate an XML file containing 
 * injected data Or adds data to existing </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */
package webCrawler.xmlManager;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import webCrawler.serialisableObjects.SrializableMediaFile;

public class XmlGenerator {

	static File f = new File("crawlingHistory.xml");

	/**
	 * <h1>writeInXmlFile</h1>
	 * <p>
	 * Function that initiate appending new data to the XML record file. Create this
	 * file if not exists
	 * </p>
	 * 
	 * @param url        -> The URL entered by the user for crawling
	 * @param myFileList -> The list of files contained in the URL (images, videos,
	 *                   etc.)
	 * @param myIndex    -> The index of words contained in the web site
	 * @param urlList    -> The list of URLs contained in the web site.
	 * @throws Exception
	 */
	public synchronized void  writeInXmlFile(String url, List<SrializableMediaFile> myFileList, Map<String, List<String>> myIndex,
			List<String> urlList) throws Exception {

		DocumentBuilder docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
		Document doc;
		if (!f.exists()) {
			doc = docBuilder.newDocument();
			createFirstChild(doc);
		} else {
			doc = docBuilder.parse(f);
		}
		appendData(url, urlList, myFileList, myIndex, doc);
		flush(doc);
	}

	/**
	 * <h1>createFirstChild</h1>
	 * <p>
	 * Function that create the root element of the XML record file. this function
	 * is only called on the first time appending data (if no existent record file)
	 * </p>
	 * @param doc 
	 */
	public static void createFirstChild(Document doc) {
		Element rootElement = doc.createElement("crawlingHistory");
		doc.appendChild(rootElement);
	}

	/**
	 * <h1>appendData</h1>
	 * <p>
	 * Function that adds the treated URL as root's child (with the link as
	 * attribute). The function calls other specific appending functions.
	 * </p>
	 * 
	 * @see appendLinkList,
	 * @see appendFileList
	 * @see appendIndex
	 * @param url        -> The URL entered by the user for crawling
	 * @param myFileList -> The list of files contained in the URL (images, videos,
	 *                   etc.)
	 * @param myIndex    -> The index of words contained in the web site
	 * @param urlList    -> The list of URLs contained in the web site.
	 * @param doc 
	 * @throws Exception
	 * @throws IOException
	 */
	private static void appendData(String url, List<String> urlList, List<SrializableMediaFile> myFileList,
			Map<String, List<String>> myIndex, Document doc) throws Exception, IOException {

		// Append new crawled link to the root element
		Element crawledLink = doc.createElement("crawledLink");
		doc.getDocumentElement().appendChild(crawledLink);
		crawledLink.setAttribute("name", url);

		// Append children
		appendLinkList(crawledLink, urlList, doc);
		appendFileList(crawledLink, myFileList, doc);
		appendIndex(crawledLink, myIndex, doc);
	}

	/**
	 * <h1>appendIndex</h1>
	 * <p>
	 * Function that adds the words index as the crawled Link's child.
	 * </p>
	 * <b> For each word of index, appends the links in which it appears as its
	 * children</b>
	 * 
	 * @param crawledLink -> The element that represents the crawled URL (as a
	 *                    parent for upcoming appending)
	 * @param myIndex     -> The index of words contained in the web site
	 * @param doc 
	 */
	private static void appendIndex(Element crawledLink, Map<String, List<String>> myIndex, Document doc) {
		Element index = doc.createElement("index");
		crawledLink.appendChild(index);

		Iterator<String> i = myIndex.keySet().iterator();
		while (i.hasNext()) {
			String myMot = i.next();
			Element mot = doc.createElement("mot");
			index.appendChild(mot);
			mot.setAttribute("name", myMot);
			for (String url : myIndex.get(myMot)) {
				Element link = doc.createElement("link");
				link.appendChild(doc.createTextNode(url));
				mot.appendChild(link);
			}
		}
	}

	/**
	 * <h1>appendFileList</h1>
	 * <p>
	 * Function that adds the file list as the crawled Link's child.
	 * </p>
	 * <b> For each file of the list sets its link as attribute and appends 2
	 * children
	 * <ul>
	 * <li>the file's type</li>
	 * <li>the file's size</li>
	 * </ul>
	 * </b>
	 * 
	 * @param crawledLink -> The element that represents the crawled URL (as a
	 *                    parent for upcoming appending)
	 * @param myFileList  -> The list of files contained in the URL (images, videos,
	 *                    etc.)
	 * @param doc 
	 */
	private static void appendFileList(Element crawledLink, List<SrializableMediaFile> myFileList, Document doc) {
		Element fileList = doc.createElement("fileList");
		crawledLink.appendChild(fileList);

		for (SrializableMediaFile myFile : myFileList) {
			Element file = doc.createElement("file");
			fileList.appendChild(file);
			file.setAttribute("name", myFile.getName());

			Element type = doc.createElement("type");
			type.appendChild(doc.createTextNode(myFile.getType()));
			file.appendChild(type);

			Element size = doc.createElement("size");
			size.appendChild(doc.createTextNode(Double.toString(myFile.getSize())));
			file.appendChild(size);
		}
	}

	/**
	 * <h1>appendLinkList</h1>
	 * <p>
	 * Function that adds the links list as the crawled Link's child.
	 * </p>
	 * 
	 * @param crawledLink -> The element that represents the crawled URL (as a
	 *                    parent for upcoming appending)
	 * @param urlList     -> The list of URLs contained in the web site.
	 * @param doc 
	 */
	private static void appendLinkList(Element crawledLink, List<String> urlList, Document doc) {
		Element linkList = doc.createElement("linkList");
		crawledLink.appendChild(linkList);

		for (String url : urlList) {
			Element link = doc.createElement("link");
			link.appendChild(doc.createTextNode(url));
			linkList.appendChild(link);
		}
	}
	
	/**
	 * <h1> flush </h1> 
	 * <p> Function that pushes updates to the file </p>
	 * @param doc 
	 * @throws TransformerException
	 */
	public static void flush(Node doc) throws TransformerException {
		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = transformerFactory.newTransformer();
		DOMSource source = new DOMSource(doc);
		StreamResult result = new StreamResult(f);
		transformer.transform(source, result);
	}

}