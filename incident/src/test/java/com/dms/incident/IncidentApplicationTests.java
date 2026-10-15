package com.dms.incident;

import com.dms.incident.repository.IncidentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class IncidentApplicationTests {

	@Autowired
	private IncidentRepository incidentRepository;

	@Test
	void contextLoads() {
		assertNotNull(incidentRepository, "IncidentRepository bean should be created");
		assertTrue(incidentRepository.count() >= 0, "tbl_incidents query should execute cleanly without schema errors");
	}

}
