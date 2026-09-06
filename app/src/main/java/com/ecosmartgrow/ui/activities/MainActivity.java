package com.ecosmartgrow.ui.activities;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.ecosmartgrow.R;
import com.ecosmartgrow.ui.fragments.CameraFragment;
import com.ecosmartgrow.ui.fragments.DashboardFragment;
import com.ecosmartgrow.ui.fragments.HarvestFragment;
import com.ecosmartgrow.ui.fragments.LogsFragment;
import com.ecosmartgrow.ui.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity implements BottomNavigationView.OnNavigationItemSelectedListener {
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnNavigationItemSelectedListener(this);

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new DashboardFragment());
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        Fragment fragment = null;

        int id = item.getItemId();
        if (id == R.id.nav_home) {
            fragment = new DashboardFragment();
        } else if (id == R.id.nav_logs) {
            fragment = new LogsFragment();
        } else if (id == R.id.nav_camera) {
            fragment = new CameraFragment();
        } else if (id == R.id.nav_harvest) {
            fragment = new HarvestFragment();
        } else if (id == R.id.nav_settings) {
            fragment = new SettingsFragment();
        }

        return loadFragment(fragment);
    }

    private boolean loadFragment(Fragment fragment) {
        if (fragment != null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .commit();
            return true;
        }
        return false;
    }
}