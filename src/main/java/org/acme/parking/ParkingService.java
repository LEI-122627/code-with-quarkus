package org.acme.parking;

import jakarta.enterprise.context.ApplicationScoped;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@ApplicationScoped
public class ParkingService {

    private static final int TOTAL_SLOTS = 10;
    private final Map<Integer, LocalDateTime> entrancesBySlot = new ConcurrentHashMap<>();

    public int totalSlots() {
        return TOTAL_SLOTS;
    }

    public synchronized int occupiedCount() {
        return entrancesBySlot.size();
    }

    public synchronized Optional<LocalDateTime> entranceFor(int slot) {
        validateSlot(slot);
        return Optional.ofNullable(entrancesBySlot.get(slot));
    }

    public synchronized void enter(int slot, LocalDateTime entranceTime) {
        validateSlot(slot);
        if (entrancesBySlot.containsKey(slot)) {
            throw new IllegalStateException("Slot " + slot + " is already occupied");
        }
        entrancesBySlot.put(slot, entranceTime);
    }

    public synchronized ExitResult exit(int slot, LocalDateTime exitTime) {
        validateSlot(slot);
        LocalDateTime entranceTime = entrancesBySlot.get(slot);
        if (entranceTime == null) {
            throw new IllegalStateException("Slot " + slot + " is already free");
        }
        if (exitTime.isBefore(entranceTime)) {
            throw new IllegalArgumentException("Exit time cannot be before entrance time");
        }

        entrancesBySlot.remove(slot);
        long durationMinutes = Duration.between(entranceTime, exitTime).toMinutes();
        return new ExitResult(entranceTime, exitTime, durationMinutes);
    }

    private void validateSlot(int slot) {
        if (slot < 1 || slot > TOTAL_SLOTS) {
            throw new IllegalArgumentException("Slot must be between 1 and " + TOTAL_SLOTS);
        }
    }

    public record ExitResult(LocalDateTime entranceTime, LocalDateTime exitTime, long durationMinutes) {
    }
}

