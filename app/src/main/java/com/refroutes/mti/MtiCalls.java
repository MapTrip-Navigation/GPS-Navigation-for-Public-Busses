package com.refroutes.mti;

import android.content.Context;

import androidx.annotation.WorkerThread;

import com.refroutes.main.RefRouteManager;
import com.refroutes.log.Logger;
import de.infoware.android.mti.Api;
import de.infoware.android.mti.ApiListener;
import de.infoware.android.mti.Navigation;
import de.infoware.android.mti.NavigationListener;
import de.infoware.android.mti.enums.ApiError;
import de.infoware.android.mti.enums.Info;
import de.infoware.android.mti.extension.MTIHelper;

@WorkerThread
public class MtiCalls implements ApiListener, NavigationListener {
    public static int CALLBACK_ON_ERROR = 1;
    public static int CALLBACK_INIT = 2;
    public static int CALLBACK_UNINIT = 3;
    public static int CALLBACK_SHOW_APP = 4;
    public static int CALLBACK_DESTINATION_REACHED = 5;
    public static int CALLBACK_STOP_NAVIGATION = 6;
    public static int CALLBACK_STATUS_INFO = 7;
    public static int CALLBACK_INFO = 8;
    public static int CALLBACK_CUSTOM_FUNCTION = 10;

    private static boolean isMtiInitialized = false;
    private Logger logger = Logger.createLogger("MtiCalls");

    // for demo purposes only
    private static boolean demoMode = true;
    private RefRouteManager refRouteManager;
    private int processedRequestId = -1;

    public MtiCalls (RefRouteManager refRouteManager) {
        this.refRouteManager = refRouteManager;
    }

    // ======================================================================================================
    //      MTI provided methods
    //      Most of the next methods use a semaphore to wait until MTI calls the callback methods
    // ======================================================================================================

    /**
     * Initializes the MTI interface.
     * Waits until MTI is available. Means that this can block the whole App.
     * @return The ApiError as received by MTI
     */
    public ApiError initMti(Context context) {
        if (!isMtiInitialized) {
            MTIHelper.initialize(context);
            isMtiInitialized = true;
        }
        Api.registerListener(this);
        Navigation.registerListener(this);
        Api.init();
        ApiError result = MtiCallbackSynchronizer.wait(CALLBACK_INIT, "Api.init", 1000L);
        Api.customFunction("packageName", "com.refroutes");
        Api.customFunction("className", "com.refroutes.main.MainActivity");

        return result;
    }

    /**
     * Initializes the MTI interface.
     * Waits until MTI is available. Means that this can block the whole App.
     * @return ApiError.OK
     */
    public ApiError initMtiDemo(Context context) throws InterruptedException {
        MTIHelper.initialize(context);
        Api.registerListener(this);
        Navigation.registerListener(this);
        Api.init();
        while (!isMtiInitialized) {
            Thread.sleep(1000);
            System.out.println("waiting");
        }
        return ApiError.OK;
    }


    public ApiError findServer() {
        logger.finest("findServer", "findServer()");
        int findServerId = Api.findServer();
        return MtiCallbackSynchronizer.wait(findServerId, "Api.findServer", 500L);
    }

    public ApiError startReferenceRoute(String refRouteFileName, boolean restartTour, Long waitTime) {
        if (demoMode) {
            try {
                return startReferenceRouteDemo(refRouteFileName);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        int startReferenceRouteId = Navigation.startReferenceRoute(refRouteFileName, restartTour);
        return MtiCallbackSynchronizer.wait(startReferenceRouteId, "Navigation.startReferenceRoute", waitTime);
    }

    /**
     * Demonstrates the usage of the MapTrip MTI callback mechanism.
     */
    public ApiError startReferenceRouteDemo(String refRouteFileName) throws InterruptedException {
        // The callBackId can be used later as an association between the function call and the callback call
        int callBackId = Navigation.startReferenceRoute(refRouteFileName, true);

        // From now the app has to wait until MapTrip started the ReferenceRotue
        // For demonstration purposes the loop is
        int loops = 0;
        while (processedRequestId != callBackId) {
            loops++;
            logger.finest("startReferenceRouteDemo", "waiting for callback with ID " + callBackId + " - loops: " + loops);
        }
        return ApiError.OK;
    }

    public ApiError getCurrentDestination(Long waitTime) {
        int getCurrentDestinationId = Navigation.getCurrentDestination();
        return MtiCallbackSynchronizer.wait(getCurrentDestinationId, "Navigation.getCurrentDestination", waitTime);
    }

    public ApiError stopNavigation(Long waitTime){
        int stopNavigationId =  Navigation.stopNavigation();
        if (null != waitTime) {
            return MtiCallbackSynchronizer.wait(stopNavigationId, "Navigation.stopNavigation", waitTime);
        }
        return ApiError.OK;
    }

    public ApiError removeAllDestinationCoordinates(Long waitTime) {
        int removeAllDestinationCoordinatesId = Navigation.removeAllDestinationCoordinates();
        if (null != waitTime) {
            return MtiCallbackSynchronizer.wait(removeAllDestinationCoordinatesId, "Navigation.removeAllDestinationCoordinates", waitTime);
        }
        return ApiError.OK;
    }

    public ApiError waitForDestinationReached() {
        return MtiCallbackSynchronizer.wait(CALLBACK_DESTINATION_REACHED, "Const: CALLBACK_DESTINATION_REACHED", null);
    }

    public void interruptRoutingByUser() {
        MtiCallbackSynchronizer.setInterruptedByUser(CALLBACK_DESTINATION_REACHED);
        stopNavigation(null);
        removeAllDestinationCoordinates(null);
    }

    private void uninitMti() {
        Api.uninit();
        MtiCallbackSynchronizer.wait(CALLBACK_UNINIT, "Api.uninit", 5000L);
    }

    public ApiError hideServer (Long waitTime) {
        int hideServerRequestId = Api.hideServer();
        return MtiCallbackSynchronizer.wait(hideServerRequestId, "Api.hideServer", waitTime);
    }

    public ApiError showApp (String packageName, String className, Long waitTime) {
        int hideServerRequestId = Api.showApp(packageName, className);
        return MtiCallbackSynchronizer.wait(hideServerRequestId, "Api.showApp", null);
    }

    public void showMessageButton () {
        Api.customFunction("buttonMessage", "true");
    }

    // ======================================================================================================
    // Implementations of MTI Interface
    // ======================================================================================================

    @Override
    public void onError(int i, String s, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(CALLBACK_ON_ERROR, s, apiError, "onError");
    }

    @Override
    public void findServerResult(int requestId, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "findServerResult");
    }

    @Override
    public void initResult(int i, ApiError apiError) {
        isMtiInitialized = true;
        MtiCallbackSynchronizer.callBackCalled(CALLBACK_INIT, null, apiError, "initResult");
    }

    @Override
    public void showAppResult(int i, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(CALLBACK_SHOW_APP, null, apiError, "showAppResult");
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
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "showServerResult");
    }

    @Override
    public void hideServerResult(int requestId, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "hideServerResult");
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
        MtiCallbackSynchronizer.callBackCalled(CALLBACK_CUSTOM_FUNCTION, "i = " + i + "s = " + s + "s1 = " + s1, null, "customFunctionResult");
    }

    @Override
    public void infoMsg(Info info, int i) {
        logger.finest("infoMsg", "Callback: infoFromSDK: " + info);
        refRouteManager.setMessageButtonClicked(true);
    }

    @Override
    public void statusInfo(double v, double v1, double v2, double v3) {
        MtiCallbackSynchronizer.callBackCalled(CALLBACK_STATUS_INFO, "v = " + v + "; v1 = " + v1 + "; v2 = " + v2 + "; v3 = " + v3, ApiError.OK, "statusInfo");
    }

    @Override
    public void coiInfo(double v, double v1, String s, String s1, double v2) {
        logger.finest("coiInfo", "Callback: infoFromSDK: Position" + v + ";" + v1 + "; " + s + ";" + s1 + "; " + v2);
    }

    @Override
    public void crossingInfo(double v, double v1, String s, String s1, String s2, double v2) {
        logger.finest("crossingInfo", "Callback: infoFromSDK: Position" + v + ";" + v1 + "; " + s + ";" + s1 + "; " + v2);
    }

    @Override
    public void destinationReached(int routeId) {
        MtiCallbackSynchronizer.callBackCalled(CALLBACK_DESTINATION_REACHED, null, ApiError.OK, "destinationReached");
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
        logger.finest("getDestinationCoordinateResult", "Callback: getDestinationCoordinateResult: " + v + ";" + v1 + "; apiError = " + apiError);
    }

    @Override
    public void getDestinationCoordinateCountResult(int i, ApiError apiError, int i1) {

    }

    @Override
    public void getCurrentDestinationResult(int requestId, ApiError apiError, int i1) {
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "getCurrentDestinationResult");
    }

    @Override
    public void removeAllDestinationCoordinatesResult(int requestId, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "removeAllDestinationCoordinatesResult");
    }

    @Override
    public void startNavigationResult(int i, ApiError apiError) {
        logger.finest("startNavigationResult", "Callback: startNavigationResult: apiError = " + apiError);
    }

    @Override
    public void startAlternativeNavigationResult(int i, ApiError apiError) {

    }

    @Override
    public void startSimulationResult(int i, ApiError apiError) {

    }

    @Override
    public void stopNavigationResult(int requestId, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "stopNavigationResult");
    }

    @Override
    public void startRouteFromFileResult(int requestId, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "startRouteFromFileResult");
    }

    @Override
    public void startReferenceRouteResult(int requestId, ApiError apiError) {
        processedRequestId = requestId;
        logger.finest("startReferenceRouteResult", "requestId " + requestId);
        MtiCallbackSynchronizer.callBackCalled(requestId, null, apiError, "startReferenceRouteResult");
    }

    @Override
    public void getReferenceRouteFileResult(int requestId, String s, ApiError apiError) {
        MtiCallbackSynchronizer.callBackCalled(requestId, s, apiError, "getReferenceRouteFileResult");
    }

    @Override
    public void syncWithActiveNavigationResult(int i, ApiError apiError) {

    }

    @Override
    public void navigateWithGuiGeocodingResult(int i, ApiError apiError) {

    }

    @Override
    public void routingStarted() {
        logger.finest("routingStarted", "Callback: routingStarted");
    }

    @Override
    public void routeCalculated() {
        logger.finest("routeCalculated", "Callback: routeCalculated");
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
