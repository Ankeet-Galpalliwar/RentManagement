package com.cagl.dto;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class provisionDto {
	
	private String provisionID;
	private String branchID;
	private String contractID;
	private int  year;
	private String month;
	private String provisionAmount;
	private String remark;
	private LocalDate dateTime;

}
