package scott.server;

import java.io.*;
import java.util.*;

public final class Packet implements Serializable {
	private String name;
	private Map parameters;
	private Map attributes;

	public Packet(String name) {
		this.name = name;
		this.parameters = new HashMap();
		this.attributes = new HashMap();
	}

	public String getName() {
		return name;
	}

	public void setParameter(String name, String value) {
		parameters.put(name, value);
	}

	public String getParameter(String name) {
		return (String)parameters.get(name);
	}

	public Iterator parameterNames() {
		return parameters.keySet().iterator();
	}

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}

	public Iterator attributesNames() {
		return attributes.keySet().iterator();
	}

	public void clear() {
		parameters.clear();
		attributes.clear();
	}

}
