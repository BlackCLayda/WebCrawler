package webCrawler.serialisableObjects;

/**
 * <h1> Serializable Text Field  </h1>
 * <p> This class creates a serializable version of a String
 * Used for communicating URL's and hints between server and client </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.Serializable;

public class SrializableTextField implements Serializable {

	private static final long serialVersionUID = 1L;
	private String text;

	// Constructor
	public SrializableTextField(String url) {
		super();
		this.text = url;
	}

	// Getter
	public String getText() {
		return text;
	}

	// toString
	public String toString() {
		return "[" + text + "]";
	}

}
