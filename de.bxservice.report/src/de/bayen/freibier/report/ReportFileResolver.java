/******************************************************************************
 * Copyright (C) 2013 Thomas Bayen                                            *
 * Copyright (C) 2013 Jakob Bayen KG             							  *
 * This program is free software; you can redistribute it and/or modify it    *
 * under the terms version 2 of the GNU General Public License as published   *
 * by the Free Software Foundation. This program is distributed in the hope   *
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied *
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.           *
 * See the GNU General Public License for more details.                       *
 * You should have received a copy of the GNU General Public License along    *
 * with this program; if not, write to the Free Software Foundation, Inc.,    *
 * 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.                     *
 *****************************************************************************/
package de.bayen.freibier.report;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.FileResolver;
import net.sf.jasperreports.engine.util.LocalJasperReportsContext;

import org.adempiere.exceptions.AdempiereException;
import org.compiere.util.CLogger;

/**
 * Abstract base class for JasperReports {@link FileResolver}s.
 * 
 * @author tbayen
 */
abstract public class ReportFileResolver implements FileResolver {

	// not yet used
	protected final static boolean deactivateCache=true;

	protected transient CLogger log = CLogger.getCLogger(getClass());

	public static final String SUFFIX_JASPER = "jasper";
	public static final String SUFFIX_JRXML = "jrxml";

	/**
	 * The cachedir keeps all files I deal with: *.jrxml, *.jasper, images,
	 * resources, properties... everything. The cache can at every time be
	 * cleaned with a command like <code>rm /tmp/JasperReportsCache* /*</code>.
	 * The cache is fresh and empty with every program run. If iDempiere is
	 * exited gracefully the cache is automatically deleted. If not you can
	 * delete the cache directory everytime with no harm.
	 */
	static File cacheDir = null;

	protected FileResolver parentFileResover;

	/**
	 * Constructor for a JasperReport FileResolver. If parent is not null the
	 * parent is used in the case that this class can not resolve the file.
	 * 
	 * @param parent
	 */
	public ReportFileResolver(FileResolver parent) {
		if (cacheDir == null) {
			// creating the cache directory as a temporary dir
			try {
				cacheDir = File.createTempFile("JasperReportsCache", "");
				// createTempFile creates a new file, but I need a directory
				String tmpDirName = cacheDir.getAbsolutePath();
				cacheDir.delete();
				cacheDir = new File(tmpDirName);
				cacheDir.mkdir();
				cacheDir.deleteOnExit();
			} catch (IOException e) {
				throw new AdempiereException();
			}
		}
		parentFileResover = parent;
	}

	@Override
	public File resolveFile(String fileName) {
		//System.out.println("resolveFile(\"" + fileName + "\"");
		// parse the filename
		fileName.replace('\\', '/');
		int i1 = fileName.lastIndexOf("/");
		int i2 = fileName.lastIndexOf(".");
		// includes '/'
		String path = i1 == -1 ? "" : fileName.substring(0, i1 + 1);
		String name = fileName.substring(i1 == -1 ? 0 : i1 + 1, i2 == -1 ? fileName.length() : i2);
		// includes not '.'
		String suffix = i2 == -1 ? "" : fileName.substring(i2 + 1);

		// this loads the file into the cache
		File cacheFile = loadFile(path, name, suffix);
		// if I can't find this file perhaps my parent can
		if (cacheFile == null && parentFileResover != null)
			return parentFileResover.resolveFile(fileName);
		return cacheFile;
	}

	/**
	 * Does the file loading. This method tries to load the file into the cache
	 * directory. If the file is not accessible the result is <code>null</code>.
	 * 
	 * @param path
	 * @param name
	 * @param suffix
	 * @return
	 */
	protected File loadFile(String path, String name, String suffix) {
		String fullSuffix = suffix != null ? "." + suffix : "";
		File cacheFile = new File(cacheDir + "/" /*+ path*/ + name + fullSuffix);

		// check if the file is actual (classpath resources never change)
		if (cacheFile.exists() && checkCacheFreshness(cacheFile, path, name, suffix)==Boolean.TRUE)
			return cacheFile;

		// copy file to cache
		InputStream originalFileStream = loadOriginalFileAsStream(path, name, suffix);
		if (originalFileStream == null) {
			if (SUFFIX_JASPER.equals(suffix)) {
				File jrxmlFile = loadFile(path, name, SUFFIX_JRXML);
				if (jrxmlFile != null)
					return compileJrxml(cacheDir + "/" + path + name);
			}
			return null;
		} else {
			copyStreamToCache(originalFileStream, cacheFile);
			return cacheFile;
		}
	}

	/**
	 * Checks if the file in the cache is still fresh. This method should be
	 * overloaded to use different checking methods for different types of file
	 * retrieval. If the file sources never change it can just return
	 * <code>true</code>; If this FileResolver does not know about this file it
	 * returns <code>null</code>.
	 * 
	 * @param cacheFile
	 * @param path
	 * @param name
	 * @param suffix
	 * @return
	 */
	abstract protected Boolean checkCacheFreshness(File cacheFile, String path, String name, String suffix);

	/**
	 * This method loads the original file from whereever the specific
	 * implementation of this abstract class takes its files. It does no
	 * freshness checks (this is done earlier), just loading.
	 * 
	 * @param path
	 * @param name
	 * @param suffix
	 * @return
	 */
	abstract protected InputStream loadOriginalFileAsStream(String path, String name, String suffix);

	/**
	 * Does a straight copy of binary data from the stream into a file.
	 * 
	 * @param originalFileStream
	 * @param cacheFile
	 */
	protected static void copyStreamToCache(InputStream originalFileStream, File cacheFile) {
		try {
			OutputStream out = null;
			try {
				out = new FileOutputStream(cacheFile);
				if (out != null) {
					byte buf[] = new byte[1024];
					int len;
					while ((len = originalFileStream.read(buf)) > 0) {
						out.write(buf, 0, len);
					}
				}
			} catch (Exception e) {
				throw new AdempiereException(e);
			} finally {
				if (out != null)
					out.close();
				if (originalFileStream != null)
					originalFileStream.close();
			}
		} catch (IOException e) {
			throw new AdempiereException();
		}
	}

	/**
	 * Compiles a *.jrxml file into an *.jasper file. The path parameter is a
	 * filepath without extension. The path has to be a (real existing) absolute
	 * file path (in the cache directory).
	 * 
	 * @param path
	 * @return
	 */
	protected File compileJrxml(String path) {
		File inputFile = new File(path + "." + SUFFIX_JRXML);
		File outputFile = new File(path + "." + SUFFIX_JASPER);
		try {
			LocalJasperReportsContext context = new LocalJasperReportsContext(DefaultJasperReportsContext.getInstance());
			context.setClassLoader(JasperReport.class.getClassLoader());
			JasperCompileManager manager = JasperCompileManager.getInstance(context);

			/*
			 * I do not really understand what is happening. But when I call the
			 * compileToFile() method here it can't find a class
			 * (java.lang.ClassNotFoundException:
			 * org/apache/xerces/jaxp/SAXParserFactoryImpl). To set the context
			 * of the manager seems not to help. We have to change the Thread's
			 * ClassLoader.
			 */
			ClassLoader cl1 = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(JasperReport.class.getClassLoader());
				manager.compileToFile(inputFile.getAbsolutePath(), outputFile.getAbsolutePath());
			} finally {
				Thread.currentThread().setContextClassLoader(cl1);
			}
			// Synchronize Dates
			outputFile.setLastModified(inputFile.lastModified());
			return outputFile;
		} catch (JRException e) {
			throw new AdempiereException(e);
		}
	}
}
