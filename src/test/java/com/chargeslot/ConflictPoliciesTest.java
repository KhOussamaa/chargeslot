package com.chargeslot;

import com.chargeslot.domain.*;
import com.chargeslot.exception.ApiException;
import com.chargeslot.service.conflict.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;

class ConflictPoliciesTest {
    private ChargingStation station;
    private Connector a, b;
    private Vehicle car;
    private static final Instant BASE = Instant.parse("2030-01-02T00:00:00Z");
    private Instant hour(int hour) { return BASE.plus(Duration.ofHours(hour)); }
    private BigDecimal kw(String value) { return new BigDecimal(value); }
    @BeforeEach void setup() {
        station = new ChargingStation("Test", kw("80")); ReflectionTestUtils.setField(station, "id", 1L);
        a = new Connector(station, "A", ConnectorType.CCS, kw("60")); ReflectionTestUtils.setField(a, "id", 1L);
        b = new Connector(station, "B", ConnectorType.CCS, kw("60")); ReflectionTestUtils.setField(b, "id", 2L);
        car = new Vehicle(17L, "Voiture", ConnectorType.CCS, kw("50"));
    }
    private Reservation reservation(Connector connector, int start, int end, String power) { return new Reservation(connector, car, hour(start), hour(end), kw(power)); }
    private ConflictContext context(int start, int end, String power, List<Reservation> reservations, List<MaintenanceWindow> maintenance, Duration margin) {
        return new ConflictContext(station, a, car, hour(start), hour(end), kw(power), reservations, maintenance, margin);
    }
    private ConflictContext context(String power, List<Reservation> reservations) { return context(10, 12, power, reservations, List.of(), Duration.ZERO); }
    @ParameterizedTest(name = "Chevauchement {0}-{1}") @CsvSource({"10,12", "10,11", "9,13", "9,11", "11,13"})
    void detectsEveryOverlapShape(int start, int end) {
        assertThat(new OverlapPolicy().check(context(start, end, "20", List.of(reservation(a,10,12,"20")), List.of(), Duration.ZERO))).extracting(Conflict::code).containsExactly("OVERLAP");
    }
    @ParameterizedTest @CsvSource({"8,10", "12,14"}) void adjacentIntervalsAreAccepted(int start, int end) {
        assertThat(new OverlapPolicy().check(context(start,end,"20",List.of(reservation(a,10,12,"20")),List.of(),Duration.ZERO))).isEmpty();
    }
    @Test void anotherConnectorDoesNotCauseTimeConflict() { assertThat(new OverlapPolicy().check(context("20",List.of(reservation(b,10,12,"30"))))).isEmpty(); }
    @Test void cancelledReservationDoesNotBlock() {
        Reservation existing = reservation(a,10,12,"50"); existing.cancel();
        assertThat(new OverlapPolicy().check(context("50",List.of(existing)))).isEmpty();
        assertThat(new PowerCapacityPolicy().check(context("50",List.of(existing)))).isEmpty();
    }
    @Test void marginBlocksAdjacentSlot() {
        assertThat(new OverlapPolicy().check(context(12,13,"20",List.of(reservation(a,10,12,"20")),List.of(),Duration.ofMinutes(10)))).isNotEmpty();
    }
    @Test void incompatibleConnectorIsRefused() {
        a.update("A",ConnectorType.TYPE2,kw("60"),true);
        assertThat(new CompatibilityPolicy().check(context("20",List.of()))).extracting(Conflict::code).contains("INCOMPATIBLE");
    }
    @Test void vehiclePowerLimitIsEnforced() {
        assertThat(new CompatibilityPolicy().check(context("55",List.of()))).extracting(Conflict::code).containsExactly("VEHICLE_POWER_LIMIT");
    }
    @Test void connectorPowerLimitIsEnforced() {
        a.update("A",ConnectorType.CCS,kw("22"),true);
        assertThat(new CompatibilityPolicy().check(context("30",List.of()))).extracting(Conflict::code).containsExactly("CONNECTOR_POWER_LIMIT");
    }
    @Test void inactiveStationIsRefused() {
        station.update("Test",kw("80"),false);
        assertThat(new CompatibilityPolicy().check(context("20",List.of()))).extracting(Conflict::code).contains("INACTIVE");
    }
    @Test void inactiveConnectorIsRefused() {
        a.update("A",ConnectorType.CCS,kw("60"),false);
        assertThat(new CompatibilityPolicy().check(context("20",List.of()))).extracting(Conflict::code).contains("INACTIVE");
    }
    @Test void validCompatibilityIsAccepted() { assertThat(new CompatibilityPolicy().check(context("50",List.of()))).isEmpty(); }
    @Test void stationMaintenanceBlocksConnector() {
        MaintenanceWindow m = new MaintenanceWindow(station,null,hour(9),hour(11),"Travaux");
        assertThat(new MaintenancePolicy().check(context(10,12,"20",List.of(),List.of(m),Duration.ZERO))).extracting(Conflict::code).containsExactly("MAINTENANCE");
    }
    @Test void connectorMaintenanceBlocksConnector() {
        MaintenanceWindow m = new MaintenanceWindow(station,a,hour(10),hour(12),"Travaux");
        assertThat(new MaintenancePolicy().check(context(10,12,"20",List.of(),List.of(m),Duration.ZERO))).isNotEmpty();
    }
    @Test void anotherConnectorMaintenanceDoesNotBlock() {
        MaintenanceWindow m = new MaintenanceWindow(station,b,hour(10),hour(12),"Travaux");
        assertThat(new MaintenancePolicy().check(context(10,12,"20",List.of(),List.of(m),Duration.ZERO))).isEmpty();
    }
    @Test void adjacentMaintenanceDoesNotBlock() {
        MaintenanceWindow m = new MaintenanceWindow(station,null,hour(9),hour(10),"Travaux");
        assertThat(new MaintenancePolicy().check(context(10,12,"20",List.of(),List.of(m),Duration.ZERO))).isEmpty();
    }
    @Test void capacityExceededProposesRemainingPower() {
        List<Conflict> result = new PowerCapacityPolicy().check(context("40",List.of(reservation(b,10,12,"50"))));
        assertThat(result).hasSize(1); assertThat(result.getFirst().code()).isEqualTo("POWER_ADJUSTMENT_REQUIRED");
        assertThat(result.getFirst().availablePowerKw()).isEqualByComparingTo("30");
    }
    @Test void capacityAtExactLimitIsAccepted() {
        assertThat(new PowerCapacityPolicy().check(context("30",List.of(reservation(b,10,12,"50"))))).isEmpty();
    }
    @Test void consecutiveLoadsAreNotAddedTogether() {
        List<Reservation> existing = List.of(reservation(b,10,11,"50"),reservation(b,11,12,"50"));
        assertThat(PowerCapacityPolicy.peak(existing,hour(10),hour(12))).isEqualByComparingTo("50");
        assertThat(new PowerCapacityPolicy().check(context("30",existing))).isEmpty();
    }
    @Test void simultaneousLoadsAreAdded() {
        assertThat(PowerCapacityPolicy.peak(List.of(reservation(a,9,11,"20"),reservation(b,10,12,"30")),hour(10),hour(12))).isEqualByComparingTo("50");
    }
    @Test void outsideIntervalDoesNotConsumePower() {
        assertThat(new PowerCapacityPolicy().available(context("50",List.of(reservation(b,8,10,"60"),reservation(b,12,14,"60"))))).isEqualByComparingTo("80");
    }
    @Test void conflictEngineReturnsAllRelevantReasons() {
        ConflictEngine engine = new ConflictEngine(List.of(new OverlapPolicy(),new CompatibilityPolicy(),new MaintenancePolicy(),new PowerCapacityPolicy()));
        ConflictContext context = context("40",List.of(reservation(a,10,12,"50")));
        assertThat(engine.detect(context)).extracting(Conflict::code).containsExactly("OVERLAP","POWER_ADJUSTMENT_REQUIRED");
        assertThatThrownBy(() -> engine.requireNoConflicts(context)).isInstanceOfSatisfying(ApiException.class, ex -> assertThat(ex.getStatus().value()).isEqualTo(409));
    }
}
