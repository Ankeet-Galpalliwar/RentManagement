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

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Entity
@Builder
public class RfBranchMaster {

	@Id
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
