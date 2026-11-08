package com.lms.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@SpringBootTest
class HealthIntegrationTest {

	@Autowired
	private WebApplicationContext context;

	@Test
	void healthEndpointsArePublicAndMinimal() throws Exception {
		MockMvc mockMvc = MockMvcBuilders.webAppContextSetup(context).build();

		var liveness = mockMvc.perform(get("/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andReturn();

		assertThat(liveness.getResponse().getContentAsString()).isEqualTo("{\"status\":\"UP\"}");

		var readiness = mockMvc.perform(get("/health/ready"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andReturn();

		assertThat(readiness.getResponse().getContentAsString()).isEqualTo("{\"status\":\"UP\"}");
	}
}
