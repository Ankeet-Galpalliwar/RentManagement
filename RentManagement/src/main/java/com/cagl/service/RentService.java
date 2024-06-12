package com.cagl.service;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.cagl.dto.AlertDto;
import com.cagl.dto.DashBoardInfo;
import com.cagl.dto.MakeActualDto;
import com.cagl.dto.PaymentReportDto;
import com.cagl.dto.Responce;
import com.cagl.dto.provisionDto;
import com.cagl.dto.varianceDto;

public interface RentService {

	Map<String, String> makeactual(String Status,List<MakeActualDto> actualDto);

	void addprovision(String provisionType, provisionDto provisionDto);

	Responce getprovision(String flag, String year);

	Responce getPaymentReport(String contractID, String month, String year, String purpose);
	
	
	ArrayList<AlertDto> getAlertContract() throws ParseException;


	List<varianceDto> getvariance(String contractID);

	List<PaymentReportDto> getResolvedAlertContract() throws Exception;

	DashBoardInfo getDashBoardDetails();

}
