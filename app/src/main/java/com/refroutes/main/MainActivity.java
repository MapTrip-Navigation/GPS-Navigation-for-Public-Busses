package com.refroutes.main;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.refroutes.R;
import com.refroutes.file.RefRouteController;
import com.refroutes.file.Settings;
import com.refroutes.itemList.RefRoutesAdapter;
import com.refroutes.log.Logger;
import com.refroutes.model.RefRoute;

import java.util.ArrayList;
import java.util.logging.Level;

import de.infoware.android.mti.enums.ApiError;

public class MainActivity extends AppCompatActivity implements RefRouteDialog.RefRouteDialogListener, RefRoutesAdapter.ItemClickListener {
    private static final int REFROUTE_DIALOG_MODE_ADD = 1;
    private static final int REFROUTE_DIALOG_MODE_EDIT = 2;
    private boolean dialogIsActive = false;

    private static final String MARK_COLOR_ROUTE_DONE = "#9CB548";
    private static final String MARK_COLOR_NOT_ACTIVE = "#E6EAED";
    private static final String MARK_COLOR_NOT_DONE = "#FFFFFF";
    private static final String MARK_COLOR_IN_WORK = "#BD1550";
    private static final String MARK_COLOR_PAUSED = "#E97F02";
    public static final String mapTripAppSystemName = "de.infoware.maptrip.navi.license";
    public static final String mapTripCompanionActivityClass = "de.infoware.maptrip.CompanionActivity";

    private static MainActivity mainActivity;

    private static Thread routingThread;
    private static Thread mtiThread;
    private static Thread serverThread;

    private ArrayList<RefRoute> refRoutes;
    private RecyclerView rvRefRoutes;

    private String routesFileDirectory;
    private boolean isRoutingActive = false;
    private boolean isPaused = false;
    private boolean pauseButtonWasClicked = false;
    private boolean mtiInitialized = false;

    private RefRouteManager refRouteManager;
    private Logger logger;

    // ======================================================================================================
    // GUI control - reacting to user actions around route selection, adding and so on
    // ======================================================================================================

    /**
     * React to GO Button click
     */
    public void startOrStopRouting(View view) {
        logger.finest("startOrStopRouting", "Main control button was clicked");

        if (isRoutingActive && !isPaused) {
            // Make a PAUSE
            buttonPauseClicked();
        } else {
            // START routing
            buttonGoClicked();
        }
    }


    @Override
    public void refRouteDialogCancel() {
        dialogIsActive = false;
    }

    /**
     * Callback when RefRouteDialog is finished with OK
     *
     * @param refRouteName        Name edited by user
     * @param refRouteDescription Description edited by user
     * @param listIndex           Is equal to or greater than 0 if dialogMode is REFROUTE_DIALOG_MODE_EDIT;
     *                            is NULL or lower than 0 if dialogMode is REFROUTE_DIALOG_MODE_ADD.
     * @param dialogMode          Distinguishes between EDIT or ADD mode.
     */
    @Override
    public void refRouteDialogOk(String refRouteName, String refRouteDescription, String refRouteFileName, Integer listIndex, int dialogMode) {
        // aware that listIndex can be -1 or null if dialogMode is to add - so ignore listIndex when adding an item

        switch (dialogMode) {
            case REFROUTE_DIALOG_MODE_ADD:
                int newListIndex = refRoutes.size();
                RefRoute newRefRoute = new RefRoute(refRouteName, refRouteDescription, refRouteFileName, newListIndex, false);
                refRoutes.add(newListIndex, newRefRoute);
                rvRefRoutes.getAdapter().notifyItemInserted(newListIndex);
                rvRefRoutes.getAdapter().notifyItemRangeChanged(listIndex, refRoutes.size());
                RefRouteController.writeRefRoutesToFile(routesFileDirectory, refRoutes);

                // if there are refroutes activate button
                activateGoButton(refRoutes.size() > 0);
                break;

            case REFROUTE_DIALOG_MODE_EDIT:
                if (null == listIndex || 0 > listIndex) {
                    Toast.makeText(this, "Fehler: Index des Listeneintrags unbekannt. " + "/n" + "Änderung wird verworfen", Toast.LENGTH_LONG);
                    return;
                }

                RefRoute refRoute = refRoutes.get(listIndex);
                refRoute.setRefRouteName(refRouteName);
                refRoute.setRefRouteDescription(refRouteDescription);
                refRoute.setRefRouteFileName(refRouteFileName);
                rvRefRoutes.getAdapter().notifyItemChanged(listIndex);
                RefRouteController.writeRefRoutesToFile(routesFileDirectory, refRoutes);
                break;

            default:
                Toast.makeText(this, "Fehler: Unbekannter Dialogmodus. " + "/n" + "Änderung wird verworfen", Toast.LENGTH_LONG);
        }
        dialogIsActive = false;
    }

    private void updateItemChecked(int listIndex) {
        RefRoute refRoute = refRoutes.get(listIndex);
        refRoute.setActive(!refRoute.isActive());
        RefRouteController.writeRefRoutesToFile(routesFileDirectory, refRoutes);
    }

    private void updateItemRange() {
        RefRouteController.writeRefRoutesToFile(routesFileDirectory, refRoutes);
    }

    private void deleteItem(int listIndex) {
        refRoutes.remove(listIndex);
        rvRefRoutes.getAdapter().notifyItemRemoved(listIndex);
        rvRefRoutes.getAdapter().notifyItemRangeChanged(listIndex, refRoutes.size());
        RefRouteController.writeRefRoutesToFile(routesFileDirectory, refRoutes);
        // Show user that he can start
        activateGoButton(refRoutes.size() > 0);
    }

    private void editItem(int listIndex) {
        String refRouteName = refRoutes.get(listIndex).getRefRouteName();
        String refRouteDescription = refRoutes.get(listIndex).getRefRouteDescription();
        String refRouteFileName = refRoutes.get(listIndex).getRefRouteFileName();

        RefRouteDialog dialog = new RefRouteDialog();
        dialog.setRefRouteName(refRouteName);
        dialog.setRefRouteDescription(refRouteDescription);
        dialog.setRefRouteFileName(refRouteFileName);
        dialog.setListIndex(listIndex);
        dialog.setDialogMode(REFROUTE_DIALOG_MODE_EDIT);

        dialog.show(getSupportFragmentManager(), "Referenzroute bearbeiten");
    }

    /**
     * Shows dialog with new reference route name and description
     */
    public void addItem(View view) {
        if (dialogIsActive) {
            return;
        }

        RefRouteDialog dialog = new RefRouteDialog();
        dialog.setDialogMode(REFROUTE_DIALOG_MODE_ADD);
        dialog.show(getSupportFragmentManager(), "Referenzroute hinzufügen");
    }

    public boolean changeItems(int oldPos, int newPos) {
        RefRoute itemMoveToPosOld = refRoutes.get(newPos);
        RefRoute itemMoveToPosNew = refRoutes.get(oldPos);
        refRoutes.set(newPos, itemMoveToPosNew);
        refRoutes.set(oldPos, itemMoveToPosOld);

        rvRefRoutes.getAdapter().notifyItemChanged(newPos);
        rvRefRoutes.getAdapter().notifyItemChanged(oldPos);

        updateItemRange();

        return true;
    }

    /**
     * React to click on a refroute item.
     * Distinguishes between textfield, checkbox and button
     *
     * @param view
     * @param listIndex
     */
    @Override
    public void itemClicked(View view, int listIndex) {
        int viewId = view.getId();
        switch (viewId) {
            case R.id.itemCheckBox:
                // Eintrag in Datei updaten
                updateItemChecked(listIndex);
                break;

            case R.id.itemRefRouteName:
                // Click on item text
                editItem(listIndex);
                break;

            case R.id.itemDeleteButton:
                deleteItem(listIndex);
                break;
            default:
        }
    }

    public static void showMessage(String title, String message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(mainActivity);
        builder.setTitle(title);
        builder.setMessage(message);
        builder.setNeutralButton("OK", null);
        builder.show();
    }

    // ======================================================================================================
    // Logical flow around routing
    // ======================================================================================================

    private void resetRouting() {
        logger.finest("resetRouting", "Activate GO Button");
        Button button = findViewById(R.id.buttonGo);
        button.setText("GO");
        button.setHintTextColor(Color.parseColor(MARK_COLOR_IN_WORK));
        isPaused = false;
        isRoutingActive = false;
    }

    private void pauseOnAutoPilotOff() {
        logger.finest("pauseOnAutoPilotOff", "React to user action");
        Button button = findViewById(R.id.buttonGo);
        button.setText("FORTSETZEN");
        button.setHintTextColor(Color.parseColor(MARK_COLOR_PAUSED));
        isPaused = false;
        isRoutingActive = false;
    }

    private void buttonPauseClicked() {
        logger.finest("buttonPauseClicked", "React to user action");

        // if 'resume' was clicked and MapTrip not was yet on top
        // the button may not be clicked once again
        if (pauseButtonWasClicked) {
            return;
        }
        pauseButtonWasClicked = true;

        Button button = findViewById(R.id.buttonGo);
        button.setText("FORTSETZEN");
        button.setHintTextColor(Color.parseColor(MARK_COLOR_PAUSED));
        isPaused = true;
        refRouteManager.interruptRouting();
    }

    private void buttonGoClicked() {
        logger.finest("buttonGoClicked", "");
        Button button = findViewById(R.id.buttonGo);
        boolean restartTour = false;
        button.setText("PAUSE");
        button.setHintTextColor(Color.parseColor(MARK_COLOR_NOT_DONE));

        if (!refRouteManager.isWorking()) {
            refreshAllItems();
        }

        // if before active route was paused, resume
        // resume means that it's not recommended to start route from beginning of tour
        if (isPaused) {
            restartTour = true;
        }

        isPaused = false;
        isRoutingActive = true;

        routeRefRoute(restartTour);
    }

    private void reactToMessageButton() {
        // showMessageButton is part of MTI
        // MTI must be initialized before using the MessageButton
        if (!mtiInitialized) {
            return;
        }
        refRouteManager.showMessageButton();

    }

    /*
    Called when a reference route was finished or cancelled or an exception was triggered
    This function has to decide what to do next; wait for user action or start the next route (if there are more routes to drive)
     */
    private void routeFinished(ApiError callbackResult) {
        logger.info("routeFinished", "callbackResult = " + callbackResult.name());

        switch (callbackResult) {
            // Last route was finished successfull
            case OK:
                // mark route as finished
                markItemDone(refRouteManager.getLastRefRouteId());

                // If all routes done finish by waiting for user interaction
                Switch autoPilot = findViewById(R.id.switchAutopilot);
                if (!refRouteManager.nextRefRouteExists()) {
                    resetRouting();
                    refRouteManager.reset();
                    hideMapTrip(refRouteManager);
                } else if (!autoPilot.isChecked()) {
                    // AutoPilot OFF: hide MapTrip and make a pause
                    pauseOnAutoPilotOff();
                    hideMapTrip(refRouteManager);
                } else {
                    // AutoPilot ON: resume with next route
                    routeRefRoute(false);
                }
                break;

            // last route was canceled
            case OPERATION_CANCELED:
                markItemPaused(refRouteManager.getActiveRefRouteId());
                break;

            default:
                showMessage("Fehler", "Route konnte nicht gestartet oder beendet werden: " + callbackResult.name());
        }
    }

    /*
    Take a reference route and start it.
    If the last route was paused it is resumed.
    If the last route was finished start the next route.
     */
    private void routeRefRoute(boolean restartTour) {
        logger.info("routeRefRoute", "see if there is to start a reference route; restartTour = " + restartTour);
        if (!restartTour) {
            logger.finest("routeRefRoute", "call mti.resetWorkingSwitch");
            refRouteManager.resetWorkingSwitch();
        }

        if (refRouteManager.nextRefRouteExists()) {
            logger.finest("routeRefRoute", "one more reference route exists; routeItem");
            int nextRefRouteId = refRouteManager.getNextRefRouteId();
            markItemInWork(nextRefRouteId);
            routeItem(this, this.refRouteManager, nextRefRouteId, restartTour);
        } else {
            logger.finest("routeRefRoute", "no more reference route exists; resetRouting, mtiCalls.reset");
            resetRouting();
            refRouteManager.reset();
            hideMapTrip(refRouteManager);
        }
    }

    // ======================================================================================================
    // Activity dedicated
    // ======================================================================================================

    @Override
    public void onDestroy() {
        super.onDestroy();
        logger.logWithBlocking("onDestroy", "Anwendung wird beendet.", Level.INFO);
    }

    @Override
    public void onResume() {
        super.onResume();
        pauseButtonWasClicked = false;
        logger.fine("onResume", "Anwendung wurde in den Vordergrund geholt");
        reactToMessageButton();
    }

    /**
     * To keep the switch from MapTrip to this view simple, this App is a singleTask (see Manifest)
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Lookup the recyclerview in activity layout
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainActivity = this;

        ActionBar actionBar = getSupportActionBar();
        ColorDrawable colorDrawable
                = new ColorDrawable(Color.parseColor("#FFFFFF"));

        // Set BackgroundDrawable
        actionBar.setBackgroundDrawable(colorDrawable);

        setTitle("Reference Routes");

        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayUseLogoEnabled(true);
        actionBar.setTitle("Reference Routes");
        actionBar.setLogo(R.drawable.logo_maptrip_refroutes_148);

        // Search instance of RecyclerView
        rvRefRoutes = (RecyclerView) findViewById(R.id.recyclerViewRefRoutes);

        String basePath = getExternalFilesDir(null).getAbsolutePath();

        // Initialize reference routes list
        routesFileDirectory = basePath + "/routes";

        // Prepare Logger
        // Basic path of files - here should be stored the loggers property file (if used)
        Logger.setBasePath(basePath);
        // Get a logger instance
        logger = Logger.createLogger("MainActivity");

        logger.finest("onCreate", "-->       New Instance        <--");
        logger.info("onCreate", "RefRoute App wird initialisiert");
        logger.config("onCreate", "Dateiverzeichnis: " + getExternalFilesDir(null).getAbsolutePath());
        RefRouteController.initSequenceFile(routesFileDirectory);

        refRoutes = RefRouteController.readRefRoutesFromFile(routesFileDirectory);
        // Create adapter passing in the sample user data
        final RefRoutesAdapter adapter = new RefRoutesAdapter(refRoutes, this);

        // Attach the adapter to the recyclerview to populate items
        rvRefRoutes.setAdapter(adapter);

        // SWIPE and MOVE
        ItemTouchHelper.SimpleCallback simpleItemTouchCallback =
                new ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP | ItemTouchHelper.DOWN, 0) {

                    @Override
                    public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder
                            target) {
                        final int fromPos = viewHolder.getAdapterPosition();
                        final int toPos = target.getAdapterPosition();
                        return changeItems(fromPos, toPos);
                    }

                    @Override
                    public void onSwiped(RecyclerView.ViewHolder viewHolder, int swipeDir) {
//                        refRoutes.remove(viewHolder.getAdapterPosition());
//                        adapter.notifyItemRemoved(viewHolder.getAdapterPosition());
                    }
                };
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(simpleItemTouchCallback);
        itemTouchHelper.attachToRecyclerView(rvRefRoutes);

        // Set layout manager to position the items
        // LinearLayoutManager for usage of dividers
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvRefRoutes.setLayoutManager(new LinearLayoutManager(this));

        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvRefRoutes.getContext(),
                layoutManager.getOrientation());
        rvRefRoutes.addItemDecoration(dividerItemDecoration);

        Settings settings = new Settings(basePath + "/refroutechains.properties");
        // init worker
        refRouteManager = new RefRouteManager(refRoutes);

        initMti(this, refRouteManager, settings.getValue("startMapTrip", Boolean.TRUE));
    }

    /**
     * Reacts to MTI Initialization.
     *
     * @param apiError If ApiError.OK MTI is initialized and the user can use routes. Otherwise the GO Button keeps disabled.
     */
    private void mtiInitFinished(ApiError apiError) {
        switch (apiError) {
            case OK:
                // Bring App to front
                hideMapTrip(refRouteManager);

                // Show user that he can start
                activateGoButton(refRoutes.size() > 0);
                mtiInitialized = true;
                break;

            default:
                String apiErrorName = apiError.name();
                logger.warn("waitForMtiInit", "waitForMtiInit Error in initialisation: " + apiErrorName);
                Toast.makeText(this, "Fehler bei Initialisierung: " + apiErrorName, Toast.LENGTH_LONG);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
        }
    }

    private void setMapTripToFront() {
        refRouteManager.showMessageButton();
        logger.fine("setMapTripToFront", "bring MapTrip Companion to front as " + mapTripCompanionActivityClass);
        Intent intent = new Intent();
        intent.setComponent(new ComponentName(mapTripAppSystemName, mapTripCompanionActivityClass));
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private boolean startMapTrip() {
        logger.info("startMapTrip", "Launching MapTrip as: " + mapTripAppSystemName);
        Intent intent = getPackageManager().getLaunchIntentForPackage(mapTripAppSystemName);
        if (null == intent) {
            logger.warn("startMapTrip", "Intent is NULL: getPackageManager().getLaunchIntentForPackage(mapTripAppSystemName) failed.");
        }

        try {
            startActivity(intent);
            return true;
        } catch (ActivityNotFoundException eToo) {
            logger.severe("startMapTrip", "MapTrip could not be started. SystemName =  " + mapTripAppSystemName + ";" + eToo.getMessage());
            Toast.makeText(this, "Maptrip konnte nicht gestartet werden", Toast.LENGTH_LONG);
        } catch (Exception ex) {
            logger.severe("startMapTrip", "MapTrip could not be started. SystemName =  " + mapTripAppSystemName + ";" + ex.getMessage());
            Toast.makeText(this, "Maptrip konnte nicht gestartet werden", Toast.LENGTH_LONG);
        }
        return false;
    }

    // ======================================================================================================
    // GUI elements manipulation
    // ======================================================================================================

    private void markItemPaused(int refRouteId) {
        try {
            RecyclerView.ViewHolder holder = rvRefRoutes.findViewHolderForAdapterPosition(refRouteId);
            holder.itemView.setBackgroundColor(Color.parseColor(MARK_COLOR_PAUSED));
            holder.itemView.findViewById(R.id.itemDeleteButton).setBackgroundColor(Color.parseColor(MARK_COLOR_PAUSED));
            holder.itemView.refreshDrawableState();
        } catch (Exception e) {
            logger.warn("markItemPause", "Fehler bei markItemPaused: " + e.getMessage());
        }
    }

    private void markItemDone(int refRouteId) {
        try {
            RecyclerView.ViewHolder holder = rvRefRoutes.findViewHolderForAdapterPosition(refRouteId);
            holder.itemView.setBackgroundColor(Color.parseColor(MARK_COLOR_ROUTE_DONE));
            holder.itemView.findViewById(R.id.itemDeleteButton).setBackgroundColor(Color.parseColor(MARK_COLOR_ROUTE_DONE));
            holder.itemView.refreshDrawableState();
        } catch (Exception e) {
            logger.warn("markItemDone", "Fehler bei markItemDone: " + e.getMessage());
        }
    }

    private void markItemNotDone(int refRouteId) {
        try {
            RecyclerView.ViewHolder holder = rvRefRoutes.findViewHolderForAdapterPosition(refRouteId);
            holder.itemView.setBackgroundColor(Color.parseColor(MARK_COLOR_NOT_DONE));
            holder.itemView.findViewById(R.id.itemDeleteButton).setBackgroundColor(Color.parseColor(MARK_COLOR_NOT_DONE));
            holder.itemView.refreshDrawableState();
        } catch (Exception e) {
            logger.warn("markItemNotDone", "Fehler bei markItemNotDone: " + e.getMessage());
        }
    }

    private void markItemInWork(final int refRouteId) {
        try {
            RecyclerView.ViewHolder holder = rvRefRoutes.findViewHolderForAdapterPosition(refRouteId);
            holder.itemView.setBackgroundColor(Color.parseColor(MARK_COLOR_IN_WORK));
            holder.itemView.findViewById(R.id.itemDeleteButton).setBackgroundColor(Color.parseColor(MARK_COLOR_IN_WORK));
            holder.itemView.refreshDrawableState();
        } catch (Exception e) {
            logger.warn("markItemInWork", "Fehler bei markItemInWork: " + e.getMessage());
        }
    }

    /**
     * Unmark list items
     */
    private void refreshAllItems() {
        for (int i = 0; i < refRoutes.size(); i++) {
            markItemNotDone(i);
        }
    }

    private void resetItemWorkingStates() {
        for (int i = 0; i < refRoutes.size(); i++) {
            if (refRoutes.get(i).isFinished()) {
                markItemDone(i);
            }
        }
    }

    private void activateGoButton(boolean active) {
        Button button = findViewById(R.id.buttonGo);
        button.setEnabled(active);
        button.setClickable(active);

        if (active) {
            button.setTextColor(Color.parseColor("#8BC34A"));
        } else {
            button.setTextColor(Color.parseColor("#000000"));
        }
    }


    // ======================================================================================================
    // Threads
    // ======================================================================================================
    private void hideMapTrip(final RefRouteManager refRouteManager) {
        serverThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("HideMapThread");
                String className = MainActivity.this.getClass().getCanonicalName();
                String packageName = getPackageName();
                refRouteManager.showApp(packageName, className);
            }
        });
        serverThread.start();
    }


    /*
     * Starts routing of one reference routing file.
     *
     * @param activity      This activity
     * @param workerService Instance of the worker service
     * @param refRouteIndex Index of the route. Used to get the required informations which are stored in an array.
     */
    private void routeItem(final MainActivity activity, final RefRouteManager refRouteManager, final int refRouteIndex, final boolean restartTour) {
        routingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                Thread.currentThread().setName("RoutingThread");
                String className = MainActivity.this.getClass().getCanonicalName();
                String packageName = getPackageName();

                final ApiError result = refRouteManager.routeItem(packageName, className, routesFileDirectory, refRouteIndex, restartTour);
                // successfully routed, initialize next route
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.routeFinished(result);
                    }
                });
            }
        });
        routingThread.start();
        setMapTripToFront();
    }


    private ApiError checkMapTripRunning(boolean startApp) {
        ApiError findServerResult = refRouteManager.findServer();
        if (ApiError.COULD_NOT_FIND_SERVER == findServerResult || ApiError.TIMEOUT == findServerResult) {
            logger.info("initMti", "MapTrip not running, starting App");
            if (startApp) {
                startMapTrip();
            }
        }
        return findServerResult;
    }


    /*
    Initialize MTI
     */
    private void initMti(final MainActivity activity, final RefRouteManager refRouteManager, final boolean startMapTripRequested) {
        logger.info("initMti", "Initializing MTI");

        if (mtiInitialized) {
            return;
        }
        mtiThread = new Thread(new Runnable() {
            @Override
            public void run() {
                Thread.currentThread().setName("MtiInitThread");

                int retries = 0;
                ApiError initMtiResult = ApiError.NOT_STARTED;
                boolean innerStartApp = startMapTripRequested;
                initMtiResult = refRouteManager.initMti(activity);
                while (retries++ < 10 && ApiError.OK != initMtiResult) {
                    if (startMapTripRequested) {
                        initMtiResult = checkMapTripRunning(innerStartApp);
                    }
                    try {
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    innerStartApp = false;
                    initMtiResult = refRouteManager.initMti(activity);
                }

                final ApiError apiErrorInRunnable = initMtiResult;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        activity.mtiInitFinished(apiErrorInRunnable);
                    }
                });

            }
        });
        mtiThread.start();
    }
}
