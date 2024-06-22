package com.chobitsfan.minigcs;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class StatusFragment extends Fragment {

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

        mViewModel.getFlightMode().observe(getViewLifecycleOwner(), mode-> {
            TextView tv = requireActivity().findViewById(R.id.flight_mode);
            tv.setText(mode);
        });
        mViewModel.getStatusTxt().observe(getViewLifecycleOwner(), txt->{
            TextView tv = requireActivity().findViewById(R.id.status_txt);
            tv.setText(txt);
        });
        mViewModel.getGlobalPos().observe(getViewLifecycleOwner(), pos -> {
            TextView tv = requireActivity().findViewById(R.id.msl_alt);
            tv.setText(Integer.toString(pos.alt()));
        });
    }

}