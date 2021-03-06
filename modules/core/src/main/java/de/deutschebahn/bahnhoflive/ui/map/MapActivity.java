package de.deutschebahn.bahnhoflive.ui.map;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProviders;

import com.google.android.gms.maps.MapFragment;

import java.util.ArrayList;

import de.deutschebahn.bahnhoflive.R;
import de.deutschebahn.bahnhoflive.analytics.StationTrackingManager;
import de.deutschebahn.bahnhoflive.analytics.TrackingManager;
import de.deutschebahn.bahnhoflive.backend.hafas.model.HafasTimetable;
import de.deutschebahn.bahnhoflive.repository.InternalStation;
import de.deutschebahn.bahnhoflive.repository.Station;
import de.deutschebahn.bahnhoflive.ui.FragmentArgs;
import de.deutschebahn.bahnhoflive.ui.map.content.rimap.RimapFilter;
import de.deutschebahn.bahnhoflive.ui.station.ElevatorIssuesLoaderFragment;
import de.deutschebahn.bahnhoflive.ui.station.LoaderFragment;
import de.deutschebahn.bahnhoflive.ui.station.ParkingOccupancyLoaderFragment;
import de.deutschebahn.bahnhoflive.view.BackHandlingFragment;

public class MapActivity extends AppCompatActivity implements
        FilterFragment.Host, MapOverlayFragment.Host, TrackingManager.Provider {

    private final static String ARG_STATION = FragmentArgs.STATION;
    private static final String ARG_LOADER_STATES = LoaderFragment.ARG_LOADER_STATES;
    private static final String ARG_STATION_DEPARTURES = "stationDepartures";

    private MapFragment mapFragment;
    private MapOverlayFragment overlayFragment;

    private Station station;

    @NonNull
    private TrackingManager trackingManager;

    private ParkingOccupancyLoaderFragment parkingOccupancyLoaderFragment;
    private MapViewModel mapViewModel;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Intent intent = getIntent();
        if (intent.hasExtra(ARG_STATION)) {
            station = intent.getParcelableExtra(ARG_STATION);
        }
        final Bundle loaderStates;
        if (intent.hasExtra(ARG_LOADER_STATES)) {
            loaderStates = intent.getBundleExtra(ARG_LOADER_STATES);
        } else {
            loaderStates = null;
        }

        mapViewModel = ViewModelProviders.of(this).get(MapViewModel.class);
        mapViewModel.setStation(station);

        trackingManager = station == null ? new TrackingManager() : new StationTrackingManager(station);

        parkingOccupancyLoaderFragment = ParkingOccupancyLoaderFragment.of(this);
        final ElevatorIssuesLoaderFragment elevatorIssuesLoaderFragment = ElevatorIssuesLoaderFragment.of(this);

        if (station != null) {
            parkingOccupancyLoaderFragment.setStation(station);
            elevatorIssuesLoaderFragment.setStation(station);
        }
        if (loaderStates != null) {
            parkingOccupancyLoaderFragment.useInstanceStateIfBetter(loaderStates);
        }

        setContentView(R.layout.activity_map);

        mapFragment = (MapFragment) getFragmentManager().findFragmentById(R.id.map_fragment);
        overlayFragment = (MapOverlayFragment) getSupportFragmentManager().findFragmentById(R.id.map_overlay_fragment);

        if (intent.hasExtra(ARG_STATION_DEPARTURES)) {
            overlayFragment.setStationDepartures(intent.getParcelableArrayListExtra(ARG_STATION_DEPARTURES));
        }

        final View contentView = findViewById(android.R.id.content);
        if (contentView != null) {
            contentView.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
                final View mapFragmentView = mapFragment.getView();
                if (mapFragmentView != null) {
                    mapViewModel.mapLaidOut(mapFragmentView.isLaidOut());
                }
            });
        }
        mapFragment.getMapAsync(overlayFragment);
    }

    @Override
    protected void onStart() {
        super.onStart();

        trackingManager.track(TrackingManager.TYPE_STATE, TrackingManager.Screen.F1);
    }

    public static Intent createIntent(Context context, Station station) {
        final Intent intent = createIntent(context);

        intent.putExtra(ARG_STATION, station instanceof Parcelable ?
                (Parcelable) station : new InternalStation(station));

        return intent;
    }

    private static Intent createIntent(Context context) {
        return new Intent(context, MapActivity.class);
    }

    @NonNull
    public static Intent createIntent(Context context, ArrayList<HafasTimetable> stationDepartures) {
        final Intent intent = createIntent(context);

        intent.putExtra(ARG_STATION_DEPARTURES, stationDepartures);

        return intent;
    }

    @NonNull
    public static Intent createIntent(Context context, Station station, ArrayList<HafasTimetable> stationDepartures) {
        final Intent intent = createIntent(context, station);

        intent.putExtra(ARG_STATION_DEPARTURES, stationDepartures);

        return intent;
    }

    @Override
    public void onFilterClick() {
        getSupportFragmentManager().beginTransaction()
                .addToBackStack("filter")
                .add(R.id.filter_fragment_container, new FilterFragment())
                .commit();

        trackingManager.track(TrackingManager.TYPE_ACTION, TrackingManager.Screen.F3);
    }

    @Override
    public void onDismissFilterFragment(FilterFragment filterFragment) {
        getSupportFragmentManager().popBackStack("filter", FragmentManager.POP_BACK_STACK_INCLUSIVE);
    }

    @Override
    public RimapFilter getFilter() {
        return overlayFragment.getFilter();
    }

    @Override
    public void onFilterChanged() {
        overlayFragment.onFilterChanged();
    }

    @Override
    public void onBackPressed() {
        final Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.filter_fragment_container);
        if (fragment instanceof BackHandlingFragment) {
            if (((BackHandlingFragment) fragment).onBackPressed()) {
                return;
            }
        }

        super.onBackPressed();
    }

    @NonNull
    @Override
    public TrackingManager getStationTrackingManager() {
        return trackingManager;
    }
}
