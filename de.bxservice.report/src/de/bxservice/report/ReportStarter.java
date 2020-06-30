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

import java.awt.print.PrinterJob;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.PropertyResourceBundle;

import javax.print.attribute.HashPrintRequestAttributeSet;
import javax.print.attribute.PrintRequestAttributeSet;
import javax.print.attribute.standard.Copies;
import javax.print.attribute.standard.JobName;

import org.adempiere.base.Service;
import org.adempiere.exceptions.AdempiereException;
import org.adempiere.exceptions.DBException;
import org.adempiere.report.jasper.JRViewerProvider;
import org.adempiere.util.IProcessUI;
import org.compiere.model.MClient;
import org.compiere.model.MPInstance;
import org.compiere.model.MProcess;
import org.compiere.model.MRole;
import org.compiere.model.MSysConfig;
import org.compiere.model.MUser;
import org.compiere.model.PrintInfo;
import org.compiere.model.X_AD_PInstance_Para;
import org.compiere.print.ArchiveEngine;
import org.compiere.print.MPrintFormat;
import org.compiere.print.PrintUtil;
import org.compiere.print.ServerReportCtl;
import org.compiere.process.ClientProcess;
import org.compiere.process.ProcessCall;
import org.compiere.process.ProcessInfo;
import org.compiere.process.ProcessInfoParameter;
import org.compiere.util.CLogger;
import org.compiere.util.CPreparedStatement;
import org.compiere.util.DB;
import org.compiere.util.Env;
import org.compiere.util.Ini;
import org.compiere.util.Language;
import org.compiere.util.Trx;
import org.compiere.util.Util;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRPropertiesUtil;
import net.sf.jasperreports.engine.JRVariable;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRPrintServiceExporter;
import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.fill.JRBaseFiller;
import net.sf.jasperreports.engine.fill.JRFiller;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.FileResolver;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.engine.util.LocalJasperReportsContext;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.ExporterOutput;
import net.sf.jasperreports.export.SimpleCsvExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterConfiguration;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterConfiguration;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimplePdfExporterConfiguration;
import net.sf.jasperreports.export.SimplePrintServiceExporterConfiguration;
import net.sf.jasperreports.export.SimpleTextExporterConfiguration;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsExporterConfiguration;
import net.sf.jasperreports.export.SimpleXmlExporterOutput;

/**
 * Starter Class to do a report using the JasperReports library. This starter is
 * well integrated into iDempiere and can be called everywhere where you can do
 * a report using the internal iDempiere report generator (using Printformat
 * etc.).
 * 
 * This class is loosely based on work done in the former JasperReports start
 * class "org.compiere.report.ReportStarter" done by many members of the
 * community: rlemeill, Ashley Ramdass, victor.perez@e-evolution.com, Teo Sarca
 * (www.arhipac.ro), Cristina Ghita (www.arhipac.ro), Trifon
 */
// TODO reading from http (checked with md5 checksums)
public class ReportStarter implements ProcessCall, ClientProcess {

	/**
	 * Parameters for the JasperReports parameter context.
	 */
	/* @formatter:off */
	public static final String JCTX_IDEMPIERE_PARAMETER    = "IDEMPIERE_PARAMETER";
	public static final String JCTX_IDEMPIERE_PARAMETER_PI = "IDEMPIERE_PARAMETER_PI";
	public static final String JCTX_SUBREPORT_DIR 	       = "SUBREPORT_DIR";
	public static final String JCTX_AD_PINSTANCE_ID        = MPInstance.COLUMNNAME_AD_PInstance_ID.toUpperCase();
	public static final String JCTX_AD_CLIENT_ID           = MClient.COLUMNNAME_AD_Client_ID.toUpperCase();
	public static final String JCTX_AD_ROLE_ID             = MRole.COLUMNNAME_AD_Role_ID.toUpperCase();
	public static final String JCTX_AD_USER_ID             = MUser.COLUMNNAME_AD_User_ID.toUpperCase();
	public static final String JCTX_AD_LANGUAGE            = "CURRENT_LANG";//"AD_Language";
	public static final String JCTX_CONTEXT                = "CONTEXT";
	public static final String JCTX_RECORD_ID              = "RECORD_ID";
	public static final String JCTX_TAB_WHERE              = "TAB_WHERE";
	public static final String JCTX_TAB_ORDER              = "TAB_ORDER";
	public static final String JCTX_GUI_SORT               = "GUI_SORT";
	public static final String JCTX_GUI_JOIN               = "GUI_JOIN";
	public static final String JCTX_RECORD_IDS             = "RECORD_IDs";
	/* @formatter:on */

	private static final int DEFAULT_SWAP_MAX_PAGES = 100;

	/* Instance fields */
	private String 	     m_printerName;
	private MPrintFormat m_printFormat;
	private PrintInfo    m_printInfo;
	private boolean      isDirectPrint;
	private String       jasperFilepath = null;
	private FileResolver m_fileResolver;
	private Language     currLang;
	
	private static CLogger log = CLogger.getCLogger(ReportStarter.class);

	/*
	 * Start class to create a JasperReports report.
	 */
	public boolean startProcess(Properties ctx, ProcessInfo pi, Trx trx) {

		ClassLoader cl1 = Thread.currentThread().getContextClassLoader();
		ClassLoader cl2 = JasperReport.class.getClassLoader();
		try {
			if (!cl1.equals(cl2)) {
				Thread.currentThread().setContextClassLoader(cl2);
			}
			return startJasperProcess(ctx, pi, trx);
		} finally {
			if (!cl1.equals(Thread.currentThread().getContextClassLoader())) {
				Thread.currentThread().setContextClassLoader(cl1);
			}
		}

	} //startProcess

	/**
	 * Start Jasper Report Process
	 * @param Properties  ctx
	 * @param ProcessInfo pi
	 * @param Trx         trx
	 * @return
	 */
	private boolean startJasperProcess(Properties ctx, ProcessInfo pi, Trx trx){

    	// initialize fields and the parameter map
		String trxName = null;
		if( trx != null )
			trxName = trx.getTrxName();

		initFromAD_Process(pi, trxName);
		if( jasperFilepath == null ){
            reportResult(pi.getAD_PInstance_ID(), "Can not find report data", trxName);
            return false;
		}
		else if ( Util.isEmpty(jasperFilepath, true) )
		{
            reportResult(pi.getAD_PInstance_ID(), "Can not find report", trxName);
            return false;
        }
		
		m_fileResolver = getReportFileResolver(jasperFilepath, pi.getAD_Process_ID());
		
		if ( m_fileResolver == null )
		{
			throw new AdempiereException("Can not solve the file type for " + jasperFilepath);
		}
		
		/*if (reportPath.startsWith("@#LocalHttpAddr@")) {
        	String localaddr = Env.getContext(Env.getCtx(), "#LocalHttpAddr");
        	if (!Util.isEmpty(localaddr)) {
        		reportPath = reportPath.replace("@#LocalHttpAddr@", localaddr);
        	}
        }*/

		HashMap<String, Object> params = initParameterMap(ctx, pi, trxName);
		// at this point all private fields are initialized

		int colonPos = jasperFilepath.indexOf(':');
		if (colonPos > -1)
			jasperFilepath = jasperFilepath.substring(colonPos + 1);
		// load jasper file
		File jasperFile = m_fileResolver.resolveFile(jasperFilepath);
		
		if (jasperFile == null || jasperFile.exists() == false)
		{
			log.severe("No report file found for given type, falling back to " + jasperFilepath);
			jasperFile = m_fileResolver.resolveFile(jasperFilepath);
		}

		if (jasperFile == null || jasperFile.exists() == false)
		{
			String tmp = "Can not find report file at path - " + jasperFilepath;
			log.severe(tmp);
			reportResult(pi.getAD_PInstance_ID(), tmp, trxName);
			throw new AdempiereException("Can not find report file at path - " + jasperFilepath);
		}

		JasperReport jasperReport;

		Connection conn = DB.getConnectionRO();
		try {
			jasperReport = (JasperReport) JRLoader.loadObjectFromFile(jasperFile.getAbsolutePath());
			
			String bundleName = jasperReport.getResourceBundle();
			if( bundleName == null )
				bundleName = jasperReport.getName();
			File propertiesFile = getPropertiesFile(bundleName, currLang);
			if ( propertiesFile != null ) {
				try {
					PropertyResourceBundle res = new PropertyResourceBundle( new FileInputStream(propertiesFile) );
					params.put("RESOURCE", res);
					params.put(JRParameter.REPORT_RESOURCE_BUNDLE, res);
				} catch (IOException e) {
					;
				}
			}

			createVirtualizer(params);
			LocalJasperReportsContext jasperContext = createJasperContext();

			// fill the report and create a print object
			JRBaseFiller filler = JRFiller.createFiller(jasperContext, jasperReport);
			JasperPrint jasperPrint = filler.fill(params, conn);
			// save the number of processed rows for the returned process
			// instance
			Object rowCount = filler.getVariableValue(JRVariable.REPORT_COUNT);

			// use the right way to output the report
			if (pi.isExport()) {
				buildReportExport(pi, jasperContext, jasperPrint);
			} else if (pi.isBatch()) {
				buildReportAsPDF(pi, jasperPrint);
			} else if (!isDirectPrint) {
				// view the report
				JRViewerProvider viewerLauncher = Service.locator().locate(JRViewerProvider.class).getService();
				viewerLauncher.openViewer(jasperPrint, pi.getTitle());

				// create pdf for archive
				// TODO check if this is really needed
				buildReportAsPDF(pi, jasperPrint);
			} else {
				buildReportDirectPrint(pi, jasperPrint);
			}
			// if PDF format as created we possibly want to archive that
			File pdfFile = pi.getPDFReport();
			if (pdfFile != null) {
				PrintInfo pinfo = m_printInfo != null ? m_printInfo : new PrintInfo(pi);
				ArchiveEngine.get().archive(pdfFile, pinfo);
			}
			/*
			 * if we got to this place without an exception we save the message
			 * what we did.
			 */
			if (rowCount instanceof Integer)
				pi.setRowCount((Integer) rowCount);
		} catch (JRException e) {
            log.severe("ReportStarter.startProcess: Can not run report - "+ e.getMessage());
        } catch (Exception e) {
			throw new AdempiereException("error while creating jasper report", e);
		} finally {
			try {
				conn.close();
			} catch (Exception e) {
			}
		}

		String errMsg = null;
		{
			int result = (errMsg == null ? 1 : 0);
			String sql = "UPDATE AD_PInstance SET Result=?, ErrorMsg=?"
					+ " WHERE AD_PInstance_ID=" + pi.getAD_PInstance_ID();
			DB.executeUpdateEx(sql, new Object[] { result, errMsg }, trxName);
		}
		
		//When succesfull return false to avoid the report process to run twice
		return true;
    } //startJasperProcess


	private File getPropertiesFile(String bundleName, Language currLang) {
		String resname = bundleName+"_"+currLang.getLocale().getLanguage()+"_"+currLang.getLocale().getCountry()+".properties";
		File originalFileStream = ((ReportFileResolver) m_fileResolver).resolveFile(resname);
		if( originalFileStream == null ){
			resname = bundleName+"_"+currLang.getLocale().getLanguage()+".properties";
			originalFileStream = ((ReportFileResolver) m_fileResolver).resolveFile(resname);
			if( originalFileStream == null ){
				resname = bundleName+".properties";
				originalFileStream = ((ReportFileResolver) m_fileResolver).resolveFile(resname);				
			}
		}

		return originalFileStream;
	}

	/**
	 * Creates a report and direct sends it to the appropriate printer.
	 * 
	 * @param pi
	 * @param jasperPrint
	 * @throws JRException
	 */
	private void buildReportDirectPrint(ProcessInfo pi, JasperPrint jasperPrint) throws JRException {
		// Get printer job
		PrinterJob printerJob = PrintUtil.getPrinterJob(m_printerName);
		// Set print request attributes

		// Paper Attributes:
		PrintRequestAttributeSet prats = new HashPrintRequestAttributeSet();

		// add: copies, job-name, priority
		if (m_printInfo == null || m_printInfo.isDocumentCopy()
				|| m_printInfo.getCopies() < 1) // @Trifon
			prats.add(new Copies(1));
		else
			prats.add(new Copies(m_printInfo.getCopies()));
		Locale locale = Language.getLoginLanguage().getLocale();
		// @Trifon
		String printFormat_name = m_printFormat == null ? "" : m_printFormat
				.getName();
		int numCopies = m_printInfo == null ? 0 : m_printInfo.getCopies();
		prats.add(new JobName(printFormat_name + "_" + pi.getRecord_ID(),
				locale));
		prats.add(PrintUtil.getJobPriority(jasperPrint.getPages().size(),
				numCopies, true));

		// Create print service exporter
		JRPrintServiceExporter exporter = new JRPrintServiceExporter();

		// Set parameters
		SimplePrintServiceExporterConfiguration configuration = new SimplePrintServiceExporterConfiguration();
		configuration.setPrintService(printerJob.getPrintService());
		configuration.setPrintServiceAttributeSet(printerJob.getPrintService()
				.getAttributes());
		configuration.setPrintRequestAttributeSet(prats);
		configuration.setDisplayPageDialog(false);
		configuration.setDisplayPrintDialog(false);
		exporter.setConfiguration(configuration);
		exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

		// Print report / document
		exporter.exportReport();
	}

	/**
	 * The Report is build and exported to a file. The filename is written to
	 * the ProcessInfo object. So the UI can give it to the user as a link to
	 * download.
	 * 
	 * @param pi
	 * @param jasperContext
	 * @param jasperPrint
	 * @throws JRException
	 */
	private void buildReportExport(ProcessInfo pi, LocalJasperReportsContext jasperContext, JasperPrint jasperPrint)
			throws JRException {
		// export
		try {
			String ext = pi.getExportFileExtension();
			if (ext == null)
				ext = "pdf";

			StringBuilder prefix = new StringBuilder();
			for (char ch : jasperPrint.getName().toCharArray())
				prefix.append(Character.isLetterOrDigit(ch) ? ch : '_');
			File file = File.createTempFile(prefix.toString(), "." + ext);
			FileOutputStream strm = new FileOutputStream(file);

			Exporter<ExporterInput, ?, ?, ? extends ExporterOutput> exporter = null;
			if (ext.equals("pdf")) {
				JRPdfExporter export = new JRPdfExporter(jasperContext);
				SimplePdfExporterConfiguration config = new SimplePdfExporterConfiguration();
				export.setConfiguration(config);
				export.setExporterOutput(new SimpleOutputStreamExporterOutput(
						strm));
				exporter = export;
			} else if (ext.equals("ps")) {
				JRPrintServiceExporter export = new JRPrintServiceExporter(
						jasperContext);
				SimplePrintServiceExporterConfiguration config = new SimplePrintServiceExporterConfiguration();
				export.setConfiguration(config);
				export.setExporterOutput(new SimpleOutputStreamExporterOutput(
						strm));
				exporter = export;
			} else if (ext.equals("xml")) {
				JRXmlExporter export = new JRXmlExporter(jasperContext);
				SimpleExporterConfiguration config = new SimpleExporterConfiguration();
				export.setConfiguration(config);
				export.setExporterOutput(new SimpleXmlExporterOutput(strm));
				exporter = export;
			} else if (ext.equals("csv") || ext.equals("ssv")) {
				JRCsvExporter export = new JRCsvExporter(jasperContext);
				SimpleCsvExporterConfiguration config = new SimpleCsvExporterConfiguration();
				if(ext.equals("ssv"))
					config.setFieldDelimiter(";");
				export.setConfiguration(config);
				export.setExporterOutput(new SimpleWriterExporterOutput(strm));
				exporter = export;
			} else if (ext.equals("txt")) {
				JRTextExporter export = new JRTextExporter(jasperContext);
				SimpleTextExporterConfiguration config = new SimpleTextExporterConfiguration();
				export.setConfiguration(config);
				exporter = export;
			} else if (ext.equals("html") || ext.equals("htm")) {
				HtmlExporter export = new HtmlExporter(jasperContext);
				SimpleHtmlExporterConfiguration config = new SimpleHtmlExporterConfiguration();
				export.setConfiguration(config);
				exporter = export;
			} else if (ext.equals("xls")) {
				JRXlsExporter export = new JRXlsExporter(jasperContext);
				SimpleXlsExporterConfiguration config = new SimpleXlsExporterConfiguration();
				export.setConfiguration(config);
				exporter = export;
			} else {
				strm.close();
				throw new AdempiereException("FileInvalidExtension=" + ext);
			}

			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));

			exporter.exportReport();
			strm.close();
			pi.setExportFile(file);
		} catch (IOException e) {
			throw new AdempiereException(
					"ReportStarter: Can not export PDF File", e);
		}
	}

	/**
	 * Creates a PDF report and set the PDFReport property in the ProcessInfo
	 * Object as a result value. This is used in automatic batch calls. This PDF
	 * can e.g. sent as an email or stored as an attachment.
	 * 
	 * @param pi
	 * @param jasperPrint
	 * @throws JRException
	 */
	private void buildReportAsPDF(ProcessInfo pi, JasperPrint jasperPrint) throws JRException {
		// You can use JasperPrint to create PDF
		try {
			StringBuilder prefix = new StringBuilder();
			for (char ch : jasperPrint.getName().toCharArray())
				prefix.append(Character.isLetterOrDigit(ch) ? ch : '_');
			File PDF;
			if (pi.getPDFFileName() != null) {
				PDF = new File(pi.getPDFFileName());
			} else {
				PDF=File.createTempFile(prefix.toString(), ".pdf");
			}

			DefaultJasperReportsContext jrContext = DefaultJasperReportsContext.getInstance();
			LocalJasperReportsContext ljrContext = new LocalJasperReportsContext(jrContext);
			ljrContext.setClassLoader(this.getClass().getClassLoader());
			JRPdfExporter exporter = new JRPdfExporter(ljrContext);
			exporter.setExporterInput(new SimpleExporterInput(jasperPrint));
			exporter.setExporterOutput(new SimpleOutputStreamExporterOutput(PDF.getAbsolutePath()));
			exporter.exportReport();
			pi.setPDFReport(PDF);
		} catch (IOException e) {
			throw new AdempiereException("ReportStarter: Can not make PDF File", e);
		}
	}

	private void createVirtualizer(HashMap<String, Object> params) {
		// create and initialize a virtualizer
		int maxPages = MSysConfig.getIntValue(MSysConfig.JASPER_SWAP_MAX_PAGES, DEFAULT_SWAP_MAX_PAGES);
		String swapPath = System.getProperty("java.io.tmpdir");
		JRSwapFile swapFile = new JRSwapFile(swapPath, 1024, 1024);
		JRSwapFileVirtualizer virtualizer = new JRSwapFileVirtualizer(maxPages, swapFile, true);
		params.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
	}

	private LocalJasperReportsContext createJasperContext() {
		// create context
		LocalJasperReportsContext jasperContext = new LocalJasperReportsContext(DefaultJasperReportsContext.getInstance());
		jasperContext.setClassLoader(JasperReport.class.getClassLoader());
		jasperContext.setFileResolver(m_fileResolver);
		JRPropertiesUtil.getInstance(jasperContext).setProperty("net.sf.jasperreports.awt.ignore.missing.font", "true");
		return jasperContext;
	}

	/**
	 * Collects a bunch of interesting values into a map to use them as
	 * parameters in the JasperReports report file.
	 * 
	 * @param ctx
	 * @param pi
	 * @param trxName
	 * @return Map
	 */
	private HashMap<String, Object> initParameterMap(Properties ctx, ProcessInfo pi, String trxName) {
		// create and initialize parameter hash
		HashMap<String, Object> params = new HashMap<String, Object>();
		/*
		 * Process Parameters hash contains the parameters set by the user in
		 * the parameters dialog.
		 */
		Map<String, Object> parameterP = getProcessParameters(pi.getAD_PInstance_ID(), trxName);

		/*
		 * The ProcessInfo object has some more info created by the process
		 * calling code. See ServerReportCtl.runJasperProcess(). It sets e.g.
		 * PARAM_PRINTER_NAME, PARAM_PRINT_FORMAT, PARAM_PRINT_INFO
		 */
		Map<String, Object> parameterPI = getProcessInfoParameters(pi.getParameter());

		// parameters are saved as map and direct
		params.put(JCTX_IDEMPIERE_PARAMETER, parameterP);
		params.put(JCTX_IDEMPIERE_PARAMETER_PI, parameterPI);
		params.putAll(parameterP);
		params.putAll(parameterPI);
		params.put(JCTX_SUBREPORT_DIR, "");
		params.put(JCTX_AD_PINSTANCE_ID, pi.getAD_PInstance_ID());
		params.put(JCTX_AD_CLIENT_ID, Env.getAD_Client_ID(ctx));
		params.put(JCTX_AD_ROLE_ID, Env.getAD_Role_ID(ctx));
		params.put(JCTX_AD_USER_ID, Env.getAD_User_ID(ctx));

		currLang = initPrinterSettings(ctx, parameterPI);
		// Locale object (JasperReports standard e.g. to access ResourceBundles)
		params.put(JRParameter.REPORT_LOCALE, currLang.getLocale());
		// String, e.g. "de_DE" or "en_US"
		params.put(JCTX_AD_LANGUAGE, currLang.getAD_Language());

		// put context into params
		params.put(JCTX_CONTEXT, ctx);

		params.putAll(createTabParams(ctx, pi));
		return params;
	}

	/**
	 * Inits the values for {@link #isDirectPrint} and {@link #jasperFilepath}
	 * from the AD_Process record for this JasperReports Process.
	 * 
	 * @param pi
	 * @param trxName
	 */
	private void initFromAD_Process(ProcessInfo pi, String trxName) {
	
		// TODO create file resolver from the m_jasperFilepath
		//m_fileResolver = new ReportFileResolverClasspath(null);

		String sql = "SELECT pr.JasperReport, pr.IsDirectPrint "
				+ "FROM AD_Process pr, AD_PInstance pi "
				+ "WHERE pr.AD_Process_ID = pi.AD_Process_ID "
				+ " AND pi.AD_PInstance_ID=?";

		CPreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, trxName);
			pstmt.setInt(1, pi.getAD_PInstance_ID());
			rs = pstmt.executeQuery();
			isDirectPrint = false;
			boolean isPrintPreview = pi.isPrintPreview();
			if (rs.next()) {
				jasperFilepath = rs.getString(1);

				if (jasperFilepath.endsWith(".jrxml"))
					jasperFilepath = jasperFilepath.replace(".jrxml", ".jasper");

				if ("Y".equalsIgnoreCase(rs.getString(2))
						&& !Ini.isPropertyBool(Ini.P_PRINTPREVIEW)
						&& !isPrintPreview)
					isDirectPrint = true;
			} else {
				throw new AdempiereException("data not found; sql = " + sql);
			}
		} catch (SQLException e) {
			throw new DBException(e, sql);
		} finally {
			DB.close(rs, pstmt);
			rs = null;
			pstmt = null;
		}


	}
	
	protected ReportFileResolver getReportFileResolver(String reportPath, int AD_Process_ID)
	{
		m_fileResolver=null;
		ReportFileResolver fileResolver = null;

		if (reportPath.startsWith("http://") || reportPath.startsWith("https://")) {

			fileResolver = new ReportFileResolverHttp(m_fileResolver);

		} else if (reportPath.startsWith("attachment:")) {

			MProcess process = new MProcess(Env.getCtx(), AD_Process_ID, null);
			if(process != null)
				fileResolver = new ReportFileResolverAttachment(m_fileResolver, process);

		} else if (reportPath.startsWith("/")) {

			fileResolver = new ReportFileResolverFileSystem(m_fileResolver);

		} else if (reportPath.startsWith("file:/")) {

			fileResolver = new ReportFileResolverURIFile(m_fileResolver);

		} else if (reportPath.startsWith("resource:")) {

			fileResolver = new ReportFileResolverResource(m_fileResolver);

		} else if (reportPath.startsWith("classpath:")) {
			fileResolver = new ReportFileResolverClasspath(m_fileResolver);
		} else {
			/*
			 * Sets the sequence level to look for the report
			 * Attachment->File->Classpath->Image
			 */
			fileResolver = new ReportFileResolverImage(fileResolver);	
			fileResolver = new ReportFileResolverClasspath(fileResolver);
			fileResolver = new ReportFileResolverFileSystem(fileResolver);
			MProcess process = new MProcess(Env.getCtx(), AD_Process_ID, null);
			if(process != null)
				fileResolver = new ReportFileResolverAttachment(fileResolver, process);
		}

		return fileResolver;
	}

	/**
	 * initializes the printer setting objects from the ProcessInfo parameters
	 * and extracts the document language.
	 * 
	 * @param parameterPI
	 * @return
	 */
	private Language initPrinterSettings(Properties ctx, Map<String, Object> parameterPI) {
		// read some values from environment like my print format etc.
		Language currLang = Env.getLanguage(ctx);
		m_printerName = (String) parameterPI.get(ServerReportCtl.PARAM_PRINTER_NAME);
		m_printFormat = (MPrintFormat) parameterPI.get(ServerReportCtl.PARAM_PRINT_FORMAT);
		m_printInfo = (PrintInfo) parameterPI.get(ServerReportCtl.PARAM_PRINT_INFO);
		// Set the language of the print format if we're printing a document
		if (m_printFormat != null && m_printInfo != null && m_printInfo.isDocument())
			currLang = m_printFormat.getLanguage();
		// Set printer name unless already set.
		if (m_printFormat != null && m_printerName == null)
			m_printerName = m_printFormat.getPrinterName();
		return currLang;
	}

	/**
	 * Collects informations about the window and tab which called the process
	 * (if any), its active record, its filter and sorting.
	 * 
	 * @param ctx
	 * @param pi
	 * @param params
	 */
	private Map<String, Object> createTabParams(Properties ctx, ProcessInfo pi) {
		Map<String, Object> params = new HashMap<String, Object>();

		if (pi.getRecord_ID() > 0)
			params.put(JCTX_RECORD_ID, pi.getRecord_ID());

		// IDEMPIERE-270, IDEMPIERE-1718 - inherit search filter from window /
		// tbayen
		// find the right Tab entry in the Context
		// int tabNo = -1;
		// try {
		// while (true) {
		// int tableID = Integer.parseInt(Env.getContext(ctx, pi.getWindowNo(),
		// ++tabNo, GridTab.CTX_AD_Table_ID));
		// if (tableID == pi.getTable_ID())
		// break;
		// }
		// } catch (NumberFormatException ex) {
		// tabNo = -1;
		// }
		// if(tabNo>=0){
		// // You can use these parameters as a "query chunk" with the $P!{...}
		// syntax
		// String sql=Env.getContext(ctx, pi.getWindowNo(), tabNo,
		// GridTab.CTX_SQL);
		// /*
		// * I rely on the format of this sql query string like it is done in
		// * GridTable.createSelectSQL().
		// */
		// final String marker1 = " WHERE ", marker2 = " ORDER BY ";
		// String whereClause = sql.substring(sql.indexOf(marker1) +
		// marker1.length(), sql.indexOf(marker2));
		// if (Util.isEmpty(whereClause, true))
		// whereClause = "true";
		// params.put(JCTX_TAB_WHERE, whereClause);
		//
		// String orderbyClause = sql.substring(sql.indexOf(marker2) +
		// marker2.length());
		// if (Util.isEmpty(orderbyClause, true))
		// orderbyClause = "1";
		// params.put(JCTX_TAB_ORDER, orderbyClause);
		//
		// String sortClause = Env.getContext(ctx, pi.getWindowNo(), tabNo,
		// GridTab.CTX_GUISort);
		// String joinClause = "";
		// if (Util.isEmpty(sortClause, true)) {
		// sortClause = "1";
		// } else {
		// /*
		// * If I want to follow a Lookup for sorting it is a bit
		// * complicated to collect everything I need. This implementation
		// * could be improved. It should work well for most table
		// * references but e.g. not for reference lists.
		// */
		// int myTableID = Env.getContextAsInt(ctx, pi.getWindowNo(), tabNo,
		// GridTab.CTX_AD_Tab_ID);
		// String myTableName = MTable.getTableName(ctx, myTableID);
		// String tableName = sortClause.substring(0, sortClause.indexOf('.'));
		// if (!myTableName.equals(tableName)) {
		// String columnName = sortClause.substring(sortClause.indexOf('.') + 1,
		// sortClause.indexOf(' '));
		// joinClause = "JOIN SORT_" + tableName + " ON(" + tableName + "." +
		// tableName + "_ID = "
		// + myTableName + "." + columnName + ")";
		// }
		// }
		// params.put(JCTX_GUI_SORT, sortClause);
		// params.put(JCTX_GUI_JOIN, joinClause);
		//
		// // records can be choosen ony by one (up to now only in zk)
		// if(pi.getRecord_IDs()!=null && pi.getRecord_IDs().length>0){
		// StringBuilder ridString=new StringBuilder();
		// for (int rid : pi.getRecord_IDs()) {
		// if(ridString.length()>0)
		// ridString.append(", ");
		// ridString.append(rid);
		// }
		// params.put(JCTX_RECORD_IDS, ridString);
		// }else
		// params.put(JCTX_RECORD_IDS, "NULL");
		// }
		return params;
	}

	private Map<String, Object> getProcessInfoParameters(ProcessInfoParameter[] para) {

		Map<String, Object> params = new HashMap<String, Object>();
		if (para == null)
			return params;
		for (int i = 0; i < para.length; i++) {
			if (para[i].getParameter_To() == null) {
				if (para[i].getParameterName().endsWith("_ID") && para[i].getParameter() instanceof BigDecimal) {
					params.put(para[i].getParameterName(), ((BigDecimal)para[i].getParameter()).intValue());
				} else {
					params.put(para[i].getParameterName(), para[i].getParameter());
				}
			} else {
				// range - from
				if (para[i].getParameterName().endsWith("_ID") && para[i].getParameter() != null && para[i].getParameter() instanceof BigDecimal) {
	                params.put( para[i].getParameterName()+"1", ((BigDecimal)para[i].getParameter()).intValue());
				} else {
	                params.put( para[i].getParameterName()+"1", para[i].getParameter());
				}
				// range - to
				if (para[i].getParameterName().endsWith("_ID") && para[i].getParameter_To() instanceof BigDecimal) {
	                params.put( para[i].getParameterName()+"2", ((BigDecimal)para[i].getParameter_To()).intValue());
				} else {
	                params.put( para[i].getParameterName()+"2", para[i].getParameter_To());
				}
			}
		}
		return params;
	} //getProcessInfoParameters

	/**
	 * Load Process Parameters into a map
	 * @param AD_PInstance_ID
	 * @param trxName
	 */
	private static Map<String, Object> getProcessParameters(int AD_PInstance_ID, String trxName) {

		Map<String, Object> params = new HashMap<String, Object>();
		// @formatter:off
		final String sql = "SELECT " 
				+ " " + X_AD_PInstance_Para.COLUMNNAME_ParameterName
				+ "," + X_AD_PInstance_Para.COLUMNNAME_P_String
				+ "," + X_AD_PInstance_Para.COLUMNNAME_P_String_To 
				+ "," + X_AD_PInstance_Para.COLUMNNAME_P_Number
				+ "," + X_AD_PInstance_Para.COLUMNNAME_P_Number_To 
				+ "," + X_AD_PInstance_Para.COLUMNNAME_P_Date
				+ "," + X_AD_PInstance_Para.COLUMNNAME_P_Date_To 
				+ "," + X_AD_PInstance_Para.COLUMNNAME_Info
				+ "," + X_AD_PInstance_Para.COLUMNNAME_Info_To
				+ " FROM "  + X_AD_PInstance_Para.Table_Name
				+ " WHERE " + X_AD_PInstance_Para.COLUMNNAME_AD_PInstance_ID + "=?";
		// @formatter:on
		PreparedStatement pstmt = null;
		ResultSet rs = null;
		try {
			pstmt = DB.prepareStatement(sql, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY, trxName);
			pstmt.setInt(1, AD_PInstance_ID);
			rs = pstmt.executeQuery();
			while (rs.next()) {
				String name = rs.getString(1);
				Object val = rs.getString(2);
				if (val == null)
					val = name.endsWith("_ID") ? rs.getInt(4) : rs.getBigDecimal(4);
				if (val == null)
					val = rs.getDate(6);
				Object valTo = rs.getString(3);
				if (valTo == null)
					valTo = name.endsWith("_ID") ? rs.getInt(5) : rs.getBigDecimal(5);
				if (valTo == null)
					valTo = rs.getDate(7);
				params.put(name, val);
				// info contains e.g. a name if value is an reference id
				params.put(name + "_Info", rs.getString(8));
				if (valTo != null) {
					params.put(name + "To", valTo);
					params.put(name + "_InfoTo", rs.getString(9));
					// only for compatibility:
					// params.put(name + "1", val);
					// params.put(name + "2", valTo);
					// params.put(name + "_Info1", rs.getString(8));
					// params.put(name + "_Info2", rs.getString(9));
				}
			}
		} catch (SQLException e) {
			throw new DBException(e, sql);
		} finally {
			DB.close(rs, pstmt);
		}
		return params;
	}
	
	/**
	 * Update AD_PInstance result and error message
     * @author rlemeill
     * @param AD_PInstance_ID
     * @param errMsg error message or null if there is no error
     */
    protected void reportResult(int AD_PInstance_ID, String errMsg, String trxName)
    {
        int result = (errMsg == null ? 1 : 0);
        String sql = "UPDATE AD_PInstance SET Result=?, ErrorMsg=?"
                     +" WHERE AD_PInstance_ID="+AD_PInstance_ID;
        DB.executeUpdateEx(sql, new Object[]{result, errMsg}, trxName);
    }

	@Override
	public void setProcessUI(IProcessUI processUI) {
		// m_processUI = processUI;
	}

	// TODO if (reportPath.startsWith("@#LocalHttpAddr@")) {
	// TODO query anpassen, wenn mehrere Record id gesetzt sind
	// TODO resource bundles
	// TODO absolute File Paths ermöglichen (und cachen)
}
