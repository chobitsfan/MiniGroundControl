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
import android.widget.Toast;

public class SetupFragment extends Fragment {

    private StatusViewModel mViewModel;

    public static SetupFragment newInstance() {
        return new SetupFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_setup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(StatusViewModel.class);
        mViewModel.getParamValue().observe(requireActivity(), param->{
            TextView tv = requireActivity().findViewById(R.id.param_name);
            tv.setText(param.first);
            tv = requireActivity().findViewById(R.id.param_val);
            tv.setText(Float.toString(param.second));
        });
        requireActivity().findViewById(R.id.read_param_btn).setOnClickListener(v->{
            TextView tv = requireActivity().findViewById(R.id.param_name);
            String name = tv.getText().toString();
            if (!name.isEmpty()) {
                mViewModel.setParamRead(name);
            }
        });
    }
}