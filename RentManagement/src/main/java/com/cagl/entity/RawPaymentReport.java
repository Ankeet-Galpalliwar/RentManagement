package com.cagl.entity;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RawPaymentReport {
	
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

	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "contractInfo")
	@ToString.Exclude
	private RentContract contractInfo;

}
