package com.cagl.entity;

import java.time.LocalDate;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToOne;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
public class SDRecords {

	@Id
	private String sdID;
	private String contractID;
	private int year;
	private String month;
	private double sdAmount;
	private String remark;
	private String timeZone;
	private LocalDate flag;
	
//	@OneToOne
//	@JoinColumn(name = "contractID")
//	private RentContract contract;
}
