package com.cagl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BranchDto {
	
	private String branchID;
	private String branchName;
	private String areaName;
	private String zone;
	private String region;
	private String state;
}
