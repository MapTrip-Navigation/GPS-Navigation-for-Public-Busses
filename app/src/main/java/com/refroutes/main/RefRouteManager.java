package com.refroutes.main;

import android.content.Context;
import androidx.annotation.WorkerThread;

import com.refroutes.log.Logger;
import com.refroutes.model.RefRoute;
import com.refroutes.mti.MtiCalls;

import java.util.ArrayList;
import de.infoware.android.mti.enums.ApiError;

@WorkerThread
public class RefRouteManager {
    private int lastRefRouteId = -1;
    private int activeRefRouteId = 0;
    private boolean isWorking = false;
    private boolean messageButtonClicked = false;
    private ArrayList<RefRoute> refRoutes;
    private int startReferenceId = -1;
    private MtiCalls mtiCalls;

    private Logger logger = Logger.createLogger("RefRouteManager");


    public RefRouteManager(ArrayList<RefRoute> refRoutes) {
        this.mtiCalls = new MtiCalls(this);
        this.refRoutes = refRoutes;
    }

    public void setMessageButtonClicked(boolean messageButtonClicked) {
        this.messageButtonClicked = messageButtonClicked;
    }

    public void reset() {
        lastRefRouteId = -1;
        activeRefRouteId = 0;
        isWorking = false;
    }

    public boolean isWorking() {
        return isWorking;
    }

    public boolean isMessageButtonClicked() {
        return messageButtonClicked;
    }

    public void resetMessageButtonClick() {
        messageButtonClicked = false;
    }

    public void resetWorkingSwitch() {
        isWorking = true;
    }

    public int getLastRefRouteId() {
        return lastRefRouteId;
    }

    @WorkerThread
    public boolean nextRefRouteExists() {
        while (activeRefRouteId < refRoutes.size()) {
            if (refRoutes.get(activeRefRouteId).isActive()) {
                return true;
            }
            ++activeRefRouteId;
        }
        return false;
    }

    @WorkerThread
    public int getNextRefRouteId() {
        while (activeRefRouteId < refRoutes.size()) {
            if (refRoutes.get(activeRefRouteId).isActive()) {
                break;
            }
            activeRefRouteId++;
        }
        return activeRefRouteId;
    }

    public int getActiveRefRouteId() {
        return activeRefRouteId;
    }

    /**
     * Start routing
     */
    @WorkerThread
    public ApiError routeItem(String packageName, String className, String routesPath, Integer routeId, boolean restartTour) {
        if (ApiError.NO_DESTINATION != mtiCalls.getCurrentDestination(new Long(10000))) {
            mtiCalls.stopNavigation(new Long(1000));
            mtiCalls.removeAllDestinationCoordinates(new Long(10000));
        }

        ApiError waitForStartResult = mtiCalls.startReferenceRoute(refRoutes.get(routeId).getRefRouteFileName(), !restartTour, null);
        logger.finest("routeItem", "refRouteFileName = " + refRoutes.get(routeId).getRefRouteFileName());
        if (waitForStartResult == ApiError.OK) {
            ApiError waitForCallBack = mtiCalls.waitForDestinationReached();
            switch (waitForCallBack) {
                case OK:
                    refRoutes.get(routeId).setFinished(true);
                    lastRefRouteId = activeRefRouteId;
                    ++activeRefRouteId;
                    return ApiError.OK;

                default:
                    return waitForCallBack;
            }
        }
        logger.warn("routeItem", "waitForStartResult = " + waitForStartResult.name() + "; routeId = " + startReferenceId);
        mtiCalls.showApp(packageName, className, null);
        return waitForStartResult;
    }

    public ApiError initMti(Context context) {
        return mtiCalls.initMti(context);
    }

    public ApiError findServer() {
        return mtiCalls.findServer();
    }

    public void showApp(String packageName, String className) {
        mtiCalls.showApp(packageName, className, null);
    }

    public void showMessageButton () {
        mtiCalls.showMessageButton();
        messageButtonClicked = false;
    }

    public void interruptRouting() {
        mtiCalls.interruptRoutingByUser();
    }
}
