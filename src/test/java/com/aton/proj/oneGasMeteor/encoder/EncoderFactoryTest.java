package com.aton.proj.oneGasMeteor.encoder;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aton.proj.oneGasMeteor.encoder.impl.NoOpEncoder;
import com.aton.proj.oneGasMeteor.encoder.impl.Tek822Encoder;

/**
 * Test della classe EncoderFactory.
 *
 * Verifica che la factory selezioni l'encoder corretto in base al deviceType:
 *  - Tek822Encoder per i 14 device type TEK supportati
 *  - NoOpEncoder (fallback) per device type sconosciuti o null/vuoti
 */
class EncoderFactoryTest {

	private EncoderFactory factory;

	@BeforeEach
	void setUp() {
		// Inizializza la factory con Tek822Encoder come encoder primario
		// e NoOpEncoder come fallback
		factory = new EncoderFactory(
				List.of(new Tek822Encoder()),
				new NoOpEncoder()
		);
	}

	// ====================== Selezione encoder TEK822 ======================

	/**
	 * Input:  deviceType = "TEK822V2"
	 * Output: Tek822Encoder (nome "Tek822Encoder")
	 *
	 * Verifica che il device type TEK822V2, il tipo più comune, venga
	 * associato correttamente al Tek822Encoder.
	 */
	@Test
	void testGetEncoder_tek822v2_returnsTek822Encoder() {
		DeviceEncoder encoder = factory.getEncoder("TEK822V2");
		assertEquals("Tek822Encoder", encoder.getEncoderName());
	}

	/**
	 * Input:  deviceType = "TEK822V1"
	 * Output: Tek822Encoder (nome "Tek822Encoder")
	 *
	 * Verifica che TEK822V1 venga selezionato dal Tek822Encoder.
	 */
	@Test
	void testGetEncoder_tek822v1_returnsTek822Encoder() {
		DeviceEncoder encoder = factory.getEncoder("TEK822V1");
		assertEquals("Tek822Encoder", encoder.getEncoderName());
	}

	/**
	 * Input:  deviceType = "TEK586"
	 * Output: Tek822Encoder
	 *
	 * Verifica che i device type della famiglia TEK586 vengano gestiti
	 * da Tek822Encoder.
	 */
	@Test
	void testGetEncoder_tek586_returnsTek822Encoder() {
		DeviceEncoder encoder = factory.getEncoder("TEK586");
		assertEquals("Tek822Encoder", encoder.getEncoderName());
	}

	/**
	 * Input:  tutti i 14 device type supportati da Tek822Encoder
	 * Output: Tek822Encoder per ciascuno
	 *
	 * Verifica che tutti i tipi di dispositivo supportati dal protocollo TEK822
	 * vengano selezionati correttamente.
	 */
	@Test
	void testGetEncoder_allSupportedTek822Types() {
		List<String> supportedTypes = List.of(
				"TEK586", "TEK733", "TEK643", "TEK811",
				"TEK822V1", "TEK733A", "TEK871", "TEK811A",
				"TEK822V1BTN", "TEK822V2", "TEK900", "TEK880",
				"TEK898V2", "TEK898V1"
		);

		for (String deviceType : supportedTypes) {
			DeviceEncoder encoder = factory.getEncoder(deviceType);
			assertEquals("Tek822Encoder", encoder.getEncoderName(),
					"Device type " + deviceType + " dovrebbe usare Tek822Encoder");
		}
	}

	// ====================== Fallback a NoOpEncoder ======================

	/**
	 * Input:  deviceType = "UNKNOWN_DEVICE"
	 * Output: NoOpEncoder (fallback)
	 *
	 * Verifica che un device type sconosciuto venga gestito dal NoOpEncoder,
	 * evitando errori e garantendo una risposta vuota.
	 */
	@Test
	void testGetEncoder_unknownDeviceType_returnsNoOpEncoder() {
		DeviceEncoder encoder = factory.getEncoder("UNKNOWN_DEVICE");
		assertEquals("NoOpEncoder", encoder.getEncoderName());
	}

	/**
	 * Input:  deviceType = null
	 * Output: NoOpEncoder (fallback)
	 *
	 * Verifica che un device type null venga gestito dal fallback senza eccezioni.
	 */
	@Test
	void testGetEncoder_nullDeviceType_returnsNoOpEncoder() {
		DeviceEncoder encoder = factory.getEncoder(null);
		assertEquals("NoOpEncoder", encoder.getEncoderName());
	}

	/**
	 * Input:  deviceType = "" (stringa vuota)
	 * Output: NoOpEncoder (fallback)
	 *
	 * Verifica che una stringa vuota per il device type venga gestita
	 * gracefully restituendo il NoOpEncoder.
	 */
	@Test
	void testGetEncoder_emptyDeviceType_returnsNoOpEncoder() {
		DeviceEncoder encoder = factory.getEncoder("");
		assertEquals("NoOpEncoder", encoder.getEncoderName());
	}

	/**
	 * Input:  deviceType = "TEK999" (non in lista supportati)
	 * Output: NoOpEncoder (fallback)
	 *
	 * Verifica che un device type non esistente nella lista dei supportati
	 * venga rediretto al fallback encoder.
	 */
	@Test
	void testGetEncoder_nonExistentTekType_returnsNoOpEncoder() {
		DeviceEncoder encoder = factory.getEncoder("TEK999");
		assertEquals("NoOpEncoder", encoder.getEncoderName());
	}

	// ====================== canEncode dei singoli encoder ======================

	/**
	 * Verifica i metodi canEncode() dei due encoder direttamente.
	 *
	 * Tek822Encoder.canEncode("TEK822V2") → true
	 * Tek822Encoder.canEncode("UNKNOWN")  → false
	 * NoOpEncoder.canEncode(qualsiasi)    → true (è il fallback)
	 */
	@Test
	void testCanEncode_directVerification() {
		Tek822Encoder tek822 = new Tek822Encoder();
		NoOpEncoder noOp = new NoOpEncoder();

		// Tek822Encoder supporta i device type TEK
		assertTrue(tek822.canEncode("TEK822V2"));
		assertFalse(tek822.canEncode("UNKNOWN"));
		assertFalse(tek822.canEncode(null));

		// NoOpEncoder è fallback: accetta tutto
		assertTrue(noOp.canEncode("TEK822V2"));
		assertTrue(noOp.canEncode("UNKNOWN"));
		assertTrue(noOp.canEncode(null));
	}
}
