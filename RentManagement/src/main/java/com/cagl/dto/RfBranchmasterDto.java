package com.cagl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RfBranchmasterDto {

	
	private String rfBranchID;
	private String branchName;
	private String areaName;
	private String region;
	private String branchMailID;
	private String amName;
	private String anGkID;
	private String amMailID;
	private String amContactNumber;
	private String rmName;
	private String rmGkID;
	private String rmMailID;
	private String rmContactNumber;
}
