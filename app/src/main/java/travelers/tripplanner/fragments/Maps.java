package travelers.tripplanner.fragments;

import android.Manifest;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;

import travelers.tripplanner.MainActivity;
import travelers.tripplanner.R;

public class Maps extends Fragment {
    MapView mMapView;
    private GoogleMap googleMap;
    ArrayList<place> place_list = new ArrayList<>();
    ArrayList<LatLng> latLng_list = new ArrayList<>();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View mView = inflater.inflate(R.layout.fragment_maps, container, false);

        mMapView = mView.findViewById(R.id.mapView);
        mMapView.onCreate(savedInstanceState);

        mMapView.onResume();

        try {
            MapsInitializer.initialize(getActivity().getApplicationContext());
        } catch (Exception e) {
            e.printStackTrace();
        }

        mMapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap mMap) {
                googleMap = mMap;

                // For showing a move to my location button
                if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    // TODO: Consider calling
                    return;
                }
                googleMap.setMyLocationEnabled(true);

                // For dropping a marker at a point on the Map
                LatLng user_location = new LatLng(MainActivity.latitude, MainActivity.longitude);
                googleMap.addMarker(new MarkerOptions().position(user_location).
                        title(getString(R.string.User_Location)).
                        snippet(getString(R.string.this_is_where_you_are))
                        .icon(bitmapDescriptorFromVector(getActivity(), R.drawable.ic_user_loc)));

                DatabaseReference mRootRef = FirebaseDatabase.getInstance().getReference();
                DatabaseReference mUserIdRef = mRootRef.child(FirebaseAuth.getInstance().getCurrentUser().getUid());

                DatabaseReference mBucketList = mUserIdRef.child(getString(R.string.BucketList));
                mBucketList.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(DataSnapshot dataSnapshot) {
                        for (DataSnapshot tripSnap: dataSnapshot.getChildren()) {
                            for(DataSnapshot placeSnap: tripSnap.getChildren()){
                                String tempName = new String();
                                String tempAddress = new String();
                                Double tempLat = new Double(0);
                                Double tempLng = new Double(0);
                                Boolean tempVisited = false;
                                for(DataSnapshot placeAttributeSnap: placeSnap.getChildren()){
                                    if(placeAttributeSnap.getKey().equals(getString(R.string.Name))) tempName = placeAttributeSnap.getValue(String.class);
                                    if(placeAttributeSnap.getKey().equals(getString(R.string.Address))) tempAddress = placeAttributeSnap.getValue(String.class);
                                    if(placeAttributeSnap.getKey().equals(getString(R.string.Latitude))) tempLat = placeAttributeSnap.getValue(Double.class);
                                    if(placeAttributeSnap.getKey().equals(getString(R.string.Longitude))) tempLng = placeAttributeSnap.getValue(Double.class);
                                    if(placeAttributeSnap.getKey().equals(getString(R.string.Visited))) tempVisited = placeAttributeSnap.getValue(Boolean.class);
                                }
                                place_list.add(new place(tempName, tempAddress, tempLat, tempLng, tempVisited));
                            }
                        }

                        for(int i = 0; i < place_list.size(); i++){
                            latLng_list.add(new LatLng(place_list.get(i).getLat(), place_list.get(i).getLng()));
                                if(place_list.get(i).getVisited()){
                                    googleMap.addMarker(new MarkerOptions().position(latLng_list.get(i)).
                                            title(place_list.get(i).getName()).
                                            snippet(place_list.get(i).getAddress())
                                            .icon(bitmapDescriptorFromVector(getActivity(), R.drawable.ic_visited)));
                                } else {
                                    googleMap.addMarker(new MarkerOptions().position(latLng_list.get(i)).
                                            title(place_list.get(i).getName()).
                                            snippet(place_list.get(i).getAddress())
                                            .icon(bitmapDescriptorFromVector(getActivity(), R.drawable.ic_visit)));
                                }
                        }

                    }

                    @Override
                    public void onCancelled(DatabaseError databaseError) {

                    }
                });

                // For zooming automatically to the location of the marker
                CameraPosition cameraPosition = new CameraPosition.Builder().target(user_location).zoom(12).build();
                googleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            }
        });

        return mView;
    }

    private BitmapDescriptor bitmapDescriptorFromVector(Context context, int vectorResId) {
        Drawable vectorDrawable = ContextCompat.getDrawable(context, vectorResId);
        vectorDrawable.setBounds(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        Bitmap bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bitmap);
    }

    @Override
    public void onResume() {
        super.onResume();
        mMapView.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        mMapView.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mMapView.onDestroy();
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        mMapView.onLowMemory();
    }

    private class place {
        String name, address;
        Double lat, lng;
        Boolean visited;
        place(String name, String address, double lat, double lng, boolean b) {
            this.name = name;
            this.address = address;
            this.lat = lat;
            this.lng = lng;
            this.visited = b;
        }

        public String getName() {
            return name;
        }

        public String getAddress() {
            return address;
        }

        public Double getLat() {
            return lat;
        }

        public Double getLng() {
            return lng;
        }

        public Boolean getVisited() {
            return visited;
        }
    }
}
