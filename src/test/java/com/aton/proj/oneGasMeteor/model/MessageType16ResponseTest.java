package com.aton.proj.oneGasMeteor.model;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

/**
 * Test della classe MessageType16Response.
 *
 * Verifica il calcolo dei campi derivati (calcolati a partire da dati grezzi):
 *  - averageSendTime   = totalSendTime / messageCount
 *  - averageRssi       = rssiTotal / rssiValidCount
 *  - deliverySuccessRate = ((messageCount - failCount) / messageCount) * 100
 */
class MessageType16ResponseTest {

	// ====================== calculateDerivedFields ======================

	/**
	 * Input:
	 *   messageCount      = 100
	 *   deliveryFailCount = 10
	 *   totalSendTime     = 5000
	 *   rssiTotal         = 900
	 *   rssiValidCount    = 90
	 *
	 * Output atteso:
	 *   averageSendTime     = 5000 / 100 = 50.0
	 *   deliverySuccessRate = (100 - 10) / 100 * 100 = 90.0%
	 *   averageRssi         = 900 / 90 = 10.0
	 *
	 * Verifica il calcolo standard dei tre campi derivati.
	 */
	@Test
	void testCalculateDerivedFields_standardValues() {
		MessageType16Response response = new MessageType16Response();
		response.setMessageCount(100);
		response.setDeliveryFailCount(10);
		response.setTotalSendTime(5000L);
		response.setRssiTotal(900L);
		response.setRssiValidCount(90);

		response.calculateDerivedFields();

		assertEquals(50.0, response.getAverageSendTime(), 0.001);
		assertEquals(90.0, response.getDeliverySuccessRate(), 0.001);
		assertEquals(10.0, response.getAverageRssi(), 0.001);
	}

	/**
	 * Input:
	 *   messageCount      = 200
	 *   deliveryFailCount = 0   (nessun fallimento)
	 *   totalSendTime     = 10000
	 *   rssiTotal         = 1000
	 *   rssiValidCount    = 200
	 *
	 * Output atteso:
	 *   deliverySuccessRate = 100.0%
	 *   averageSendTime     = 50.0
	 *   averageRssi         = 5.0
	 *
	 * Verifica che con zero fallimenti il tasso di consegna sia 100%.
	 */
	@Test
	void testCalculateDerivedFields_noFailures() {
		MessageType16Response response = new MessageType16Response();
		response.setMessageCount(200);
		response.setDeliveryFailCount(0);
		response.setTotalSendTime(10000L);
		response.setRssiTotal(1000L);
		response.setRssiValidCount(200);

		response.calculateDerivedFields();

		assertEquals(100.0, response.getDeliverySuccessRate(), 0.001);
		assertEquals(50.0, response.getAverageSendTime(), 0.001);
		assertEquals(5.0, response.getAverageRssi(), 0.001);
	}

	/**
	 * Input:
	 *   messageCount   = 0  (nessun messaggio inviato)
	 *   totalSendTime  = 0
	 *   rssiValidCount = 0
	 *
	 * Output atteso:
	 *   averageSendTime     = null  (divisione per zero evitata)
	 *   deliverySuccessRate = null
	 *   averageRssi         = null
	 *
	 * Verifica che quando messageCount è zero non si verifichino divisioni per zero
	 * e i campi derivati rimangano null.
	 */
	@Test
	void testCalculateDerivedFields_zeroMessageCount() {
		MessageType16Response response = new MessageType16Response();
		response.setMessageCount(0);
		response.setDeliveryFailCount(0);
		response.setTotalSendTime(0L);
		response.setRssiTotal(0L);
		response.setRssiValidCount(0);

		// Non deve lanciare ArithmeticException
		assertDoesNotThrow(() -> response.calculateDerivedFields());

		assertNull(response.getAverageSendTime());
		assertNull(response.getDeliverySuccessRate());
		assertNull(response.getAverageRssi());
	}

	/**
	 * Input:
	 *   messageCount      = null (non settato)
	 *   deliveryFailCount = null
	 *   totalSendTime     = null
	 *   rssiTotal         = null
	 *   rssiValidCount    = null
	 *
	 * Output atteso:
	 *   averageSendTime     = null
	 *   deliverySuccessRate = null
	 *   averageRssi         = null
	 *
	 * Verifica che quando tutti i valori di input sono null, il metodo non generi
	 * NullPointerException e i campi rimangano null.
	 */
	@Test
	void testCalculateDerivedFields_nullValues() {
		MessageType16Response response = new MessageType16Response();
		// Non impostare nessun valore → tutti null

		assertDoesNotThrow(() -> response.calculateDerivedFields());

		assertNull(response.getAverageSendTime());
		assertNull(response.getDeliverySuccessRate());
		assertNull(response.getAverageRssi());
	}

	/**
	 * Input:
	 *   messageCount      = 1   (un solo messaggio)
	 *   deliveryFailCount = 1   (l'unico messaggio è fallito)
	 *   totalSendTime     = 500
	 *   rssiTotal         = 20
	 *   rssiValidCount    = 1
	 *
	 * Output atteso:
	 *   deliverySuccessRate = 0.0%  ((1-1)/1 * 100)
	 *   averageSendTime     = 500.0
	 *   averageRssi         = 20.0
	 *
	 * Verifica il caso limite in cui tutti i messaggi sono falliti (0% success).
	 */
	@Test
	void testCalculateDerivedFields_allFailures() {
		MessageType16Response response = new MessageType16Response();
		response.setMessageCount(1);
		response.setDeliveryFailCount(1);
		response.setTotalSendTime(500L);
		response.setRssiTotal(20L);
		response.setRssiValidCount(1);

		response.calculateDerivedFields();

		assertEquals(0.0, response.getDeliverySuccessRate(), 0.001);
		assertEquals(500.0, response.getAverageSendTime(), 0.001);
		assertEquals(20.0, response.getAverageRssi(), 0.001);
	}

	/**
	 * Input:
	 *   messageCount      = 150
	 *   deliveryFailCount = 3
	 *   totalSendTime     = 45000
	 *   rssiTotal         = 2500
	 *   rssiValidCount    = 148
	 *
	 * Output atteso:
	 *   deliverySuccessRate ≈ 98.0%     ((150-3)/150 * 100)
	 *   averageSendTime     = 300.0     (45000/150)
	 *   averageRssi         ≈ 16.89     (2500/148)
	 *
	 * Verifica valori realistici tratti da un dispositivo TEK822 in campo.
	 */
	@Test
	void testCalculateDerivedFields_realisticValues() {
		MessageType16Response response = new MessageType16Response();
		response.setMessageCount(150);
		response.setDeliveryFailCount(3);
		response.setTotalSendTime(45000L);
		response.setRssiTotal(2500L);
		response.setRssiValidCount(148);

		response.calculateDerivedFields();

		assertEquals(98.0, response.getDeliverySuccessRate(), 0.01);
		assertEquals(300.0, response.getAverageSendTime(), 0.001);
		assertEquals(2500.0 / 148.0, response.getAverageRssi(), 0.001);
	}

	// ====================== Getters & Setters ======================

	/**
	 * Input:  tutti i campi di MessageType16Response
	 * Output: stesso valore recuperato con i getter
	 *
	 * Verifica che i getter restituiscano i valori impostati dai setter.
	 */
	@Test
	void testGettersAndSetters() {
		MessageType16Response response = new MessageType16Response();

		response.setDeviceId("DEV001");
		response.setDeviceType("TEK822V2");
		response.setIccid("89882390000028895236");
		response.setEnergyUsed(19875L);
		response.setMinTemperature(5);
		response.setMaxTemperature(55);
		response.setMessageCount(150);
		response.setDeliveryFailCount(3);
		response.setTotalSendTime(45000L);
		response.setMaxSendTime(1200L);
		response.setMinSendTime(800L);
		response.setRssiTotal(2500L);
		response.setRssiValidCount(148);
		response.setRssiFailCount(2);

		assertEquals("DEV001", response.getDeviceId());
		assertEquals("TEK822V2", response.getDeviceType());
		assertEquals("89882390000028895236", response.getIccid());
		assertEquals(19875L, response.getEnergyUsed());
		assertEquals(5, response.getMinTemperature());
		assertEquals(55, response.getMaxTemperature());
		assertEquals(150, response.getMessageCount());
		assertEquals(3, response.getDeliveryFailCount());
		assertEquals(45000L, response.getTotalSendTime());
		assertEquals(1200L, response.getMaxSendTime());
		assertEquals(800L, response.getMinSendTime());
		assertEquals(2500L, response.getRssiTotal());
		assertEquals(148, response.getRssiValidCount());
		assertEquals(2, response.getRssiFailCount());
	}
}
