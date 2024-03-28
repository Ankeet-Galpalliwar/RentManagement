package com.cagl.customexceptation;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import com.cagl.dto.Responce;

@ControllerAdvice
public class CustomeExceptationHandler {
	@ExceptionHandler(PaymentReport.class)
	public ResponseEntity<Responce> handleCustomException(PaymentReport ex) {
		return ResponseEntity.status(HttpStatus.OK)
				.body(Responce.builder().error(Boolean.TRUE).msg(ex.getMessage()).data("-NA-").build());
	}

	@ExceptionHandler(InvalidUser.class)
	public ResponseEntity<String> InvalidUser(InvalidUser msg) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(msg.getMessage() + "..!");
	}

}
