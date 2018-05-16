package com.example.android.mygarden.ui;

import android.app.IntentService;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.util.Log;

import com.example.android.mygarden.R;
import com.example.android.mygarden.provider.PlantContract;
import com.example.android.mygarden.utils.PlantUtils;

import static com.example.android.mygarden.provider.PlantContract.BASE_CONTENT_URI;
import static com.example.android.mygarden.provider.PlantContract.PATH_PLANTS;

public class PlantWateringService extends IntentService {

    private static final String LOG_TAG = PlantWateringService.class.getSimpleName();

    public static final String ACTION_WATER_PLANTS =  "com.example.android.mygarden.action.water_plants";
    public static final String ACTION_UPDATE_PLANT_WIDGET = "com.example.android.mygarden.action.update_plant_widget";
    public static final String EXTRA_PLANT_ID = "com.example.android.mygarden.extra.PLANT_ID";



    public PlantWateringService() {
        super("PlantWateringService");
    }

    public static void startActionUpdateWaterPlants(Context context) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_UPDATE_PLANT_WIDGET);
        context.startService(intent);
    }

    public static void startActionWaterPlants(Context context, long plantId) {
        Intent intent = new Intent(context, PlantWateringService.class);
        intent.setAction(ACTION_WATER_PLANTS);
        intent.putExtra(EXTRA_PLANT_ID, plantId);
        context.startService(intent);
    }


    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_WATER_PLANTS.equals(action)) {
                final long plantId = intent.getLongExtra(EXTRA_PLANT_ID,PlantContract.INVALID_PLANT_ID);
                handleActionWaterPlants(plantId);
            }else if (ACTION_UPDATE_PLANT_WIDGET.equals(action))
            {
                handleActionUpdatePlantsWidgets();
            }
        }
    }

        private void handleActionWaterPlants(long plantId){

            Uri SINGLE_PLANT_URI = ContentUris.withAppendedId(
                    BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build(), plantId);
            ContentValues contentValues = new ContentValues();
            long timeNow = System.currentTimeMillis();
            contentValues.put(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME, timeNow);
            // Update only plants that are still alive
            getContentResolver().update(
                    SINGLE_PLANT_URI,
                    contentValues,
                    PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME+">?",
                    new String[]{String.valueOf(timeNow - PlantUtils.MAX_AGE_WITHOUT_WATER)});

            Log.d(LOG_TAG, "Plants have been watered successfully. ");

            startActionUpdateWaterPlants(this);

        }

        private void handleActionUpdatePlantsWidgets()
        {
            Uri PLANTS_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PLANTS).build();

            //Get the plant which is about to Die
            Cursor cursor = getContentResolver().query(
                    PLANTS_URI,
                    null,
                    null,
                    null,
                    PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME
            );

            int imgRes = R.drawable.grass;
            long plantId = PlantContract.INVALID_PLANT_ID;
            boolean needWater = false;

            if(cursor != null && cursor.getCount() > 0)
            {
                cursor.moveToFirst();

                int createTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_CREATION_TIME);
                int waterTimeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_LAST_WATERED_TIME);
                int plantTypeIndex = cursor.getColumnIndex(PlantContract.PlantEntry.COLUMN_PLANT_TYPE);
                int plantIdIndex = cursor.getColumnIndex(PlantContract.PlantEntry._ID);

                long currentTime = System.currentTimeMillis();
                long wateredAt = cursor.getLong(waterTimeIndex);
                long createdAt = cursor.getLong(createTimeIndex);
                int plantType = cursor.getInt(plantTypeIndex);

                plantId = cursor.getLong(plantIdIndex);
                needWater = (currentTime - wateredAt) > PlantUtils.MIN_AGE_BETWEEN_WATER &&
                            (currentTime - wateredAt) < PlantUtils.MAX_AGE_WITHOUT_WATER;
                imgRes = PlantUtils.getPlantImageRes(this, currentTime-createdAt, currentTime-wateredAt, plantType);
                cursor.close();

                AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(this);
                int[] appWidgetId = appWidgetManager.getAppWidgetIds(new ComponentName(this, PlantWidgetProvider.class));

                appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid_view);

                PlantWidgetProvider.plantUpdateAppWidget(this, appWidgetManager, imgRes, plantId, needWater, appWidgetId);
            }
        }


    }

