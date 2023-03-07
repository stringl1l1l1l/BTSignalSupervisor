package com.example.signalsupervisor3.ui.activities;

import android.Manifest;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.example.signalsupervisor3.R;
import com.example.signalsupervisor3.bluetooth.BluetoothController;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.signalsupervisor3.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private ActivityMainBinding binding;
    private BluetoothController mBluetoothController;

    private void checkBTPermission() {
        Log.d(TAG, "checkBTPermission: Start");
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            int permissionCheck = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                // 有一个权限没有，permission就-1
                permissionCheck = this.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION);
                permissionCheck += this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
                permissionCheck += this.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT);
                permissionCheck += this.checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN);
                if (permissionCheck != 0) {
                    this.requestPermissions(new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.BLUETOOTH_CONNECT,
                            Manifest.permission.BLUETOOTH_SCAN
                    }, 1001); //any number
                } else {
                    Log.d(TAG, "checkBTPermissions: No need to check permissions. SDK version < LOLLIPOP.");
                }
            }
        }
        Log.d(TAG, "checkBTPermission: Finish");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        BottomNavigationView navView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_home, R.id.navigation_dashboard).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
        checkBTPermission();
    }

}