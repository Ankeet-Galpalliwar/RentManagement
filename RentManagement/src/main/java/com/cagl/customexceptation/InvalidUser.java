package com.cagl.customexceptation;

public class InvalidUser extends RuntimeException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public InvalidUser() {
		super("Invalid Username and Password");
	}
}
