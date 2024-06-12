package com.cagl.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Data
public class DashBoardInfo {


	private String provisionPreviousMonth;
	private String provisionPreviousMonthSum;
	private String provisionCurrentMonth;
	private String provisionCurrentMonthSum;
//=================================================

	private String grossPreviousMonth;
	private String grossPreviousMonthSum;
	private String grossCurrentMonth;
	private String grossCurrentMonthSum;
}
