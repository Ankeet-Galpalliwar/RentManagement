package com.cagl.dto;

import java.time.LocalDate;

import com.cagl.entity.RentDue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;



@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class RentDueDto {

	
	private String rentDueID;
	private int ContractID;
	private double january;
	private double february;
	private double march;
	private double april;
	private double may;
	private double june;
	private double july;
	private double august;
	private double september;
	private double october;
	private double november;
	private double december;
	private LocalDate startDate;
	private LocalDate endDate;
	private int year;
	private double escalation;
	private String Status;
	
	
	
}
