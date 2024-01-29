package com.cagl.entity;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
@Data
public class ApiCallRecords {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private long id;
	private String apiname;
	private String timeZone;
	private String msg;

}
