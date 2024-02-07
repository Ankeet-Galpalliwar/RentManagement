package com.cagl.notes;

import java.time.LocalDate;

public class RoughNotes {

	/**
	 * @Tds Calculation logic
	 */
	/*
	 * // ----------TDS Value initiate--------- double overallTDSValue = 0.0;
	 * LocalDate sdate = null; if (flagDate.getMonthValue() < 4) sdate =
	 * LocalDate.of(flagDate.getYear() - 1, 4, 1); if (flagDate.getMonthValue() >=
	 * 4) sdate = LocalDate.of(flagDate.getYear(), 4, 1); for (int m = 0; m < 12;
	 * m++) { LocalDate CrDate = sdate.plusMonths(m); String tdsQuery = "SELECT " +
	 * CrDate.getMonth() + " FROM rent_due e where e.contractid='" + contractID +
	 * "' and e.year='" + CrDate.getYear() + "'"; if (!getvalue(tdsQuery).isEmpty())
	 * overallTDSValue += Double.parseDouble(getvalue(tdsQuery).get(0)); } if
	 * (overallTDSValue > 240000)// IF(TDS->right)->Here TDS Value modify Base on
	 * Gross Value..! tds = Math.round((gross * (10 / 100.0f)));// By Default its
	 * (0.0)
	 * 
	 * 
	 */

}
