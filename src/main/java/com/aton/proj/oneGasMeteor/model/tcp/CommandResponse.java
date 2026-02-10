package com.aton.proj.oneGasMeteor.model.tcp;

public record CommandResponse(String asciiCommand, byte[] binaryData) {
	public CommandResponse(String asciiCommand) {
		this(asciiCommand, asciiCommand.getBytes());
	}
}