package com.cagl.entity;

import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.Id;

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
public class provision {
	
	@Id
	private String provisionID;
	private boolean provisiontype;
	private String branchID;
	private String contractID;
	private int  year;
	private String month;
	private double provisionAmount;
	private String remark;
	private LocalDate dateTime;
	

}
