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
import java.io.InputStream;
import java.net.URL;

import net.sf.jasperreports.engine.util.FileResolver;

/**
 * Resolves Files from the Java Classpath.
 * 
 * This could be extended to read resources from OSGi plugins but this is not
 * yet done. I wonder if it is better to use some kind of service to offer more
 * resources via OSGi or if just a fragment package (possibly without java
 * classes) is easier for the user.
 * 
 * @author tbayen
 */
public class ReportFileResolverClasspath extends ReportFileResolver {
	
	protected String getDefaultResourcepath() {
//		return ReportFileResolver.class.getPackage().getName()
//				.replace('.', '/')
//				+ "/";
		return "reports/";
	}

	public ReportFileResolverClasspath(FileResolver parent) {
		super(parent);
	}

	protected Boolean checkCacheFreshness(File cacheFile, String path, String name, String suffix) {
		String fullSuffix = suffix != null ? "." + suffix : "";
		String fullPath =  getDefaultResourcepath() + name + fullSuffix;
		URL url = getClass().getClassLoader().getResource(fullPath);
		if (url == null)
			return null;
		else
			// classpath never changes
			return true;
	}

	protected InputStream loadOriginalFileAsStream(String path, String name, String suffix) {
		String fullSuffix = suffix != null ? "." + suffix : "";
		String fullPath = getDefaultResourcepath() + name + fullSuffix;
		ClassLoader cl = getClass().getClassLoader();
		//ClassLoader cl2 = FrameworkUtil.getBundle(getClass()).getBundleContext().getBundle().adapt(BundleWiring.class).getClassLoader();
		InputStream strm = cl.getResourceAsStream(fullPath);
		log.warning("loading file from classpath " + fullPath);
		return strm;
	}
}
