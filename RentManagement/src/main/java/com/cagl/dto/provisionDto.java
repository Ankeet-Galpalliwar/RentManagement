package com.cagl.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class provisionDto {

	private String provisionID;
	private String provisiontype;
	private String branchID;
	private String contractID;
	private int year;
	private String month;
	private double provisionAmount;
	private String remark;
	private LocalDate dateTime;
	private LocalDate flag;
	private RentContractDto info;
	//field is use for to give Delete Option
	private  boolean deleteFlag;
	//In reversed_provision->Middle payment Done or not
	private String paymentFlag;
	
	private String makerID;
	private String makerTimeZone;

}
