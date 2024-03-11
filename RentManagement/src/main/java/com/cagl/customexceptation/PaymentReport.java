package com.cagl.customexceptation;

public class PaymentReport extends RuntimeException {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public PaymentReport(String massage) {
		super(massage);
	}

}
