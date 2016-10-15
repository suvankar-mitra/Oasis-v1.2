package net.net16.suvankar.oasis_v12;


import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.graphics.ColorUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;


/**
 * A simple {@link Fragment} subclass.
 */
public class NavigationDrawerFragment extends Fragment {


    public NavigationDrawerFragment() {
        // Required empty public constructor
    }

    LinearLayout empty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_navigation_drawer, container, false);

        empty = (LinearLayout) v.findViewById(R.id.empty_nav);
        empty.setBackgroundColor(Color.parseColor("#7b100f0f"));
        empty.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                empty.setBackgroundColor(Color.TRANSPARENT);
                getFragmentManager().beginTransaction().setCustomAnimations(R.anim.frag_enter_from_right,R.anim.frag_exit_to_left,
                        R.anim.frag_enter_from_left,R.anim.frag_exit_to_right).remove(NavigationDrawerFragment.this).commit();
                getFragmentManager().popBackStack();
            }
        });

        return v;
    }

}
