package iut_bm_lp.projet_mountaincompanion_v1.models;

/**
 * Created by jalvara2 on 29/11/17.
 */

public class Mountain {
    private int id;
    private float latitude;
    private float longitude;
    private String nom;
    private double altitude;
    private String wiki;

    private double direction;
    private double distance;
    private double visualElevation; // Vertical angle looking at peak

    public Mountain(){

    }

    public Mountain(int id, float latitude, float longitude, String nom, double altitude,String wiki) {

        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.nom = nom;
        this.altitude = altitude;
        this.wiki = wiki;
    }

    @Override
    public String toString() {
        return "Mountain{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", nom='" + nom + '\'' +
                ", altitude=" + altitude +
                ", wiki='" + wiki + '\'' +
                '}';
    }

    /*****************          GETTERS-SETTERS         *********************/

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public double getAltitude() {
        return altitude;
    }

    public void setAltitude(int altitude) {
        this.altitude = altitude;
    }

    public double getDirection() {
        return direction;
    }

    public void setDirection(double direction) {
        this.direction = direction;
    }

    public double getDistance() {
        return distance;
    }

    public void setDistance(double distance) {
        this.distance = distance;
    }

    public double getVisualElevation() {
        return visualElevation;
    }

    public void setVisualElevation(double visualElevation) {
        this.visualElevation = visualElevation;
    }

    public String getWiki() {
        return wiki;
    }

    public void setWiki(String wiki) {
        this.wiki = wiki;
    }
}
