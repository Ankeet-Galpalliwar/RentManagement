package com.cagl.entity;

import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;

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
public class Variance {

	@Id
	private String varianceID;
	private String branchID;
	private String contractID;
	private int year;
	private String month;
	private double varianceAmount;
	private String remark;
	private LocalDate dateTime;
	private LocalDate flag;
	
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name="contractInfo")
	private RentContract contractInfo;
}
