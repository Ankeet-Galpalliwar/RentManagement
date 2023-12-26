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
public class Rentduecalculation {
	private String branchID;
	private int contractID;
	private String lesseeBranchType;
	private LocalDate rentStartDate;
	private LocalDate rentEndDate;
	private String escalation;
	private String renewalTenure;
	private double monthlyRent;
}
