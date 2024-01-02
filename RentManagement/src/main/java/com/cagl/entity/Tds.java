package com.cagl.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
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
@Data
public class Tds {
	
	@Id
	private String rentTdsID;
	private String branchID;
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
	private int year;


}
