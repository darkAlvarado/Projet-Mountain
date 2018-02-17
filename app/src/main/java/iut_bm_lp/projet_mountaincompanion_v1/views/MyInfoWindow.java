package iut_bm_lp.projet_mountaincompanion_v1.views;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.Marker;

import iut_bm_lp.projet_mountaincompanion_v1.R;

/**
 * Created by Zekri on 06/12/2017.
 */

public class MyInfoWindow implements GoogleMap.InfoWindowAdapter {

    private Context context;

    public MyInfoWindow(Context context) {

        this.context = context;
    }

    @Override
    public View getInfoWindow(Marker marker) {
        return null;
    }

    @Override
    public View getInfoContents(Marker marker) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View v = inflater.inflate(R.layout.windowmarker, null);

        TextView mTitle = (TextView) v.findViewById(R.id.title_info);
        TextView mAltitude_Distance = (TextView) v.findViewById(R.id.altitude_distance_info);


        mTitle.setText(marker.getTitle());
        mAltitude_Distance.setText(marker.getSnippet());
        return v;
    }
}
