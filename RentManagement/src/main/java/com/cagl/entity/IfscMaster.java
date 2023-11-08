package com.cagl.entity;

import javax.persistence.Entity;
import javax.persistence.Id;

import lombok.Data;

@Entity
@Data
public class IfscMaster {
	
	@Id
	private String ifsc;
	private String bankName;
	private String branchName;
	private String address;
	private String district;
	private String state;
	private String city;
	private String micr;

}
