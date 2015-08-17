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
package de.bxservice.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import net.sf.jasperreports.engine.util.FileResolver;

import org.compiere.util.Ini;

/**
 * Reads JasperReport report files (and other resources) from the filesystem.
 * You can change the base path with the systemproperty
 * "org.compiere.report.path".
 * 
 * @author tbayen
 */
public class ReportFileResolverFileSystem extends ReportFileResolver {

	private static File REPORT_HOME = null;

	// REPORT_HOME is static initialized when this class is loaded
	static {
		String reportPath = System.getProperty("org.compiere.report.path");
		if (reportPath == null)
			REPORT_HOME = new File(Ini.getAdempiereHome() + File.separator + "reports");
		else
			REPORT_HOME = new File(reportPath);
	}

	public ReportFileResolverFileSystem(FileResolver parent) {
		super(parent);
	}
	
	/*@Override
	public File resolveFile(String fileName) {
		return new File(fileName);
	}*/

	@Override
	protected Boolean checkCacheFreshness(File cacheFile, String path, String name, String suffix) {
		String fullSuffix = suffix != null ? "." + suffix : "";
		String fullPath = REPORT_HOME + "/" + path + name + fullSuffix;
		if (cacheFile == null || !cacheFile.exists()){
			if(new File(fullPath).exists())
				return false;
			else
				return null;
		}
		return cacheFile.lastModified() > new File(fullPath).lastModified();
	}

	@Override
	protected InputStream loadOriginalFileAsStream(String path, String name, String suffix) {
		String fullSuffix = suffix != null ? "." + suffix : "";
		String fullPath;
		if(path.equals("./"))
			path = "";
		if( path == null || path.isEmpty() )
			fullPath= REPORT_HOME + "/" + name + fullSuffix;
		else
			fullPath= path + name + fullSuffix; 
		try {
			FileInputStream strm = new FileInputStream(fullPath);
			log.warning("loading file from " + fullPath);
			return strm;
		} catch (FileNotFoundException e) {
			return null;
		}
	}

}
