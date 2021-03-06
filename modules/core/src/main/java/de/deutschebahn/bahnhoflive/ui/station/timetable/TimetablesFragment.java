package de.deutschebahn.bahnhoflive.ui.station.timetable;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.deutschebahn.bahnhoflive.R;
import de.deutschebahn.bahnhoflive.analytics.TrackingManager;
import de.deutschebahn.bahnhoflive.backend.ris.model.RISTimetable;
import de.deutschebahn.bahnhoflive.backend.ris.model.TrainEvent;
import de.deutschebahn.bahnhoflive.backend.ris.model.TrainInfo;
import de.deutschebahn.bahnhoflive.ui.ToolbarViewHolder;
import de.deutschebahn.bahnhoflive.ui.station.HistoryFragment;
import de.deutschebahn.bahnhoflive.ui.station.StationViewModel;
import de.deutschebahn.bahnhoflive.ui.timetable.localtransport.HafasDeparturesFragment;

public class TimetablesFragment extends TwoTabsFragment {

    private boolean tabsInitialized = false;

    @Override
    protected void showFragment(int position) {
        final TrackingManager trackingManager = TrackingManager.fromActivity(getActivity());
        switch (position) {
            case 0:
                trackTap(trackingManager, TrackingManager.UiElement.TOGGLE_DB);
                showDbFragment();
                break;
            default:
                trackTap(trackingManager, TrackingManager.UiElement.TOGGLE_OEPNV);
                showLocalTransportFragment();
        }

        tabsInitialized = true;
    }

    private void trackTap(TrackingManager trackingManager, String uiElement) {
        if (tabsInitialized) {
            trackingManager.track(TrackingManager.TYPE_ACTION, TrackingManager.Screen.H2, TrackingManager.Action.TAP, uiElement);
        }
    }

    @Nullable
    public static TimetablesFragment findIn(HistoryFragment historyFragment) {
        final FragmentManager childFragmentManager = historyFragment.getChildFragmentManager();
        final Fragment fragment = childFragmentManager.findFragmentByTag("root");
        if (fragment instanceof TimetablesFragment) {
            return (TimetablesFragment) fragment;
        }

        return null;
    }

    public void switchTo(boolean localTransport, boolean arrivals, String trackFilter) {
        if (localTransport) {
            setTab(1);
        } else {
            final Fragment dbTimeableFragmentCandidate = getChildFragmentManager().findFragmentByTag(DbTimetableFragment.TAG);
            if (dbTimeableFragmentCandidate instanceof DbTimetableFragment) {
                final DbTimetableFragment dbTimetableFragment = (DbTimetableFragment) dbTimeableFragmentCandidate;
                dbTimetableFragment.setModeAndFilter(arrivals, trackFilter);
            }
            setTab(0);
        }
    }

    public interface Host {
        void showTimetablesFragment(boolean localTransport, boolean arrivals, String trackFilter);
    }

    public static final String TAG = TimetablesFragment.class.getSimpleName();

    private List<TrainInfo> departureTrainInfos;


    public void onUpdateRISTimetables(List<RISTimetable> timetables) {
        if (timetables == null) {
            return;
        }


        final ArrayList<TrainInfo> departureTrainInfos = new ArrayList<>();

        for (RISTimetable timetable : timetables) {
            for (TrainInfo trainInfo : timetable.getTrains()) {
                if (trainInfo.getDeparture() != null) {
                    departureTrainInfos.add(trainInfo);
                }
            }
        }

        Collections.sort(departureTrainInfos, new TrainInfo.Comparator(TrainEvent.DEPARTURE));

        this.departureTrainInfos = departureTrainInfos;
    }


    public TimetablesFragment() {
        super(R.string.tab_db, R.string.tab_local_transport, R.string.sr_tab_timetable_station, R.string.sr_tab_timetable_local);
    }

    protected void showLocalTransportFragment() {

        final String tag = HafasDeparturesFragment.Companion.getTAG();

        if (setFragment(tag, HafasDeparturesFragment.class)) {
            return;
        }

        installFragment(tag, new HafasDeparturesFragment());

    }

    protected void showDbFragment() {
        final String tag = DbTimetableFragment.TAG;

        if (setFragment(tag, DbTimetableFragment.class)) {
            return;
        }

        installFragment(tag, new DbTimetableFragment());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View view = super.onCreateView(inflater, container, savedInstanceState);

        final FragmentActivity activity = getActivity();
        if (activity != null) {
            final StationViewModel stationViewModel = new ViewModelProvider(activity).get(StationViewModel.class);

            final ToolbarViewHolder toolbarViewHolder = new ToolbarViewHolder(view);

            stationViewModel.getStationResource().getData().observe(getViewLifecycleOwner(), station -> {
                if (station != null) {
                    toolbarViewHolder.setTitle(station.getTitle());
                }
            });
        }

        return view;
    }
}
