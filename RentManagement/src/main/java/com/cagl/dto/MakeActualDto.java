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
public class MakeActualDto {

	private int contractID;
	private String branchID;
	private String month;
	private int year;
	private double amount;
	private LocalDate startDate;
	private LocalDate endDate;
	private double monthRent;
}
