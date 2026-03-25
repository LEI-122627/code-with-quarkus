package org.acme;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.acme.parking.ParkingService;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;

@Path("/parking")
@Produces(MediaType.TEXT_PLAIN)
public class ParkingResource {
    private final ParkingService parkingService;

    public ParkingResource(ParkingService parkingService) {
        this.parkingService = parkingService;
    }

    @GET
    public String status() {
        int total = parkingService.totalSlots();
        int occupied = parkingService.occupiedCount();
        int available = total - occupied;
        return "Parking service online | total=" + total + " occupied=" + occupied + " available=" + available;
    }

    @GET
    @Path("/slots/{slot}")
    public Response slotStatus(@PathParam("slot") int slot) {
        try {
            LocalDateTime entranceTime = parkingService.entranceFor(slot).orElse(null);
            if (entranceTime == null) {
                return Response.ok("Slot " + slot + " is FREE").build();
            }
            return Response.ok("Slot " + slot + " is OCCUPIED since " + entranceTime).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/entries/{slot}")
    public Response registerEntry(@PathParam("slot") int slot, @QueryParam("entrance") String entranceRaw) {
        try {
            LocalDateTime entranceTime = parseOrNow(entranceRaw);
            parkingService.enter(slot, entranceTime);
            return Response.ok("Entry registered at slot " + slot + " on " + entranceTime).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        } catch (IllegalStateException ex) {
            return Response.status(Response.Status.CONFLICT).entity(ex.getMessage()).build();
        }
    }

    @POST
    @Path("/exits/{slot}")
    public Response registerExit(@PathParam("slot") int slot, @QueryParam("exit") String exitRaw) {
        try {
            LocalDateTime exitTime = parseOrNow(exitRaw);
            ParkingService.ExitResult result = parkingService.exit(slot, exitTime);
            return Response.ok(
                    "Exit registered at slot " + slot +
                            " | entrance=" + result.entranceTime() +
                            " | exit=" + result.exitTime() +
                            " | durationMinutes=" + result.durationMinutes()
            ).build();
        } catch (IllegalArgumentException ex) {
            return Response.status(Response.Status.BAD_REQUEST).entity(ex.getMessage()).build();
        } catch (IllegalStateException ex) {
            return Response.status(Response.Status.CONFLICT).entity(ex.getMessage()).build();
        }
    }

    private LocalDateTime parseOrNow(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(rawValue);
        } catch (DateTimeParseException ex) {
            throw new IllegalArgumentException("Invalid date-time format. Use ISO_LOCAL_DATE_TIME (yyyy-MM-ddTHH:mm:ss)");
        }
    }
}
