package com.cagl.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SDRecoardDto {
	private String sdID;
	private int contractID;
	private int year;
	private String month;
	private double sdAmount;
	private String remark;
	private String timeZone;
	private LocalDate flag;
}
