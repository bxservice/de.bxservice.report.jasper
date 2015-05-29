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


/**
 * 
 * @author tbayen
 */
public interface IReportFileResolverFactory {

	/**
	 * The prefix can be null or "" or it can be like "attachment:". You can
	 * register for every prefix if you like. When the resolver can not resolve
	 * the file the next resolver in the service list is called.
	 * 
	 * @param prefix
	 * @return
	 */
	ReportFileResolver getFileResolver(String prefix);
}
