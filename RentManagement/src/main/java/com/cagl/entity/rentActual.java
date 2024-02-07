package com.cagl.entity;

import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class rentActual {

	
	@Id
	private String rentActualID;
	private String BranchID;
	private int ContractID;
	private int year;
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
}
