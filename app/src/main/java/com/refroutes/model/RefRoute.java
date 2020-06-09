package com.refroutes.model;

public class RefRoute {
    private  String refRouteName;
    private  String refRouteDescription;
    private  int refRouteIndex;
    private  boolean isActive;
    private boolean isFinished;
    private  String refRouteFileName;

    public RefRoute(String refRouteName, String refRouteDescription, String refRouteFileName, int refRouteIndex, boolean isActive) {
        this.refRouteName = refRouteName;
        this.refRouteDescription = refRouteDescription;
        this.refRouteIndex = refRouteIndex;
        this.isActive = isActive;
        this.refRouteFileName = refRouteFileName;
    }

    public boolean isFinished() {
        return isFinished;
    }

    public void setFinished (boolean finished) {
        this.isFinished = finished;
    }

    public String getRefRouteName() {
        return refRouteName;
    }

    public void setRefRouteName (String refRouteName) {
        this.refRouteName = refRouteName;
    }

    public String getRefRouteDescription () {
        return refRouteDescription;
    }

    public void setRefRouteDescription (String refRouteDescription) {
        this.refRouteDescription = refRouteDescription;
    }

    public int getRefRouteIndex () {
        return refRouteIndex;
    }

    public void setRefRouteIndex (int refRouteIndex) {
        this.refRouteIndex = refRouteIndex;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive (boolean isActive) {
        this.isActive = isActive;
    }

    public void setRefRouteFileName(String refRouteFileName) {
        this.refRouteFileName = refRouteFileName;
    }

    public String getRefRouteFileName() {
        return this.refRouteFileName;
    }
}