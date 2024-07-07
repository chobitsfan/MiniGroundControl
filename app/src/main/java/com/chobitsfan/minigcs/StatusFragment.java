package com.chobitsfan.minigcs;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.text.Html;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import io.dronefleet.mavlink.common.Heartbeat;

public class StatusFragment extends Fragment {
    static final String[] GPS_FIX_TYPE = {"No GPS", "No Fix", "2D Fix", "3D Fix", "DGPS", "RTK Float", "RTK Fix"};
    private StatusViewModel mViewModel;
    public static StatusFragment newInstance() {
        return new StatusFragment();
    }
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_status, container, false);
    }
    @Override
    public void onViewCreated(@Nullable View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(StatusViewModel.class);

        mViewModel.getCurMode().observe(getViewLifecycleOwner(), mode->{
            TextView tv = requireActivity().findViewById(R.id.flight_mode);
            tv.setText(mode);
        });
        mViewModel.getStatusTxt().observe(getViewLifecycleOwner(), txt->{
            TextView tv = requireActivity().findViewById(R.id.status_txt);
            tv.append(txt+"\n");
        });
        mViewModel.getGlobalPos().observe(getViewLifecycleOwner(), pos->{
            TextView tv = requireActivity().findViewById(R.id.alt_msl);
            tv.setText(Html.fromHtml(String.format("<small>Altitude MSL</small><br><big><b>%.1f</b></big><small>m</small>", pos.alt()*0.001), Html.FROM_HTML_MODE_COMPACT));
            tv = requireActivity().findViewById(R.id.alt_home);
            tv.setText(Html.fromHtml(String.format("<small>Relative Alt</small><br><big><b>%.1f</b></big><small>m</small>", pos.relativeAlt()*0.001), Html.FROM_HTML_MODE_COMPACT));
        });
        mViewModel.getGpsStatus().observe(getViewLifecycleOwner(), gps->{
            TextView tv = requireActivity().findViewById(R.id.gps_status);
            tv.setText(Html.fromHtml("<small>GPS</small><br><big><b>"+GPS_FIX_TYPE[gps.fixType().value()]+"</b></big>", Html.FROM_HTML_MODE_COMPACT));
            tv = (TextView)requireActivity().findViewById(R.id.gps_hdop);
            tv.setText(Html.fromHtml("<small>HDOP</small><br><big><b>"+String.format("%.1f", gps.eph() * 0.01)+"</b></big>", Html.FROM_HTML_MODE_COMPACT));
            tv = (TextView)requireActivity().findViewById(R.id.gps_satellites);
            tv.setText(Html.fromHtml("<small>Satellites</small><br><big><b>"+gps.satellitesVisible()+"</b></big>", Html.FROM_HTML_MODE_COMPACT));
        });
        mViewModel.getBatVol().observe(getViewLifecycleOwner(), vol->{
            TextView tv = requireActivity().findViewById(R.id.bat_status);
            tv.setText(Html.fromHtml(String.format("<small>Battery</small><br><big><b>%.1f</b></big><small>v</small>", vol), Html.FROM_HTML_MODE_COMPACT));
        });
        requireActivity().findViewById(R.id.flight_mode).setOnClickListener(v->{
            String[] modes = mViewModel.getModes();
            if (modes != null) {
                PopupMenu popup = new PopupMenu(requireActivity(), v);
                Menu menu = popup.getMenu();
                for (String mode : modes) {
                    if (!mode.isEmpty()) menu.add(mode);
                }
                popup.setOnMenuItemClickListener(menuItem->{
                    mViewModel.setDstMode(menuItem.getTitle().toString());
                    return true;
                });
                popup.show();
            }
        });
    }
}