package fr.itix.enricher;

public class LocationEntity {

    public String parcelNumber;
    public String location;
    public String direction;
    public long timestamp;

    public LocationEntity() {
    }

    public LocationEntity(String parcelNumber, String location, String direction, long timestamp) {
        this.parcelNumber = parcelNumber;
        this.location = location;
        this.direction = direction;
        this.timestamp = timestamp;
    }
}
