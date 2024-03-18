package com.cagl.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class ConfirmPaymentReport {

	@Id
//	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private String ID;
	private double due;
	private double provision;
	private double Gross;
	private double tds;
	private double net;
	private double GST;
	private double monthlyRent;
	private String month;
	private String year;
	private String ActualAmount;
	private String contractID;
	private String branchID;
	private boolean Redflag;// Base on Raw_Report Actual And Gross Difference

}
