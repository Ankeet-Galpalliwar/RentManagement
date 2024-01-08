package com.cagl.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PaymentReport {

	@Id
	private String ID;
	private double due;
	private double provision;
	private double Gross;
	private double tds;
	private double net;
	private double monthlyRent;
	private String month;
	private String year;
	private String contractID;
	private String branchID;

}
