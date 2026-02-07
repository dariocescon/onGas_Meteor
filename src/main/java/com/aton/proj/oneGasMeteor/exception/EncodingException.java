package com.aton.proj.oneGasMeteor.exception;

public class EncodingException extends RuntimeException {
    
	private static final long serialVersionUID = -5150826711921281828L;

	public EncodingException(String message) {
        super(message);
    }
    
    public EncodingException(String message, Throwable cause) {
        super(message, cause);
    }
}