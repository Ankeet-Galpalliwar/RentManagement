package com.cagl.entity;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

import com.fasterxml.jackson.annotation.JsonIgnore;

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
public class Recipiant {

	@Id
	private String recipiantsID;
	private String lessorRecipiantsName;
	private String lessorBankName;
	private String lessorBranchName;
	private String lessorIfscNumber;
	private String lessorAccountNumber;
	private String panNo;
	private String gstNo;
	private double lessorRentAmount;
	
	@ToString.Exclude
	@ManyToOne(cascade = CascadeType.PERSIST)
	@JsonIgnore
	@JoinColumn(name = "rentcontractID")
	private RentContract rentContractRecipiant;

}
