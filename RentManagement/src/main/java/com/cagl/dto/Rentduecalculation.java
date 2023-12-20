package com.cagl.dto;

import java.time.LocalDate;
import java.util.List;

import org.springframework.cglib.core.Local;

import com.cagl.entity.Recipiant;

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
	private String contractID;
	private String lesseeBranchType;
	private LocalDate rentStartDate;
	private LocalDate rentEndDate;
	private String escalation;
	private String renewalTenure;
	private double monthlyRent;
}
