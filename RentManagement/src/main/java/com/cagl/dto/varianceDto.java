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
public class varianceDto {

	
	private String varianceID;
	private String branchID;
	private String contractID;
	private int year;
	private String month;
	private double varianceAmount;
	private String remark;
	private LocalDate dateTime;
	private LocalDate flag;
	
	private RentContractDto info;
}
