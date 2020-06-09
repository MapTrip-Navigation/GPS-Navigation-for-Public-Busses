package com.refroutes.mti;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.refroutes.model.RefRoute;
import com.refroutes.log.Logger;
import com.refroutes.MainActivity;

import java.util.ArrayList;

import de.infoware.android.mti.Api;
import de.infoware.android.mti.ApiListener;
import de.infoware.android.mti.Navigation;
import de.infoware.android.mti.NavigationListener;
import de.infoware.android.mti.enums.ApiError;
import de.infoware.android.mti.enums.Info;
import de.infoware.android.mti.extension.MTIHelper;

/**
 * Works at reference routes
 */

@WorkerThread
public class MtiCalls implements ApiListener, NavigationListener {
    private int lastRefRouteId = -1;
    private int activeRefRouteId = 0;
    private ArrayList<RefRoute> refRoutes;
    private boolean isWorking = false;
    private static Context context;
    private static boolean isMtiInitialized = false;
    private boolean messageButtonClicked = false;

    private Logger logger = Logger.createLogger("RefRouteWorkerService");

    public void reset() {
        lastRefRouteId = -1;
        activeRefRouteId = 0;
        isWorking = false;
    }

    public MtiCalls(MainActivity mainActivity, ArrayList<RefRoute> refRoutes) {
        this.context = mainActivity;
        this.refRoutes = refRoutes;
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

    // ======================================================================================================
    // React to user actions
    // ======================================================================================================

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

    // ======================================================================================================
    // MTI Provided Methods
    // ======================================================================================================

    private static int CALLBACK_ON_ERROR = 1;
    private static int CALLBACK_INIT = 2;
    private static int CALLBACK_UNINIT = 3;
    private static int CALLBACK_SHOW_APP = 4;
    private static int CALLBACK_DESTINATION_REACHED = 5;
    private static int CALLBACK_STOP_NAVIGATION = 6;
    private static int CALLBACK_STATUS_INFO = 7;
    private static int CALLBACK_INFO = 8;
    private static int CALLBACK_CUSTOM_FUNCTION = 10;

    private int startReferenceId = -1;

    public ApiError findServer() {
        logger.finest("findServer", "findServer()");
        int findServerId = Api.findServer();
        MtiCallbackSynchronizer.Semaphore semaphore = MtiCallbackSynchronizer.getSemaphoreForCallBack(findServerId, "Api.findServer");
        return semaphore.waitForCallback(null);

    }

    /**
     * Initializes the MTI interface.
     * Waits until MTI is available. Means that this can block the whole App.
     * @return The ApiError as received by MTI
     */
    public ApiError initMti() {
        if (!isMtiInitialized) {
            MTIHelper.initialize(context);
            isMtiInitialized = true;
        }
        Api.init();
        Api.registerListener(this);
        Navigation.registerListener(this);
        MtiCallbackSynchronizer.Semaphore semaphore = MtiCallbackSynchronizer.getSemaphoreForCallBack(CALLBACK_INIT, "Api.init");

//        isMtiInitialized = true;
        ApiError result =  semaphore.waitForCallback(null);
        Api.customFunction("packageName", "com.refroutes");
        Api.customFunction("className", "com.refroutes.MainActivity");

        return result;
    }

    public void showMessageButton () {
        Api.customFunction("buttonMessage", "true");
        messageButtonClicked = false;
    }

    private void uninitMti() {
        Api.uninit();
        MtiCallbackSynchronizer.Semaphore semaphore = MtiCallbackSynchronizer.getSemaphoreForCallBack(CALLBACK_UNINIT, "Api.uninit");
        semaphore.waitForCallback(new Long(5000));
    }

    public ApiError hideServer (String packageName, String className) {
        int hideServerRequestId = Api.showApp(packageName, className);
        MtiCallbackSynchronizer.Semaphore semaphore = MtiCallbackSynchronizer.getSemaphoreForCallBack(hideServerRequestId, "Api.hideServer");
        return semaphore.waitForCallback();
    }

    public void interruptRouting() {
        MtiCallbackSynchronizer.setInterruptedByUser(CALLBACK_DESTINATION_REACHED);
        Navigation.stopNavigation();
        Navigation.removeAllDestinationCoordinates();
    }

    /**
     * Start routing
     */
    @WorkerThread
    public ApiError routeItem(String routesPath, Integer routeId, boolean restartTour) {
        int currentDestRequestId = Navigation.getCurrentDestination();
        MtiCallbackSynchronizer.Semaphore semaphore = MtiCallbackSynchronizer.getSemaphoreForCallBack(currentDestRequestId, "Navigation.getCurrentDestination");
        if (ApiError.NO_DESTINATION != semaphore.waitForCallback(new Long(10000))) {
            Navigation.stopNavigation();
            MtiCallbackSynchronizer.getSemaphoreForCallBack(CALLBACK_STOP_NAVIGATION, "Const: CALLBACK_STOP_NAVIGATION").waitForCallback(new Long(1000));
            int removeRequestId = Navigation.removeAllDestinationCoordinates();
            MtiCallbackSynchronizer.getSemaphoreForCallBack(removeRequestId, "Navigation.removeAllDestinationCoordinates").waitForCallback(new Long(10000));
        }

        String refRouteFileName = routesPath + "/" + refRoutes.get(routeId).getRefRouteFileName();
        startReferenceId = Navigation.startReferenceRoute(refRouteFileName, !restartTour);
        logger.finest("routeItem", "refRouteFileName = " + refRouteFileName + "; routeId = " + startReferenceId);

        ApiError waitForStartResult =  MtiCallbackSynchronizer.getSemaphoreForCallBack(startReferenceId, "Navigation.startReferenceRoute").waitForCallback(null);
        if (waitForStartResult == ApiError.OK) {
            ApiError waitForCallBack = MtiCallbackSynchronizer.getSemaphoreForCallBack(CALLBACK_DESTINATION_REACHED, "Const: CALLBACK_DESTINATION_REACHED").waitForCallback(null);
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
        Api.hideServer();
        return waitForStartResult;
    }

    private void callBackCalled(int callBackOrRoute, String info, ApiError apiError, String logInfo) {
        // apiError sometimes is null - then the result is ok
        MtiCallbackSynchronizer.callBackCalled(callBackOrRoute, info, apiError != null ? apiError : ApiError.OK, logInfo);
    }


    // ======================================================================================================
    // Implementations of MTI Interface
    // ======================================================================================================

    @Override
    public void onError(int i, String s, ApiError apiError) {
        callBackCalled(CALLBACK_ON_ERROR, s, apiError, "onError");
    }

    @Override
    public void findServerResult(int requestId, ApiError apiError) {
        callBackCalled(requestId, null, apiError, "findServerResult");
    }

    @Override
    public void initResult(int i, ApiError apiError) {
        callBackCalled(CALLBACK_INIT, null, apiError, "initResult");
    }

    @Override
    public void showAppResult(int i, ApiError apiError) {
        callBackCalled(CALLBACK_SHOW_APP, null, apiError, "showAppResult");
    }

    @Override
    public void sendTextResult(int i, ApiError apiError) {

    }

    @Override
    public void getMapVersionResult(int i, String s, ApiError apiError) {

    }

    @Override
    public void getMaptripVersionResult(int i, String s, ApiError apiError) {

    }

    @Override
    public void getApiVersionResult(int i, String s, ApiError apiError) {
    }

    @Override
    public void showServerResult(int requestId, ApiError apiError) {
        callBackCalled(requestId, null, apiError, "showServerResult");
    }

    @Override
    public void hideServerResult(int requestId, ApiError apiError) {
        callBackCalled(requestId, null, apiError, "hideServerResult");
    }

    @Override
    public void stopServerResult(int i, ApiError apiError) {

    }

    @Override
    public void enableNetworkConnectionsResult(int i, ApiError apiError) {

    }

    @Override
    public void setDataUsageMonthlyLimitResult(int i, ApiError apiError) {

    }

    @Override
    public void resetDataUsageMonthlyLimitResult(int i, ApiError apiError) {

    }

    @Override
    public void getDataUsageMonthlyLimitResult(int i, int i1, ApiError apiError) {

    }

    @Override
    public void getDataUsageRemainingQuotaResult(int i, int i1, ApiError apiError) {

    }

    @Override
    public void isNetworkConnectionEnabledResult(int i, boolean b, ApiError apiError) {

    }

    @Override
    public void customFunctionResult(int i, String s, String s1, ApiError apiError) {
        callBackCalled(CALLBACK_CUSTOM_FUNCTION, "i = " + i + "s = " + s + "s1 = " + s1, null, "infoMsg");
    }

    @Override
    public void infoMsg(Info info, int i) {
        messageButtonClicked = true;
//        callBackCalled(CALLBACK_INFO, "info: " + info + "; i = " + i, null, "infoMsg");
    }

    @Override
    public void statusInfo(double v, double v1, double v2, double v3) {
        callBackCalled(CALLBACK_STATUS_INFO, "v = " + v + "; v1 = " + v1 + "; v2 = " + v2 + "; v3 = " + v3, ApiError.OK, "statusInfo");
    }

    @Override
    public void coiInfo(double v, double v1, String s, String s1, double v2) {

    }

    @Override
    public void crossingInfo(double v, double v1, String s, String s1, String s2, double v2) {

    }

    @Override
    public void destinationReached(int routeId) {
        callBackCalled(CALLBACK_DESTINATION_REACHED, null, ApiError.OK, "destinationReached");
    }

    @Override
    public void insertDestinationCoordinateResult(int i, ApiError apiError) {

    }

    @Override
    public void appendDestinationCoordinateResult(int i, int i1, ApiError apiError) {

    }

    @Override
    public void insertDestinationAddressResult(int i, ApiError apiError) {

    }

    @Override
    public void appendDestinationAddressResult(int i, int i1, ApiError apiError) {

    }

    @Override
    public void insertGeocodedDestinationResult(int i, ApiError apiError) {

    }

    @Override
    public void appendGeocodedDestinationResult(int i, int i1, ApiError apiError) {

    }

    @Override
    public void markDestinationCoordinateAsViaPointResult(int i, ApiError apiError) {

    }

    @Override
    public void getDestinationCoordinateResult(int i, ApiError apiError, double v, double v1) {

    }

    @Override
    public void getDestinationCoordinateCountResult(int i, ApiError apiError, int i1) {

    }

    @Override
    public void getCurrentDestinationResult(int requestId, ApiError apiError, int i1) {
        callBackCalled(requestId, null, apiError, "getCurrentDestinationResult");
    }

    @Override
    public void removeAllDestinationCoordinatesResult(int requestId, ApiError apiError) {
        callBackCalled(requestId, null, apiError, "removeAllDestinationCoordinatesResult");
    }

    @Override
    public void startNavigationResult(int i, ApiError apiError) {

    }

    @Override
    public void startAlternativeNavigationResult(int i, ApiError apiError) {

    }

    @Override
    public void startSimulationResult(int i, ApiError apiError) {

    }

    @Override
    public void stopNavigationResult(int requestId, ApiError apiError) {
        callBackCalled(requestId, null, apiError, "stopNavigationResult");
    }

    @Override
    public void startRouteFromFileResult(int requestId, ApiError apiError) {
        callBackCalled(requestId, null, apiError, "startRouteFromFileResult");
    }

    @Override
    public void startReferenceRouteResult(int requestId, ApiError apiError) {
        callBackCalled(requestId, null, apiError, "startReferenceRouteResult");
    }

    @Override
    public void getReferenceRouteFileResult(int requestId, String s, ApiError apiError) {
        callBackCalled(requestId, s, apiError, "getReferenceRouteFileResult");
    }

    @Override
    public void syncWithActiveNavigationResult(int i, ApiError apiError) {

    }

    @Override
    public void navigateWithGuiGeocodingResult(int i, ApiError apiError) {

    }

    @Override
    public void routingStarted() {

    }

    @Override
    public void routeCalculated() {

    }

    @Override
    public void setRoutingModeHybridResult(int i, ApiError apiError) {

    }

    @Override
    public void newRouteAvailable() {

    }

    @Override
    public void setEmergencyRoutingEnabledResult(int i, ApiError apiError) {

    }

    @Override
    public void getEmergencyRoutingEnabledResult(int i, ApiError apiError, boolean b, boolean b1, int i1) {

    }

    @Override
    public void setEmergencyRouteRadiusResult(int i, ApiError apiError) {

    }

    @Override
    public void getEmergencyRouteRadiusResult(int i, ApiError apiError, int i1) {

    }
}
