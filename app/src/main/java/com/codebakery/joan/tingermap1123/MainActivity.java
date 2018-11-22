package com.codebakery.joan.tingermap1123;

import android.content.Context;
import android.graphics.PointF;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.UiThread;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.recyclerview.extensions.ListAdapter;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.naver.maps.geometry.LatLng;
import com.naver.maps.map.MapFragment;
import com.naver.maps.map.NaverMap;
import com.naver.maps.map.Pickable;
import com.naver.maps.map.Symbol;
import com.naver.maps.map.overlay.Align;
import com.naver.maps.map.overlay.Marker;
import com.naver.maps.map.overlay.Overlay;
import com.naver.maps.map.util.MarkerIcons;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    NaverMap naverMap = null;
    static int cnt = 0;

    private ArrayList<Marker> markerList = null;
    private Marker marker = null;

    Adapter adapter = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        markerList = new ArrayList<>();

        ((Button)findViewById(R.id.button)).setOnClickListener(this::onClick);

        MapFragment mapFragment = (MapFragment)getSupportFragmentManager().findFragmentById(R.id.map_fragment);
        if (mapFragment == null) {
            mapFragment = MapFragment.newInstance();
            getSupportFragmentManager().beginTransaction().add(R.id.map_fragment, mapFragment).commit();
        }
        mapFragment.getMapAsync(this::onMapReady);
        adapter = new Adapter();
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);
    }

    /*
    * 검색버튼 클릭
    * */
    private void onClick(View view) {
        switch(view.getId()){
            case R.id.button :
                LatLng latLng = searchMeetupSpot();
                PointF point = new PointF((float)latLng.latitude,(float)latLng.longitude);

                View circle = findViewById(R.id.circle);

                int radius = getResources().getDimensionPixelSize(R.dimen.pick_radius);
                circle.setX(point.x - radius);
                circle.setY(point.y - radius);
                circle.setVisibility(View.VISIBLE);
                adapter.submitList(naverMap.pickAll(point, radius));
                break;
        }
    }

    /*
    * 중심점 찾아주는 메서드
    * */
    private LatLng searchMeetupSpot() {

        double minX=0.0, maxX=0.0, minY=0.0, maxY=0.0;

        double[][] markerArray = null;
        if (markerList!=null){
            markerArray = new double[markerList.size()][2];
            for(int i=0 ; i<markerList.size() ; i++){
                markerArray[i][0] = markerList.get(i).getPosition().latitude; // X,위도
                markerArray[i][1] = markerList.get(i).getPosition().longitude; // Y,경도
            }
        }

        if (markerArray.length > 0){
            for(int i=0; i< markerArray.length; i++){
                minX = (markerArray[i][0] < minX || minX == 0) ? markerArray[i][0] : minX;
                maxX = (markerArray[i][0] > maxX || maxX == 0) ? markerArray[i][0] : maxX;
                minY = (markerArray[i][1] < minY || minY == 0) ? markerArray[i][1] : minY;
                maxY = (markerArray[i][1] > maxY || maxY == 0) ? markerArray[i][1] : maxY;
            }
        }

        return new LatLng((minX + maxX) /2, (minY + maxY) /2);
       // return new LatLng(minX+((maxX-minX)/2), minY+((maxY-minY)/2));
    }

    @UiThread
    //@Override
    // OnMapReadyCallback
    public void onMapReady(@NonNull NaverMap map) {
        this.naverMap = map;
        naverMap.setOnMapClickListener(this::onMapClick);

        /*Adapter adapter = new Adapter();

        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setAdapter(adapter);

        View circle = findViewById(R.id.circle);

        int radius = getResources().getDimensionPixelSize(R.dimen.pick_radius);

        naverMap.setOnMapClickListener((point, coord) -> {
            circle.setX(point.x - radius);
            circle.setY(point.y - radius);
            circle.setVisibility(View.VISIBLE);
            adapter.submitList(naverMap.pickAll(point, radius));
        });

        naverMap.addOnCameraChangeListener((reason, animated) -> {
            circle.setVisibility(View.GONE);
            adapter.submitList(Collections.emptyList());
        });*/
    }

    private void onMapClick(PointF pointF, LatLng latLng) {
        String name = "ATTENDEE" + ++cnt;

        marker = new Marker(MarkerIcons.PINK);
        marker.setPosition(latLng);
        marker.setCaptionAlign(Align.Top);
        marker.setCaptionText(name);
        marker.setMap(naverMap);

        markerList.add(marker);

        marker.setOnClickListener(this::onClick);
    }

    /*
    * 지도 클릭
    * 마커 표시
    * */
    private boolean onClick(Overlay overlay) {
        marker = (Marker)overlay;
        marker.setMap(null);
        markerList.remove(marker);
        for (Marker m :markerList) {
            Log.e("/marker : ",m.getCaptionText() + " / "+ m.getPosition());
        }
        return true;
    }





    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView text;

        public ViewHolder(View itemView) {
            super(itemView);
            text = (TextView)itemView;
        }

        public void setItem(@NonNull Pickable item) {
            Context context = itemView.getContext();

            if (item instanceof Symbol) {
                text.setText(context.getString(R.string.format_pickable_symbol, ((Symbol)item).getCaption()));
            } else if (item instanceof Marker) {
                text.setText(
                        context.getString(R.string.format_pickable_marker, ((Marker)item).getCaptionText()));
            } else {
                text.setText(context.getString(R.string.pickable_overlay));
            }
        }
    }



    private static class Adapter extends ListAdapter<Pickable, ViewHolder> {
        private Adapter() {
            super(new DiffUtil.ItemCallback<Pickable>() {
                @Override
                public boolean areItemsTheSame(Pickable oldItem, Pickable newItem) {
                    return oldItem.equals(newItem);
                }

                @Override
                public boolean areContentsTheSame(Pickable oldItem, Pickable newItem) {
                    return oldItem.equals(newItem);
                }
            });
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(
                    LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pickable, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.setItem(getItem(position));
        }
    }
}
