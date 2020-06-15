package webCrawler.clientExplorer;
/** 
 * <h1> Explorer </h1>
 * <p> This class parses the HTML pages 
 * to return links and keywords </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */


import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import webCrawler.serialisableObjects.SrializableTextField;;

public class Explorer {

	final String WORDS_SEPARATEUR = " ";

	/**
	 * <h1> getSourceCode </h1>
	 * <p> This function downloads the HTML file and put it into a String </p> 
	 * 
	 * @param rootLink -> The URL requested
	 * @param id -> A random ID not to overwrite an existing file
	 * @return A string containing the downloaded HTML file
	 * @throws IOException
	 */
	public String getSourceCode(String rootLink, int id) throws IOException {
		URL url = new URL(rootLink);
		ReadableByteChannel rbc = Channels.newChannel(url.openStream());
		File f = new File("fos" + id + ".txt");
		FileOutputStream fos = new FileOutputStream(f);
		FileChannel fchannel = fos.getChannel();
		fchannel.transferFrom(rbc, 0, Long.MAX_VALUE);
		fos.close();
		String result = readFile("fos" + id + ".txt");
		f.delete();
		return result;
	}

	/**
	 * <h1> getUrlType </h1>
	 * <p> This function returns the type of the URL</p> 
	 * 
	 * @param rootLink -> The URL requested
	 * @return the type of the HTML file
	 * @throws IOException
	 */
	public String getUrlType(String rootLink) throws IOException {
		return new URL(rootLink).openConnection().getContentType();
	}

	/**
	 * <h1> getMediaSize </h1>
	 * <p> This function returns the size of the URL </p> 
	 * 
	 * @param rootLink -> The URL requested
	 * @return the size of the HTML file
	 * @throws MalformedURLException
	 * @throws IOException
	 */
	public int getMediaSize(String rootLink) throws MalformedURLException, IOException {
		return new URL(rootLink).openConnection().getContentLength();

	}

	/**
	 * <h1> readFile </h1>
	 * <p> This function reads a given file and return a string of its content </p> 
	 * 
	 * @param file -> The path of the file to read
	 * @return A String filled with the file content. 
	 * @throws IOException
	 */
	public String readFile(String file) throws IOException {
		String result = "";
		BufferedReader bfr = new BufferedReader(new FileReader(file));
		String line = "";
		while ((line = bfr.readLine()) != null) {
			result += line;
		}
		bfr.close();
		return result;
	}

	//**************** HTML parsing functions

	/**
	 * <h1> getLinks </h1>	
	 * <p> This function gets the list of URLs in an HTML page</p> 
	 * 
	 * @param html -> The String containing the HTML file content
	 * @param url -> The URL requested
	 * @return The list of links contained in the HTML page 
	 * @throws MalformedURLException
	 * @throws InterruptedException
	 */
	public List<String> getLinks(String html, SrializableTextField url) throws MalformedURLException, InterruptedException {
		List<String> result = new ArrayList<>();
		Elements elements = Jsoup.parse(html).body().select("a");
		for (Element element : elements) {
			String link = element.attr("href");
			result.add(link);
		}
		// Remove useless links using filters
		result = filterUrlList(result, new URL(url.getText()));

		return result;
	}

	/**
	 * <h1> getkeyWords </h1>
	 * <p> This function gets the list of keyWords in an HTML page</p> 
	 * 
	 * @param -> The String containing the HTML file content
	 * @return The list of keyword contained in the HTML page  
	 */
	public List<String> getkeyWords(String html) {
		String texts = Jsoup.parse(html).select("html").text();
		return FilterWords(texts);	
	}

	//**************** Filtering functions

	/**
	 * <h1> filterUrlList </h1>
	 * <p> This function filters the URLs parsed in the HTML 
	 * page and return the final List to send back to server </p> 
	 * 
	 * @param urlList -> The complete list of links contained in the HTML page 
	 * @param urlSource -> The URL requested
	 * @return A filtered list of links
	 * @throws InterruptedException
	 */
	public static List<String> filterUrlList(List<String> urlList, URL urlSource) throws InterruptedException {

		List<String> result = new ArrayList<String>();

		for (String url : urlList) {

			// empty link.
			if (url.length() == 0) {
				continue;
			}

			// Reference in current page (anchors)
			if (url.startsWith("#")) {
				continue;
			}

			// Other protocols
			if (url.startsWith("https://") | url.startsWith("ftp://") | url.startsWith("mailto:")
					| url.startsWith("file:")) {
				continue;
			}

			// JavaScript
			if (url.toLowerCase().startsWith("javascript")) {
				continue;
			}

			// Reference to local URL
			if (url.equals("/")) {
				continue;
			}

			// Remove WWW from URL
			int index = url.indexOf("//www.");
			if (index != -1) {
				url = url.substring(0, index + 2) + url.substring(index + 6);
			}

			// From relative URL to absolute URL
			if (url.indexOf("://") == -1) {
				// Handle absolute URLs.
				if (url.startsWith("/")) {
					url = "http://" + urlSource.getHost() + url;
					// Handle relative URLs.
				} else {
					String file = urlSource.getFile();
					if (file.indexOf('/') == -1) {
						url = "http://" + urlSource.getHost() + "/" + url;
					} else {
						String path = file.substring(0, file.lastIndexOf('/') + 1);
						url = "http://" + urlSource.getHost() + path + url;
					}
				}
			}
			
			// External links
			try {
				if (!urlSource.getHost().toLowerCase().equals(new URL(url).getHost().toLowerCase())) {
					continue;
				}
			} catch (MalformedURLException e) {
				continue;
			}
			
			if((index = url.indexOf("/../")) != -1) {
				String url1=url.substring(0, index);
				String url2=url.substring(index+3, url.length() );
				index= url1.lastIndexOf("/");
				url1= url1.substring(0, index);
				url=url1+url2;
			}

			
			//Finally add the URL to the returned list
			result.add(url);

		}
		return result;

	}

	/**
	 * <h1> FilterWords </h1>
	 * <p> This function filters the text parsed in the HTML 
	 * page and return the final List of keywords to send back to server </p> 
	 * 
	 * @param str -> A String containing all the text of the URL 
	 * @return The list of filtered words
	 */
	public static List<String> FilterWords(String str) {
		str = str.toLowerCase();
		List<String> result = new ArrayList<String>();
		Pattern p = Pattern.compile("['\\p{L}]+");
		Matcher m = p.matcher(str);

		while (m.find()) {
			String word = m.group();
			if (!result.contains(word))
				result.add(word);
		}

		for (int i = 0; i < result.size(); i++) {
			String s = result.get(i);
			int index = s.indexOf("'");
			// Ex: "l'enfer"
			if (index != -1 && index == 1) {
				result.remove(s);
				result.add(i, s.substring(2));

			// Ex: "what's"
			} else if (index != -1 && index == s.length() - 2) {
				result.remove(s);
				result.add(i, s.substring(0, s.length() - 2));

			}
		}

		//Example of words that are not keyWords to remove
		List<String> nonKeyWordsList = new ArrayList<String>(
				Arrays.asList("you", "you're", "is", "le", "la", "les", "du", "des", "un", "une", "to", "your", "us",
						"à", "où", "don't", "if", "do", "or", "the", "let's", "i", "am"));
		result.removeAll(nonKeyWordsList);

		return result;
	}

}
