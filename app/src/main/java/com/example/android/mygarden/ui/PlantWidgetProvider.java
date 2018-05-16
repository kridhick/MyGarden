package com.example.android.mygarden.ui;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import com.example.android.mygarden.R;
import com.example.android.mygarden.provider.PlantContract;

/**
 * Implementation of App Widget functionality.
 */
public class PlantWidgetProvider extends AppWidgetProvider {

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int imgResource, long plantId, boolean showWater, int appWidgetId) {

        RemoteViews remoteViews;
        // Construct the RemoteViews object
        Bundle bundle = appWidgetManager.getAppWidgetOptions(appWidgetId);
        int width = bundle.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH);
        if (width < 300)
        {
            remoteViews = getSinglePlantRemoteView(context, imgResource, plantId, showWater);
        }
        else
        {
            remoteViews = getGridViewPlantRemoteView(context);
        }




        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
    }

    private static RemoteViews getSinglePlantRemoteView(Context context, int imgResource, long plantId, boolean showWater)
    {

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.plant_widget_provider);

        //Click on Plant Image
        Intent mainIntent;
        if (plantId == PlantContract.INVALID_PLANT_ID) {
            mainIntent = new Intent(context, MainActivity.class);
        } else { // Set on click to open the corresponding detail activity
            Log.d(PlantWidgetProvider.class.getSimpleName(), "plantId=" + plantId);
            mainIntent = new Intent(context, PlantDetailActivity.class);
            mainIntent.putExtra(PlantDetailActivity.EXTRA_PLANT_ID, plantId);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, mainIntent, 0);
        views.setOnClickPendingIntent(R.id.img_plant_widget, pendingIntent);


        //Click on Water Image
        Intent wateringIntent = new Intent(context, PlantWateringService.class);
        wateringIntent.setAction(PlantWateringService.ACTION_WATER_PLANTS);
        PendingIntent wateringPendingIntent = PendingIntent.getService(
                context,
                0,
                wateringIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.img_water_widget, wateringPendingIntent);

        //Setting the Plant Image
        views.setImageViewResource(R.id.img_plant_widget, imgResource);
        //Setting the plant name
        views.setTextViewText(R.id.tv_plant_name, String.valueOf(plantId));
        //Setting the visiblity of water drop
        if(showWater) views.setViewVisibility(R.id.img_water_widget, View.VISIBLE);
        else views.setViewVisibility(R.id.img_water_widget, View.INVISIBLE);

        return views;
    }

    private static RemoteViews getGridViewPlantRemoteView(Context context)
    {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_grid_view);
        // Set the GridWidgetService intent to act as the adapter for the GridView
        Intent intent = new Intent(context, GridWidgetService.class);
        views.setRemoteAdapter(R.id.widget_grid_view, intent);
        // Set the PlantDetailActivity intent to launch when clicked
        Intent appIntent = new Intent(context, PlantDetailActivity.class);
        PendingIntent appPendingIntent = PendingIntent.getActivity(context, 0, appIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        views.setPendingIntentTemplate(R.id.widget_grid_view, appPendingIntent);
        // Handle empty gardens
        views.setEmptyView(R.id.widget_grid_view, R.id.empty_view);
        return views;
    }



    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        PlantWateringService.startActionUpdateWaterPlants(context);


    }

    public static void plantUpdateAppWidget(Context context, AppWidgetManager appWidgetManager, int imgResource, long plantId, boolean showWater, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, imgResource, plantId, showWater, appWidgetId);
        }

    }

    @Override
    public void onEnabled(Context context) {
        // Enter relevant functionality for when the first widget is created
    }

    @Override
    public void onDisabled(Context context) {
        // Enter relevant functionality for when the last widget is disabled
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        PlantWateringService.startActionUpdateWaterPlants(context);
        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }
}

