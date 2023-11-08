package com.cagl.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Responce {

	
	private Boolean error;
	private Object data;
	private String msg;
}
