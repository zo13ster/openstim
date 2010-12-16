
package openstim.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.DocumentType;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLSerializer {
	private String indent        = "    ";
	private String lineSeparator = "\n";
	private String encoding      = "UTF-8";
	private int attrCountLimit   = 5;
	private int attrLengthLimit  = 100;
	private boolean displayAttributesOnSeperateLine = true;

	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	public String getEncoding() {
		return this.encoding;
	}

	public void setEncoding(String encoding) {
		this.encoding = encoding;
	}

	public String getIndent() {
		return this.indent;
	}

	public void setIndent(String indent) {
		this.indent = indent;
	}

	public boolean isDisplayAttributesOnSeperateLine() {
		return displayAttributesOnSeperateLine;
	}

	public void setDisplayAttributesOnSeperateLine(boolean displayAttributesOnSeperateLine) {
		this.displayAttributesOnSeperateLine = displayAttributesOnSeperateLine;
	}

	public void serialize(Document doc, OutputStream out) throws IOException {
		Writer writer = new OutputStreamWriter(out, encoding);
		serialize(doc, writer);
	}

	public void serialize(Document doc, File file) throws IOException {
		Writer writer = new FileWriter(file);
		serialize(doc, writer);
	}

	public void serialize(Document doc, Writer writer) throws IOException {
		doc.normalize();
		// Start serialization recursion with no indenting
		serializeNode(doc, writer, "");
		writer.write(lineSeparator);
		writer.write(lineSeparator);
		writer.write(lineSeparator);
		writer.flush();
	}

	private void serializeNode(Node node, Writer writer, String indentLevel) throws IOException {
		// Determine action based on node type
		switch (node.getNodeType()) {
			case Node.DOCUMENT_NODE:
				Document doc = (Document)node;
				writer.write("<?xml version=\"");
				writer.write(doc.getXmlVersion());
				writer.write("\" encoding=\"" + encoding + "\"?>");
				writer.write(lineSeparator);

				// recurse on each top-level node
				NodeList nodes = node.getChildNodes();
				if (nodes != null) {
					for (int i = 0; i < nodes.getLength(); i++) {
						serializeNode(nodes.item(i), writer, indentLevel);
					}
				}
				break;

			case Node.ELEMENT_NODE:
				String name = node.getNodeName();
				writer.write(indentLevel + "<" + name);

				NamedNodeMap attrMap = node.getAttributes();
				for (int i = 0; i < attrMap.getLength(); i++) {
					Node attr = attrMap.item(i);
					writer.write(" " + attr.getNodeName() + "=\"" + print(attr.getNodeValue()) + "\"");
				}

				// recurse on each child
				NodeList children = node.getChildNodes();
				if (children != null && children.getLength() > 0) {
					writer.write(">");
					if (children.item(0) != null && children.item(0).getNodeType() == Node.ELEMENT_NODE) {
						writer.write(lineSeparator);
					}
					for (int i = 0; i < children.getLength(); i++) {
						serializeNode(children.item(i), writer, indentLevel + indent);
					}
					if (children.item(0) != null && children.item(0).getNodeType() == Node.TEXT_NODE) {
						writer.write("</" + name + ">");
					} else {
						writer.write(indentLevel + "</" + name + ">");
					}
				} else {
					writer.write("/>");
				}

				writer.write(lineSeparator);
				break;

			case Node.TEXT_NODE:
				String text = print(node.getNodeValue().trim());
				if (text.length() > 0) {
					//writer.write(lineSeparator);
					//writer.write(indentLevel);
					writer.write(text);
					//writer.write(lineSeparator);
				}
				break;

			case Node.CDATA_SECTION_NODE:
				writer.write(indentLevel);
				writer.write("<![CDATA[");
				writer.write(lineSeparator);
				//writer.write(ASBeautifier.beautify(node.getNodeValue(), indentLevel + indent));
				writer.write(print(node.getNodeValue()));
				writer.write(indentLevel);
				writer.write("]]>");
				break;

			case Node.COMMENT_NODE:
				writer.write(indentLevel + "<!-- " + node.getNodeValue() + " -->");
				writer.write(lineSeparator);
				break;

			case Node.PROCESSING_INSTRUCTION_NODE:
				writer.write("<?" + node.getNodeName() + " " + node.getNodeValue() + "?>");
				writer.write(lineSeparator);
				break;

			case Node.ENTITY_REFERENCE_NODE:
				writer.write("&" + node.getNodeName() + ";");
				break;

			case Node.DOCUMENT_TYPE_NODE:
				DocumentType docType = (DocumentType) node;
				String publicId = docType.getPublicId();
				String systemId = docType.getSystemId();
				String internalSubset = docType.getInternalSubset();

				writer.write("<!DOCTYPE " + docType.getName());
				if (publicId != null) {
					writer.write(" PUBLIC \"" + publicId + "\" ");
				} else {
					writer.write(" SYSTEM ");
				}
				writer.write("\"" + systemId + "\"");
				if (internalSubset != null) {
					writer.write(" [" + internalSubset + "]");
				}
				writer.write(">");

				writer.write(lineSeparator);
				break;
			}
		}

	private String print(String s) throws IOException {
		StringWriter writer = new StringWriter();
		if (s == null) return writer.toString();

		for (int i = 0, len = s.length(); i < len; i++) {
			char c = s.charAt(i);
			switch (c) {
				case '<':
					writer.write("&lt;");
					break;

				case '>':
					writer.write("&gt;");
					break;

				case '&':
					writer.write("&amp;");
					break;

				case '\r':
					writer.write("&#xD;");
					break;

				default:
					writer.write(c);
			}
		}

		return writer.toString();
	}
}



