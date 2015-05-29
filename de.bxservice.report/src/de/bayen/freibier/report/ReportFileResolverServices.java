/******************************************************************************
 * Copyright (C) 2014 Thomas Bayen                                            *
 * Copyright (C) 2014 Jakob Bayen KG, BX Service GmbH          				  *
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
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.util.FileResolver;

import org.adempiere.base.Service;

public class ReportFileResolverServices extends ReportFileResolver {

	private Map<String, List<ReportFileResolver>> factoryCache;

	public ReportFileResolverServices(FileResolver parent) {
		super(parent);
		initServiceList();
	}

	private void initServiceList() {
		factoryCache=new HashMap<String, List<ReportFileResolver>>();
	}

	private List<ReportFileResolver> getFileResolverList(String path){
		String prefix = getPrefix(path);
		List<ReportFileResolver> list = factoryCache.get(prefix);
		if(list==null){
			list=new ArrayList<ReportFileResolver>();
			List<IReportFileResolverFactory> factories = Service.locator().list(IReportFileResolverFactory.class).getServices();
			for (IReportFileResolverFactory factory : factories) {
				ReportFileResolver resolver = factory.getFileResolver(prefix);
				if(resolver!=null)
					list.add(resolver);
			}
			factoryCache.put(prefix, list);
		}
		return list;
	}

	static private String getPrefix(String path) {
		int index=path.indexOf(':');
		String prefix=null;
		if(index>=0)
			prefix=path.substring(0, index);
		return prefix;
	}
	
	@Override
	protected Boolean checkCacheFreshness(File cacheFile, String path,
			String name, String suffix) {
		List<ReportFileResolver> resolverList = getFileResolverList(path);
		for (ReportFileResolver reportFileResolver : resolverList) {
			Boolean freshness = reportFileResolver.checkCacheFreshness(cacheFile, path, name, suffix);
			if(freshness!=null)
				return freshness;
		}
		// not found. I am not responsible for this filepath
		return null;
	}

	@Override
	protected InputStream loadOriginalFileAsStream(String path, String name,
			String suffix) {
		List<ReportFileResolver> resolverList = getFileResolverList(path);
		for (ReportFileResolver reportFileResolver : resolverList) {
			InputStream strm = reportFileResolver.loadOriginalFileAsStream(path, name, suffix);
			if(strm!=null)
				return strm;
		}
		// not found. I am not responsible for this filepath
		return null;
	}
}
