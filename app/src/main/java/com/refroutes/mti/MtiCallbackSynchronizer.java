package com.refroutes.mti;

import com.refroutes.log.Logger;

import java.util.HashMap;
import java.util.logging.Level;

import de.infoware.android.mti.enums.ApiError;

public class MtiCallbackSynchronizer {
    private static HashMap<Integer, Semaphore> map = new HashMap<>();
    private static MtiCallbackSynchronizer manager = new MtiCallbackSynchronizer();
    private static Logger logger = Logger.createLogger("WaitForCallbackManager");


    public static Semaphore getSemaphoreForCallBack(int callBackId, String info) {
        logger.finest("getSemaphoreForCallBack", "callBackId = " + callBackId + ", info = " + info + ")");
        Semaphore sem = retrieveSemaphore(callBackId);
        if (null != sem && sem.isWaiting()) {
            Semaphore dummySem = addSemaphore(-1, info, logger);
            dummySem.apiError = ApiError.MESSAGE_NOT_SEND;
            dummySem.interruptByError();
            return dummySem;
        }
        if (null != sem) {
            removeSemaphore(callBackId);
        }

        return addSemaphore(callBackId, info, logger);
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


    public static void setUserInterrupt(int callBack) {
        manager.interruptByUser(callBack);
    }


    public static void callBackCalled(final int callBackOrRoute, final String infoFromSDK, final ApiError apiError, final String callBack) {
        logger.finest("callbackCalled", "Callback: " + callBack + "; infoFromSDK: " + infoFromSDK + "; ApiError = " + apiError);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("SemaphoreCreateWaiter");

                Semaphore sem = null;
                while (sem == null) {
                    sem = manager.retrieveSemaphore(callBackOrRoute);
                    int i = 0;
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

    public class Semaphore {
        private int callBackOrRoute;
        private String about;
        private ApiError apiError;
        private boolean interruptedByUser = false;
        private boolean interruptedByError = false;
        private boolean interruptedByTimeOut = false;
        private boolean isActive = true;
        private Logger logger;

        public Semaphore (String info, Logger logger) {
            this.about = info;
            this.logger = logger;
        }

        public ApiError waitForCallback () {
            return waitForCallback(null);
        }

        /**
         * Waits until the MTI calls the callback method.
         * @param timeToWait Maximum time in millis to wait.
         *                   If null or value = 0 there is no maximum time
         * @return
         */
        public ApiError waitForCallback(Long timeToWait) {
            if (null == timeToWait) {
                timeToWait = new Long(0);
            }

            logger.finest("waitForCallback", "Start wait for " + this.about);
            long timeToStop = System.currentTimeMillis() + timeToWait;
            while (isActive && !(interruptedByTimeOut || interruptedByUser || interruptedByError)) {
                try {
                    Thread.sleep(100);
                    if (timeToWait > 0 && System.currentTimeMillis() > timeToStop) {
                        interruptedByTimeOut = true;
                        break;
                    }
                } catch (InterruptedException ie) {
                }
            }
            logger.finest("waitForCallback", "End wait for " + this.about);

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

        public String getAbout() {
            return about;
        }
    }
}

