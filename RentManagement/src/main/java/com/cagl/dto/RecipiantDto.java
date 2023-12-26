package com.cagl.dto;

import com.cagl.entity.RentContract;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecipiantDto {

	private int id;
	private String lessorRecipiantsName;
	private String lessorBankName;
	private String lessorBranchName;
	private String lessorIfscNumber;
	private String lessorAccountNumber;
	private String panNo;
	private String gstNo;
	private double lessorRentAmount;
	private RentContract rentContractRecipiant;

}
