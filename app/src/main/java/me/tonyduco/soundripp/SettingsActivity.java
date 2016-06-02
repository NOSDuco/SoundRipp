package me.tonyduco.soundripp;


import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

public class SettingsActivity extends PreferenceActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction().replace(android.R.id.content, new SettingsFragment()).commit();

        setContentView(R.layout.fragment_settings);

    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

       // LinearLayout root = (LinearLayout)findViewById(android.R.id.list).getParent().getParent().getParent();
       // Toolbar bar = (Toolbar) LayoutInflater.from(this).inflate(R.layout.settings_toolbar, root, false);
      //  root.addView(bar, 0); // insert at top
      //  bar.setNavigationOnClickListener(new View.OnClickListener() {
       //     @Override
       //     public void onClick(View v) {
       //         finish();
       //     }
      //  });
    }

}