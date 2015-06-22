package de.bxservice.report;


import org.adempiere.base.IProcessFactory;
import org.compiere.process.ProcessCall;

public class ProcessFactory implements IProcessFactory {
	
	public static final String BAYEN_JASPER_STARTER_CLASS = "de.bayen.freibier.report.ReportStarter";

	@Override
	public ProcessCall newProcessInstance(String className) {
		if (className == null)
			return null;
		/*
		 * Special code to use this as an replacement for the standard
		 * JasperReports starter class
		 */
		if(BAYEN_JASPER_STARTER_CLASS.equals(className))
			return new ReportStarter();
		return null;
	}
}
