package com.cagl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PaymentReportDto {
	
	private double due;
	private String provision;
	private double Gross;
	private String reporttds;
	private String net;
	private String gstamt;
	private String actualAmount;
	private double sdAmount;
	private double monthRent;
//	private double initialRent;
	private String monthYear;
	private String month;
	private String year;
	private RentContractDto Info;
	//Field is use for Payment Done or not
	private boolean PaymentFlag;
	
	
}
