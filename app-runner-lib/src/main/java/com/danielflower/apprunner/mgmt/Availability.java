package com.danielflower.apprunner.mgmt;

public class Availability {

    public final boolean isAvailable;
    public final String availabilityStatus;

    public Availability(boolean isAvailable, String availabilityStatus) {
        this.isAvailable = isAvailable;
        this.availabilityStatus = availabilityStatus;
    }

    public static Availability available() {
        return new Availability(true, "Running");
    }
    public static Availability unavailable(String message) {
        return new Availability(false, message);
    }
}
