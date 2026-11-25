package com.chargeslot;

import com.chargeslot.domain.*;
import com.chargeslot.dto.ApiModels.*;
import com.chargeslot.exception.ApiException;
import com.chargeslot.repository.*;
import com.chargeslot.service.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:chargeslot-test;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=15000", "spring.jpa.hibernate.ddl-auto=create-drop", "chargeslot.demo-data=false"})
@AutoConfigureMockMvc
@Import(ReservationIntegrationTest.TimeConfig.class)
class ReservationIntegrationTest {
    @TestConfiguration static class TimeConfig {
        @Bean @Primary Clock testClock() { return Clock.fixed(Instant.parse("2030-01-01T00:00:00Z"), ZoneOffset.UTC); }
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired StationRepository stations;
    @Autowired ConnectorRepository connectors;
    @Autowired VehicleRepository vehicles;
    @Autowired ReservationRepository reservations;
    @Autowired MaintenanceRepository maintenances;
    @Autowired NotificationRepository notifications;
    @Autowired ReservationService service;
    @MockitoBean NotificationGateway gateway;
    private ChargingStation station;
    private Connector a,b,type2;
    private Vehicle alice,bob,aliceType2;
    private static final Instant START = Instant.parse("2030-01-02T14:00:00Z");
    private static final Instant END = START.plus(Duration.ofHours(1));
    @BeforeEach void setup() {
        reset(gateway);
        notifications.deleteAll(); reservations.deleteAll(); maintenances.deleteAll(); connectors.deleteAll(); vehicles.deleteAll(); stations.deleteAll();
        station = stations.saveAndFlush(new ChargingStation("Test",new BigDecimal("80")));
        a = connectors.saveAndFlush(new Connector(station,"A",ConnectorType.CCS,new BigDecimal("60")));
        b = connectors.saveAndFlush(new Connector(station,"B",ConnectorType.CCS,new BigDecimal("60")));
        type2 = connectors.saveAndFlush(new Connector(station,"Type 2",ConnectorType.TYPE2,new BigDecimal("22")));
        alice = vehicles.saveAndFlush(new Vehicle(17L,"Alice CCS",ConnectorType.CCS,new BigDecimal("50")));
        bob = vehicles.saveAndFlush(new Vehicle(18L,"Bob CCS",ConnectorType.CCS,new BigDecimal("60")));
        aliceType2 = vehicles.saveAndFlush(new Vehicle(17L,"Alice Type 2",ConnectorType.TYPE2,new BigDecimal("22")));
    }
    private String password(String user) { return switch (user) { case "alice" -> "AliceDemo21!"; case "bob" -> "BobDemo21!"; default -> "OperateurDemo21!"; }; }
    private ReservationRequest request(Connector connector, Vehicle vehicle, Instant start, Instant end, String power) {
        return new ReservationRequest(vehicle.getDriverId(),vehicle.getId(),connector.getId(),start,end,new BigDecimal(power));
    }
    private ResultActions reserve(String user, ReservationRequest request) throws Exception {
        return mvc.perform(post("/api/reservations").with(httpBasic(user,password(user))).with(csrf()).contentType("application/json").content(json.writeValueAsBytes(request)));
    }
    private JsonNode createAlice(String power) throws Exception {
        String body = reserve("alice",request(a,alice,START,END,power)).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(body);
    }
    @Test void nominalScenarioPersistsAndNotifiesAfterCommit() throws Exception {
        JsonNode created = createAlice("40");
        assertThat(created.get("status").asText()).isEqualTo("CONFIRMED");
        assertThat(reservations.count()).isEqualTo(1);
        assertThat(notifications.findAll()).singleElement().satisfies(n -> { assertThat(n.getStatus()).isEqualTo("SENT"); assertThat(n.getEventType()).isEqualTo("CONFIRMED"); });
        verify(gateway).send(eq(17L),contains("CONFIRMED"));
    }
    @Test void overlappingRequestReturns409WithoutCreatingAnotherReservation() throws Exception {
        createAlice("30");
        reserve("bob",request(a,bob,START.plusSeconds(1800),END.plusSeconds(1800),"30"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("OVERLAP"));
        assertThat(reservations.count()).isEqualTo(1); assertThat(notifications.count()).isEqualTo(1);
    }
    @Test void adjacentRequestIsAccepted() throws Exception {
        createAlice("30"); reserve("bob",request(a,bob,END,END.plusSeconds(3600),"30")).andExpect(status().isCreated());
    }
    @Test void stationPowerIsSharedBetweenConnectors() throws Exception {
        createAlice("50");
        reserve("bob",request(b,bob,START,END,"40")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("POWER_ADJUSTMENT_REQUIRED")).andExpect(jsonPath("$.availablePowerKw").value(30));
        assertThat(reservations.count()).isEqualTo(1);
    }
    @Test void anotherConnectorWithinPowerBudgetIsAccepted() throws Exception {
        createAlice("50"); reserve("bob",request(b,bob,START,END,"30")).andExpect(status().isCreated());
    }
    @Test void consecutiveExistingLoadsDoNotCauseFalsePowerConflict() throws Exception {
        reserve("bob",request(b,bob,START,END,"50")).andExpect(status().isCreated());
        reserve("bob",request(b,bob,END,END.plusSeconds(3600),"50")).andExpect(status().isCreated());
        reserve("alice",request(a,alice,START,END.plusSeconds(3600),"30")).andExpect(status().isCreated());
    }
    @Test void incompatibleVehicleIsRefused() throws Exception {
        reserve("alice",request(type2,alice,START,END,"20")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INCOMPATIBLE"));
    }
    @Test void excessiveVehiclePowerIsRefused() throws Exception {
        reserve("alice",request(a,alice,START,END,"55")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VEHICLE_POWER_LIMIT"));
    }
    @Test void stationMaintenanceBlocksReservation() throws Exception {
        maintenances.saveAndFlush(new MaintenanceWindow(station,null,START,END,"Controle"));
        reserve("alice",request(a,alice,START,END,"20")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("MAINTENANCE"));
    }
    @Test void cancellationReleasesSlotAndIsIdempotent() throws Exception {
        long id = createAlice("40").get("id").asLong();
        mvc.perform(post("/api/reservations/{id}/cancel",id).with(httpBasic("alice",password("alice"))).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mvc.perform(post("/api/reservations/{id}/cancel",id).with(httpBasic("alice",password("alice"))).with(csrf())).andExpect(status().isOk());
        reserve("bob",request(a,bob,START,END,"40")).andExpect(status().isCreated());
        assertThat(notifications.findAll()).filteredOn(n -> n.getEventType().equals("CANCELLED")).hasSize(1);
    }
    @Test void anotherDriverCannotCancel() throws Exception {
        long id = createAlice("40").get("id").asLong();
        mvc.perform(post("/api/reservations/{id}/cancel",id).with(httpBasic("bob",password("bob"))).with(csrf())).andExpect(status().isForbidden());
        assertThat(reservations.findById(id).orElseThrow().getStatus()).isEqualTo(ReservationStatus.CONFIRMED);
    }
    @Test void operatorCanCancel() throws Exception {
        long id = createAlice("40").get("id").asLong();
        mvc.perform(post("/api/reservations/{id}/cancel",id).with(httpBasic("operateur",password("operateur"))).with(csrf())).andExpect(status().isOk());
    }
    @Test void notificationFailureDoesNotRollBackReservation() throws Exception {
        doThrow(new IllegalStateException("Simulated transport failure")).when(gateway).send(anyLong(),anyString());
        createAlice("40"); assertThat(reservations.count()).isEqualTo(1);
        assertThat(notifications.findAll()).singleElement().satisfies(n -> assertThat(n.getStatus()).isEqualTo("FAILED"));
    }
    @Test void availabilityExcludesBookedAndIncompatibleConnectors() throws Exception {
        createAlice("40");
        mvc.perform(get("/api/availability").with(httpBasic("bob",password("bob")))
            .param("vehicleId",bob.getId().toString()).param("stationId",station.getId().toString()).param("connectorType","CCS")
            .param("startAt",START.toString()).param("endAt",END.toString()).param("requestedPowerKw","30"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].connectorId").value(b.getId()));
    }
    @Test void driverCannotImpersonateAnotherDriver() throws Exception {
        reserve("alice",request(a,bob,START,END,"20")).andExpect(status().isForbidden());
    }
    @Test void vehicleAndDeclaredDriverMustAgree() throws Exception {
        ReservationRequest invalid = new ReservationRequest(17L,bob.getId(),a.getId(),START,END,new BigDecimal("20"));
        reserve("alice",invalid).andExpect(status().isBadRequest());
    }
    @Test void invalidDatesAreRejected() throws Exception { reserve("alice",request(a,alice,END,START,"20")).andExpect(status().isBadRequest()); }
    @Test void pastDatesAreRejected() throws Exception { reserve("alice",request(a,alice,Instant.parse("2020-01-01T10:00:00Z"),Instant.parse("2020-01-01T11:00:00Z"),"20")).andExpect(status().isBadRequest()); }
    @Test void zeroPowerIsRejectedByValidation() throws Exception { reserve("alice",request(a,alice,START,END,"0")).andExpect(status().isBadRequest()); }
    @Test void missingConnectorReturns404() throws Exception {
        reserve("alice",new ReservationRequest(17L,alice.getId(),999999L,START,END,new BigDecimal("20"))).andExpect(status().isNotFound());
    }
    @Test void authenticationIsRequired() throws Exception { mvc.perform(get("/api/reservations")).andExpect(status().isUnauthorized()); }
    @Test void creationLocationCanBeReadByOwnerOnly() throws Exception {
        long id = createAlice("40").get("id").asLong();
        mvc.perform(get("/api/reservations/{id}",id).with(httpBasic("alice",password("alice")))).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/reservations/{id}",id).with(httpBasic("bob",password("bob")))).andExpect(status().isForbidden());
    }
    @Test void csrfIsRequiredForWrites() throws Exception {
        mvc.perform(post("/api/reservations").with(httpBasic("alice",password("alice"))).contentType("application/json").content(json.writeValueAsBytes(request(a,alice,START,END,"20")))).andExpect(status().isForbidden());
    }
    @Test void operatorCanCreateStationAndConnector() throws Exception {
        String body = mvc.perform(post("/api/stations").with(httpBasic("operateur",password("operateur"))).with(csrf()).contentType("application/json").content("{\"name\":\"Nouvelle station\",\"maximumPowerKw\":100,\"active\":true}"))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();
        mvc.perform(post("/api/stations/{id}/connectors",id).with(httpBasic("operateur",password("operateur"))).with(csrf()).contentType("application/json").content("{\"name\":\"CCS 1\",\"type\":\"CCS\",\"maximumPowerKw\":60,\"active\":true}"))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.stationId").value(id));
    }
    @Test void driverCannotManageInfrastructure() throws Exception {
        mvc.perform(post("/api/stations").with(httpBasic("alice",password("alice"))).with(csrf()).contentType("application/json").content("{\"name\":\"Interdit\",\"maximumPowerKw\":100,\"active\":true}")).andExpect(status().isForbidden());
    }
    @Test void maintenanceCannotInvalidateConfirmedReservation() throws Exception {
        createAlice("40");
        MaintenanceRequest request = new MaintenanceRequest(station.getId(),null,START,END,"Travaux");
        mvc.perform(post("/api/maintenances").with(httpBasic("operateur",password("operateur"))).with(csrf()).contentType("application/json").content(json.writeValueAsBytes(request)))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("RESERVATION_EXISTS"));
    }
    @Test void operatorCanCreateAndDeleteMaintenance() throws Exception {
        MaintenanceRequest request = new MaintenanceRequest(station.getId(),a.getId(),START,END,"Controle");
        String body = mvc.perform(post("/api/maintenances").with(httpBasic("operateur",password("operateur"))).with(csrf()).contentType("application/json").content(json.writeValueAsBytes(request)))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        long id = json.readTree(body).get("id").asLong();
        mvc.perform(delete("/api/maintenances/{id}",id).with(httpBasic("operateur",password("operateur"))).with(csrf())).andExpect(status().isNoContent());
        assertThat(maintenances.count()).isZero();
    }
    @Test void stationPowerCannotBeReducedBelowConfirmedLoad() throws Exception {
        createAlice("50");
        mvc.perform(patch("/api/stations/{id}",station.getId()).with(httpBasic("operateur",password("operateur"))).with(csrf()).contentType("application/json").content("{\"name\":\"Test\",\"maximumPowerKw\":30,\"active\":true}"))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CAPACITY_IN_USE"));
    }
    @Test void deactivatedConnectorIsExcluded() throws Exception {
        mvc.perform(patch("/api/connectors/{id}",a.getId()).with(httpBasic("operateur",password("operateur"))).with(csrf()).contentType("application/json").content("{\"name\":\"A\",\"type\":\"CCS\",\"maximumPowerKw\":60,\"active\":false}")).andExpect(status().isOk());
        reserve("alice",request(a,alice,START,END,"20")).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INACTIVE"));
    }
    @Test void driversOnlySeeTheirOwnReservationsAndNotifications() throws Exception {
        createAlice("40");
        mvc.perform(get("/api/reservations").with(httpBasic("bob",password("bob")))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(get("/api/notifications").with(httpBasic("bob",password("bob")))).andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(0));
    }
    @Test void parallelRequestsCannotDoubleBookConnector() throws Exception {
        int[] outcomes = concurrently(request(a,alice,START,END,"20"),new Actor("alice",17L,false),request(a,bob,START,END,"20"),new Actor("bob",18L,false));
        assertThat(outcomes).containsExactlyInAnyOrder(201,409); assertThat(reservations.count()).isEqualTo(1);
    }
    @Test void parallelRequestsCannotExceedStationPower() throws Exception {
        int[] outcomes = concurrently(request(a,alice,START,END,"50"),new Actor("alice",17L,false),request(b,bob,START,END,"40"),new Actor("bob",18L,false));
        assertThat(outcomes).containsExactlyInAnyOrder(201,409); assertThat(reservations.count()).isEqualTo(1);
    }
    private int[] concurrently(ReservationRequest left, Actor leftActor, ReservationRequest right, Actor rightActor) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        try (ExecutorService executor = Executors.newFixedThreadPool(2)) {
            Future<Integer> first = executor.submit(() -> outcome(start,left,leftActor));
            Future<Integer> second = executor.submit(() -> outcome(start,right,rightActor));
            start.countDown(); return new int[]{first.get(25,TimeUnit.SECONDS),second.get(25,TimeUnit.SECONDS)};
        }
    }
    private int outcome(CountDownLatch start, ReservationRequest request, Actor actor) throws Exception {
        if (!start.await(5,TimeUnit.SECONDS)) throw new IllegalStateException("Start timeout");
        try { service.create(request,actor); return 201; }
        catch (ApiException ex) { return ex.getStatus().value(); }
    }
}
