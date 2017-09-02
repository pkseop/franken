package kr.co.future.sslvpn.xtmconf;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;

public class NodeWrapper {
	private Node node;

	public NodeWrapper(Node node) {
		this.node = node;
	}

	public String name() {
		return node.getNodeName();
	}

	public boolean isName(String name) {
		return name().equals(name);
	}

	public Boolean boolValue() {
		return (value() != null && !value().isEmpty()) ? value().equals("on") : null;
	}

	public Integer intValue() {
		return (value() != null && !value().isEmpty()) ? Integer.parseInt(value()) : null;
	}

	public String value() {
		return node.getTextContent();
	}

	public Boolean boolAttr(String name) {
		String value = attr(name);
		if (value == null)
			return false;
		
		return (value.equals("on") || value.equals("true"));
	}

	public Integer intAttr(String name) {
		String attr = attr(name);
		if (attr == null || attr.isEmpty() || attr.equals("null"))
			return null;
		return Integer.parseInt(attr);
	}

	public String attr(String name) {
		Node n = node.getAttributes().getNamedItem(name);
		if (n != null)
			return n.getTextContent();
		return null;
	}

	public Node node() {
		return node;
	}

	public List<NodeWrapper> children() {
		List<NodeWrapper> children = new ArrayList<NodeWrapper>();
		for (Node c = node.getFirstChild(); c != null; c = c.getNextSibling())
			children.add(new NodeWrapper(c));
		return children;
	}
}
