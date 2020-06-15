package webCrawler.serialisableObjects;

/** 
 * <h1> Serializable List </h1>
 * <p> This class creates a serializable version of a List
 * Used for communicating word searching answer between server and user </p>
 * 
 * @author TALBI LILIA - NAIMA BENAZZI.
 * 
 */

import java.io.Serializable;
import java.util.List;

public class SrializableList<T> implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private List<T> list;
	
	// Constructor
	public SrializableList(List<T> list) {
		super();
		this.list = list;
	}

	//Getter
	public List<T> getList() {
		return list;
	}

	//toString
	public String toString() {
		return "[list=" + list + "]";
	}
	
}
