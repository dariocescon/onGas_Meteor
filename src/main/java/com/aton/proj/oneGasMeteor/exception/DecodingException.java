package com.aton.proj.oneGasMeteor.exception;

public class DecodingException extends RuntimeException {

	private static final long serialVersionUID = 2917029462150696530L;

	public DecodingException(String message) {
		super(message);
	}

	public DecodingException(String message, Throwable cause) {
		super(message, cause);
	}
}