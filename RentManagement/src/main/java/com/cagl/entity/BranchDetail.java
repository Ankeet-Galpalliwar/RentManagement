package com.cagl.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;
@Entity
@Data
public class BranchDetail {
	@Id
	private String branchID;
	private String branchName;
	private String areaName;
	private String zone;
	private String region;
	private String state;
	private String bmName;
	private String bmContact;
	private String bmMailID;
	private String amName;
	private String amContact;
	private String amMailID;
	private String dmName;
	private String dmContactNumber;
	private String dmMailID;
	private String zmName;
	private String zmContactNumber;
	private String zmMailID;
	private String bhName;
	private String bhContactNumber;
	private String bhMailID;
	private String pmName;
	private String pmContactNumber;
	private String pmMailID;
	private String branchAddress;
}
