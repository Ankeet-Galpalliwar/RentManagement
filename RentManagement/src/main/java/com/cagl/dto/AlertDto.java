package com.cagl.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Data
@ToString
@Builder
public class AlertDto {

	private String contractID;
	private String branchid;
	private String lessee_branch_name;
	private String lessorName;
	private String rent_start_date;
	private String rentEndDate;
	private String monthYear;
	private String actualAmount;

}
