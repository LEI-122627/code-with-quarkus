package org.acme;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;

@QuarkusTest
class ParkingResourceTest {

    @Test
    void testParkingStatusEndpoint() {
        given()
          .when().get("/parking")
          .then()
             .statusCode(200)
             .body(startsWith("Parking service online | total=10"));
    }

    @Test
    void testSlotStatusFlow() {
        given()
          .when().get("/parking/slots/1")
          .then()
             .statusCode(200)
             .body(is("Slot 1 is FREE"));

        given()
          .when().post("/parking/entries/1?entrance=2026-03-23T10:00:00")
          .then()
             .statusCode(200)
             .body(is("Entry registered at slot 1 on 2026-03-23T10:00"));

        given()
          .when().get("/parking/slots/1")
          .then()
             .statusCode(200)
             .body(is("Slot 1 is OCCUPIED since 2026-03-23T10:00"));

        given()
          .when().post("/parking/exits/1?exit=2026-03-23T11:30:00")
          .then()
             .statusCode(200)
             .body(is("Exit registered at slot 1 | entrance=2026-03-23T10:00 | exit=2026-03-23T11:30 | durationMinutes=90"));
    }

    @Test
    void testCannotEnterOccupiedSlot() {
        given()
          .when().post("/parking/entries/2?entrance=2026-03-23T12:00:00")
          .then()
             .statusCode(200);

        given()
          .when().post("/parking/entries/2?entrance=2026-03-23T12:30:00")
          .then()
             .statusCode(409)
             .body(is("Slot 2 is already occupied"));

        given()
          .when().post("/parking/exits/2?exit=2026-03-23T13:00:00")
          .then()
             .statusCode(200)
             .body(is("Exit registered at slot 2 | entrance=2026-03-23T12:00 | exit=2026-03-23T13:00 | durationMinutes=60"));
    }

    @Test
    void testCannotExitFreeSlot() {
        given()
          .when().post("/parking/exits/3?exit=2026-03-23T15:00:00")
          .then()
             .statusCode(409)
             .body(is("Slot 3 is already free"));
    }

    @Test
    void testRejectsInvalidSlotAndInvalidTime() {
        given()
          .when().post("/parking/entries/0?entrance=2026-03-23T10:00:00")
          .then()
             .statusCode(400)
             .body(is("Slot must be between 1 and 10"));

        given()
          .when().post("/parking/entries/4?entrance=invalid")
          .then()
             .statusCode(400)
             .body(is("Invalid date-time format. Use ISO_LOCAL_DATE_TIME (yyyy-MM-ddTHH:mm:ss)"));
    }

    @Test
    void testRejectsExitBeforeEntrance() {
        given()
          .when().post("/parking/entries/5?entrance=2026-03-23T11:00:00")
          .then()
             .statusCode(200);

        given()
          .when().post("/parking/exits/5?exit=2026-03-23T10:45:00")
          .then()
             .statusCode(400)
             .body(is("Exit time cannot be before entrance time"));

        given()
          .when().post("/parking/exits/5?exit=2026-03-23T11:15:00")
          .then()
             .statusCode(200)
             .body(is("Exit registered at slot 5 | entrance=2026-03-23T11:00 | exit=2026-03-23T11:15 | durationMinutes=15"));
    }
}

