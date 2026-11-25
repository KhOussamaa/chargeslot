package com.chargeslot;

import com.chargeslot.domain.*;
import com.chargeslot.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.time.*;
import java.util.Map;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties={"spring.datasource.url=jdbc:h2:mem:elaboration;DB_CLOSE_DELAY=-1", "spring.jpa.hibernate.ddl-auto=create-drop", "chargeslot.demo-data=false"})
@AutoConfigureMockMvc
class ElaborationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired StationRepository stations;
    @Autowired ConnectorRepository connectors;
    @Autowired VehicleRepository vehicles;
    @Autowired ReservationRepository reservations;
    @Test void executableArchitectureAcceptsNominalAndRejectsOverlap() throws Exception {
        ChargingStation station = stations.saveAndFlush(new ChargingStation("Elaboration",new BigDecimal("80")));
        Connector connector = connectors.saveAndFlush(new Connector(station,"CCS",ConnectorType.CCS,new BigDecimal("60")));
        Vehicle vehicle = vehicles.saveAndFlush(new Vehicle(17L,"Alice",ConnectorType.CCS,new BigDecimal("50")));
        Instant start = Instant.now().plus(Duration.ofDays(1)).truncatedTo(java.time.temporal.ChronoUnit.HOURS);
        byte[] request = json.writeValueAsBytes(Map.of("driverId",17,"vehicleId",vehicle.getId(),"connectorId",connector.getId(),"startAt",start,"endAt",start.plus(Duration.ofHours(1)),"requestedPowerKw",40));
        mvc.perform(post("/api/reservations").with(httpBasic("alice","AliceDemo21!")).with(csrf()).contentType("application/json").content(request))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("CONFIRMED"));
        mvc.perform(post("/api/reservations").with(httpBasic("alice","AliceDemo21!")).with(csrf()).contentType("application/json").content(request))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OVERLAP"));
        assertThat(reservations.count()).isEqualTo(1);
    }
}
