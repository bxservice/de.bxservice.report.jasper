package de.bayen.freibier.report;


import org.adempiere.base.IProcessFactory;
import org.adempiere.util.ProcessUtil;
import org.compiere.process.ProcessCall;

public class ProcessFactory implements IProcessFactory {

	@Override
	public ProcessCall newProcessInstance(String className) {
		if (className == null)
			return null;
		/*
		 * Special code to use this as an replacement for the standard
		 * JasperReports starter class
		 */
		if(ProcessUtil.JASPER_STARTER_CLASS.equals(className))
			return new ReportStarter();
		return null;
	}
}
