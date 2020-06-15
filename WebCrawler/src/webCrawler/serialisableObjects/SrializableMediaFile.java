package webCrawler.serialisableObjects;

/** 
 * <h1> Serializable Media File  </h1>
 * <p> This class creates a serializable version of a data 
 * structure that represents any "non-HTML" URL found in the web site.
 * It contains the name which is the URL itself, its type and size </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.Serializable;

public class SrializableMediaFile implements Serializable{

	private static final long serialVersionUID = 1L;
	String name; 
	String type; 
	double size;

	// Constructor
	public SrializableMediaFile(String name, String type, double size) {
		this.name = name;
		this.type = type;
		this.size = size;
	}
	
	// Getters
	public String getName() {
		return name;
	}
	
	public String getType() {
		return type;
	}
	
	public double getSize() {
		return size;
	}

	// toString
	public String toString() {
		return "File :[name=" + name + ", type=" + type + ", size=" + size + "]";
	}
		
}
