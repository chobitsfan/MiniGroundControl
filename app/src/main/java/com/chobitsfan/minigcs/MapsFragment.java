package com.chobitsfan.minigcs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MapsFragment extends Fragment implements OnMapReadyCallback {

    //private OnMapReadyCallback callback = new OnMapReadyCallback() {
        /**
         * Manipulates the map once available.
         * This callback is triggered when the map is ready to be used.
         * This is where we can add markers or lines, add listeners or move the camera.
         * In this case, we just add a marker near Sydney, Australia.
         * If Google Play services is not installed on the device, the user will be prompted to
         * install it inside the SupportMapFragment. This method will only be triggered once the
         * user has installed Google Play services and returned to the app.
         */
        /*@Override
        public void onMapReady(GoogleMap googleMap) {
            LatLng sydney = new LatLng(-34, 151);
            googleMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
        }*/
    //};
    StatusViewModel mViewModel;
    GoogleMap googleMap = null;
    Marker droneMarker = null;
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_maps, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        mViewModel = new ViewModelProvider(requireActivity()).get(StatusViewModel.class);
        mViewModel.getGlobalPos().observe(getViewLifecycleOwner(), pos->{
            if (googleMap != null) {
                int lat = pos.lat();
                int lon = pos.lon();
                if (lat > 0 &&  lon > 0) {
                    LatLng droneLatlng = new LatLng(lat * 1e-7, lon * 1e-7);
                    if (droneMarker == null) {
                        droneMarker = googleMap.addMarker(new MarkerOptions().position(droneLatlng).icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_arrow)));
                        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(droneLatlng, 18));
                    } else {
                        droneMarker.setPosition(droneLatlng);
                        googleMap.moveCamera(CameraUpdateFactory.newLatLng(droneLatlng));
                    }
                }
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        googleMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
        googleMap.moveCamera(CameraUpdateFactory.zoomTo(18));
        droneMarker = googleMap.addMarker(new MarkerOptions().position(new LatLng(24.7741608,121.044659)).icon(BitmapDescriptorFactory.fromResource(R.drawable.drone_arrow)));
        this.googleMap = googleMap;
    }
}