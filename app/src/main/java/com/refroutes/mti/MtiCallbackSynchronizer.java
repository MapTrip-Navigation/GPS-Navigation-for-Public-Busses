package com.refroutes.mti;

import androidx.annotation.WorkerThread;

import com.refroutes.log.Logger;

import java.util.HashMap;

import de.infoware.android.mti.enums.ApiError;

@WorkerThread
public class MtiCallbackSynchronizer {
    private static HashMap<Integer, Semaphore> map = new HashMap<>();
    private static MtiCallbackSynchronizer manager = new MtiCallbackSynchronizer();
    private static Logger logger = Logger.createLogger("WaitForCallbackManager");

    /**
     * This method should be called by the callback methods which are called themselve by MTI
     * to 'release' the waiting of other methods to the callback.
     *
     * @param callBackOrRoute Most MTI methods deliver an ID as a result which can be used later to
     *                        identify the relation between the initial call and the later incoming callback.
     *                        If the result of a MTI method isn't such a ID you can use an own ID
     * @param infoFromSDK     Any information of the MTI which is used in the callback call
     * @param apiError        Result of MTI
     * @param callBack        Name of the callback (used for information only - e.g. logging)
     */
    public static void callBackCalled(final int callBackOrRoute, final String infoFromSDK, final ApiError apiError, final String callBack) {
        logger.finest("callbackCalled", "Callback: " + callBack + "; infoFromSDK: " + infoFromSDK + "; ApiError = " + apiError);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("SemaphoreCreateWaiter");

                Semaphore sem = null;
                int i = 0;
                while (sem == null) {
                    sem = manager.retrieveSemaphore(callBackOrRoute);
                    try {
                        Thread.sleep(200);
                        if (i++ > 2) {
                            break;
                        }
                    } catch (InterruptedException ie) {
                    }
                }
                if (null != sem) {
                    sem.update(callBackOrRoute, infoFromSDK, apiError);
                    sem.deactivate();
                }
                ;
            }
        });
        thread.start();
    }

    /**
     * Returns a semaphore which can be used for waiting to callbacks
     * @param callBackId The ID to identify the relation between the MTI call and the callback of MTI.
     *                   This ID is used to wait for the callback.
     * @param semaphoreInfo String which can be used to identify the semaphore.
     * @return A semaphore which can be used to wait until a callback comes
     */
    public static Semaphore getSemaphoreForCallBack(int callBackId, String semaphoreInfo) {
        logger.finest("getSemaphoreForCallBack", "callBackId = " + callBackId + ", info = " + semaphoreInfo + ")");
        Semaphore sem = retrieveSemaphore(callBackId);
        if (null != sem && sem.isWaiting()) {
            // still there was a semaphore waiting to the same callBackId
            // in this version this means that the last call was not successfull and we return a MESSAGE NOT SEND
            Semaphore dummySem = addSemaphore(-1, semaphoreInfo, logger);
            dummySem.apiError = ApiError.MESSAGE_NOT_SEND;
            dummySem.interruptByError();
            return dummySem;
        }
        if (null != sem) {
            removeSemaphore(callBackId);
        }

        return addSemaphore(callBackId, semaphoreInfo, logger);
    }

    public static void setUserInterrupt(int callBack) {
        manager.interruptByUser(callBack);
    }

    public static Semaphore addSemaphore(int callBack, String info, Logger logger) {
        return manager.createSemaphore(callBack, info, logger);
    }

    public static void removeSemaphore(int callBack) {
        manager.dropSemaphore(callBack);
    }

    public static void setInterruptedByUser(int callBack) {
        manager.setUserInterrupt(callBack);
    }

    public static void setInterruptedByError(int callBack) {
        manager.setErrorInterrupt(callBack);
    }

    private Semaphore createSemaphore(int callBack, String info, Logger logger) {
        Semaphore sem = new Semaphore(info, logger);
        map.put(callBack, sem);
        return sem;
    }

    private static Semaphore retrieveSemaphore(int callBack) {
        if (map.containsKey(callBack)) {
            return map.get(callBack);
        }
        return null;
    }

    private void dropSemaphore(int callBack) {
        if (map.containsKey(callBack)) {
            map.remove(callBack);
        }
    }

    private void interruptByUser(int callBack) {
        Semaphore sem = retrieveSemaphore(callBack);
        if (null != sem) {
            sem.interrruptByUser();
        }
    }

    private void setErrorInterrupt(int callBack) {
        Semaphore sem = retrieveSemaphore(callBack);
        if (null != sem) {
            sem.interruptByError();
        }
    }

    // ============================================================
    //      The class Semaphore is used for waiting to callbacks
    // ============================================================
    public class Semaphore {
        private int callBackOrRoute;
        private String semaphoreInfo;
        private ApiError apiError = ApiError.OK;
        private boolean interruptedByUser = false;
        private boolean interruptedByError = false;
        private boolean interruptedByTimeOut = false;
        private boolean isActive = true;
        private Logger logger;

        public Semaphore(String info, Logger logger) {
            this.semaphoreInfo = info;
            this.logger = logger;
        }

        public ApiError waitForCallback() {
            return waitForCallback(null);
        }

        /**
         * Waits until the MTI calls the callback method.
         *
         * @param timeout Maximum time in millis to wait.
         *                If null or value = 0 there is no maximum time, means the function doesn't
         *                returns until the callback is called
         * @return
         */
        public ApiError waitForCallback(Long timeout) {
            if (null == timeout) {
                timeout = new Long(0);
            }

            logger.finest("waitForCallback", "Start wait for " + this.semaphoreInfo);
            long timeToStop = System.currentTimeMillis() + timeout;
            while (isActive && !(interruptedByTimeOut || interruptedByUser || interruptedByError)) {
                try {
                    Thread.sleep(100);
                    if (timeout > 0 && System.currentTimeMillis() > timeToStop) {
                        interruptedByTimeOut = true;
                        break;
                    }
                } catch (InterruptedException ie) {
                }
            }
            logger.finest("waitForCallback", "End wait for " + this.semaphoreInfo);

            isActive = false;
            if (interruptedByTimeOut) return ApiError.TIMEOUT;
            if (interruptedByError) return ApiError.INVALID_OPERATION;
            if (interruptedByUser) return ApiError.OPERATION_CANCELED;
            return apiError;
        }

        public boolean isWaiting() {
            return isActive && !(interruptedByError || interruptedByUser || interruptedByTimeOut);
        }

        public void update(int callBackOrRoute, String infoFromSDK, ApiError apiError) {
            this.callBackOrRoute = callBackOrRoute;
            this.apiError = apiError;
        }

        public void deactivate() {
            this.isActive = false;
        }

        public void interrruptByUser() {
            this.interruptedByUser = true;
        }

        public void interruptByTimeout() {
            this.interruptedByTimeOut = true;
        }

        public void interruptByError() {
            this.interruptedByError = true;
        }

        public String getSemaphoreInfo() {
            return semaphoreInfo;
        }
    }
}

