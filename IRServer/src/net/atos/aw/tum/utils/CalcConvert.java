/**
 * truc quick and dirty pour convertir les feuilles OfficeCalc en données JSON
 */

package net.atos.aw.tum.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

public class CalcConvert {

	static final String OfficeNS = "urn:oasis:names:tc:opendocument:xmlns:office:1.0";
	static final String TableNS = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
	
	public static void main(String[] args) {
		Document xml = null;

		try {
			ZipFile inputFile = new ZipFile(args[0]);
			ZipEntry content = inputFile.getEntry("content.xml");
			InputStream contentStream = inputFile.getInputStream(content);
			xml = DocumentBuilderFactory.newInstance()
					.newDocumentBuilder().parse(contentStream);
			inputFile.close();
		} catch (SAXException | IOException | ParserConfigurationException e) {
			e.printStackTrace();
			System.exit(1);
		}
		NodeList sheets = xml.getDocumentElement()
				.getElementsByTagName("table:table");
		for(int i = 0; i < sheets.getLength(); i++){
			Element e = (Element)sheets.item(i);
			dumpSheet(e);
		}
	}

	static void dumpSheet(Element table) {
		String protocol = table.getAttribute("table:name");
		String period = null, header = "", prefix = null;
		List<String> keys = new ArrayList<String>();

		NodeList rows = table.getChildNodes();

		for (int l = 0; l < rows.getLength(); l++) {
			Node row = rows.item(l);
			if (!row.getNodeName().equals("table:table-row")) {
				continue;
			}
			String[] cells = getCells(row.getChildNodes());

			if (cells[0] == null || cells[0].equals("") || cells[0].equals("#")) {
				continue;
			} else if (cells[0].equals("prefix")) {
				prefix = "[ " + header + getValueList(cells) + " ]";
			} else if (cells[0].equals("header")) {
				header = getValueList(cells) + ", ";
			} else if (cells[0].equals("period")) {
				period = cells[1];
			} else {
				try {
					keys.add("'" + cells[0] + "': [ " + getValueList(cells) + " ]");
				} catch (NullPointerException e) {
					System.out.println("Error for key '" + cells[0] + "'");
				}
			}
		}

		System.out.println("\t'" + protocol + "': {");
		System.out.println("\t\t'frequency': 38000,");
		System.out.println("\t\t'period': " + period + ",");
		System.out.println("\t\t'prefix': " + prefix + ",");
		System.out.println("\t\t'keys': {");
		for (String key : keys) {
			System.out.println("\t\t\t" + key + ",");
		}
		System.out.println("\t\t}");
		System.out.println("\t},");
	}
	
	static String[] getCells(NodeList row) {
		List<String> result = new ArrayList<String>();
		for (int i = 0; i < row.getLength(); i++) {
			Node cell = row.item(i);
			String value = getCell(cell);
			Node repeated = cell.getAttributes().getNamedItem("table:number-columns-repeated");
			if (repeated != null) {
				int r = Integer.parseInt(repeated.getNodeValue());
				while(r != 0) {
					result.add(value);
					r--;
				}
			} else {
				result.add(value);
			}
		}
		return result.toArray(new String[0]);
	}

	static String getCell (Node n) {
		// table:table-cell -> text:p
		if (n == null || !n.getNodeName().equals("table:table-cell")) {
			return null;
		}
		if (n.getFirstChild() == null || !n.getFirstChild().getNodeName().equals("text:p")) {
			return null;
		}
		return n.getFirstChild().getTextContent();
	}

	static String getValueList(String[] row) {
		StringBuffer result = new StringBuffer(row[5]);
		int i = 6;
		while(i < row.length && row[i] != null && !row[i].equals("")) {
			result.append(", ");
			result.append(row[i]);
			i++;
		}
		return result.toString();
	}
}
