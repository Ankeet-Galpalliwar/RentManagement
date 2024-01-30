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
	private double provision;
	private double Gross;
	private double reporttds;
	private double net;
	private double gstamt;
	private double actualAmount;
	private double sdAmount;
	private double monthRent;
//	private double initialRent;
	private String monthYear;
	private RentContractDto Info;
	
	
}
