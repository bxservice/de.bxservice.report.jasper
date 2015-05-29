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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;

import net.sf.jasperreports.engine.util.FileResolver;

import org.compiere.model.MImage;
import org.compiere.model.Query;
import org.compiere.util.Env;

/**
 * This FileResolver allows to load Images directly from the AD_Image table. You
 * have to use a filename like "AD_Image:1000123.jpg" to load the images with
 * the database ID 1000123. You have to be sure that the filetype suffix is
 * identical, the file is not converted. (or use no suffix, JasperReports can
 * read an image without knowing the filename suffix). This works with images
 * saved as binary into the database or images saved as an URL String.
 * 
 * You can also read the image direct into your jasper report with a sql query.
 * If you want to do this you have to use "InputStream" as the Expression Class
 * and use an expression like this:
 * "new java.io.ByteArrayInputStream((byte[])$F{binarydata})".
 * 
 * The main difference is that from the view of JasperReports this FileResolver
 * allows to give an fixed filename and JasperReports can be configured to cache
 * the image. If you use a logo image in your page header then the logo will be
 * loader only once into your pdf. If you use an expression type of InputStream
 * then JasperReports can never be sure that the Image is the same and will save
 * every single image instance into the output file.
 * 
 * @author tbayen
 */
public class ReportFileResolverImage extends ReportFileResolver {

	public static final String AD_IMAGE_PREFIX = "AD_Image:";

	public ReportFileResolverImage(FileResolver parent) {
		super(parent);
	}

	@Override
	protected Boolean checkCacheFreshness(File cacheFile, String path,
			String name, String suffix) {
		// Images are always cached forever
		// (someone can implement a database refresh method if needed)
		if (name == null || !name.startsWith(AD_IMAGE_PREFIX))
			return true;
		if (parentFileResover instanceof ReportFileResolver) {
			return ((ReportFileResolver) parentFileResover)
					.checkCacheFreshness(cacheFile, path, name, suffix);
		}
		// unknown resolver type: It is the surest not to cache that
		return null;
	}

	@Override
	protected InputStream loadOriginalFileAsStream(String path, String name,
			String suffix) {
		if (name == null || !name.startsWith(AD_IMAGE_PREFIX))
			return null;
		int AD_Image_ID = new Integer(name.substring(AD_IMAGE_PREFIX.length()));
		MImage image = new Query(Env.getCtx(), MImage.Table_Name,
				MImage.COLUMNNAME_AD_Image_ID + "=? ", null).setParameters(
				AD_Image_ID).firstOnly();
		InputStream strm = new ByteArrayInputStream(image.getData());
		return strm;
	}

}
