package net.atos.aw.tum.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.MissingResourceException;
import java.util.TreeSet;

public class FileTools {

	public static String read(File file, String encoding) {
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(file);
		} catch (FileNotFoundException e) {
			throw new RuntimeException("[FileTools.read] File not found :" + file.getAbsolutePath(), e);
		}
		BufferedInputStream bis = new BufferedInputStream(fis);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		int c;
		try {
			while ((c = bis.read()) != -1) {
				baos.write(c);
			}

			baos.flush();
			return baos.toString(encoding);
		} catch (IOException e) {
			throw new RuntimeException("[FileTools.read] IOException while reading '" + file.getAbsolutePath(), e);
		} finally {
			try {
				bis.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

		}
	}
	
	public static InputStream loadResource(String resource) throws MissingResourceException {
		Thread thread = Thread.currentThread();
		ClassLoader cLoader = thread.getContextClassLoader();
		URL url = cLoader.getResource(resource);
		if (url == null) {
			throw new MissingResourceException("Unable to find resource '" + resource + "'.", resource, resource);
		}
		try {
			InputStream is = url.openStream();
			return is;
		} catch (IOException e) {
			throw new MissingResourceException("Unable to load resource '" + resource + "' (IOException).", resource,
					resource);
		}
	}
}