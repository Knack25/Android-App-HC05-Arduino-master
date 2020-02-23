package com.example.danyal.bluetoothhc05;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Formatter;
import java.util.Locale;
import java.util.UUID;

public class Main_Activity extends AppCompatActivity implements LocationListener {


    /********************************Initialize Vars**********************************/
    Button btnDis;
    String address = null;

    Switch unitSwitch;

    SeekBar seekBar;
    TextView throttle, speed;


    int min = 0, max = 10, current;

    double Latitude, Longitude;


    private ProgressDialog progress;
    public LocationManager locationManager;

    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");




    /********************************On Create**********************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


/********************************Layout Map**********************************/

        Intent newint = getIntent();
        address = newint.getStringExtra(DeviceList.EXTRA_ADDRESS);

        setContentView(R.layout.activity_main);


        btnDis = (Button) findViewById(R.id.button4);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        throttle = (TextView) findViewById(R.id.throttle);
        unitSwitch = (Switch) findViewById(R.id.unitSwitch);
        speed = (TextView) findViewById(R.id.speed);


        /********************************Throttle INIT**********************************/

        seekBar.setProgress(max - min);

        seekBar.setProgress(current - min);

        throttle.setText("" + current);




        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {


            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                current = progress + min;
                throttle.setText("" + current);
                sendSignal(current);
                Log.d("Output", String.valueOf(current));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }


        });



        /********************************Bluetooth INIT**********************************/
        new ConnectBT().execute();






        btnDis.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick (View v) {
                Disconnect();
            }
        });



        /********************************GPS INIT**********************************/


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
        != PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},1000);
        } else{
            serviceStart();

        }

        this.updateSpeed(null);

        unitSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                Main_Activity.this.updateSpeed(null);
            }
        });


    }


    private void serviceStart(){

        LocationManager locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        if(locationManager == null){
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0 , 0, this);
        }
        Toast.makeText(this,"Waiting for GPS Connection",Toast.LENGTH_SHORT).show();
    }


    private void updateSpeed(CLocation location){
        float nCurrentSpeed = 0;

        if (location != null){
            location.setbUseMetricUnits(this.useMetricUnits());
            nCurrentSpeed = location.getSpeed();
        }
        Formatter formatter = new Formatter(new StringBuilder());
        formatter.format(Locale.US,"%5.1f", nCurrentSpeed);
        String strCurrSpeed  = formatter.toString();
        strCurrSpeed = strCurrSpeed.replace("","0");

        if (this.useMetricUnits()){
            speed.setText(strCurrSpeed + " kmh");
        } else {
            speed.setText(strCurrSpeed + " mph");
        }
    }

    protected boolean useMetricUnits(){
        return unitSwitch.isChecked();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        if (requestCode == 1000){
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                serviceStart();
            } else {
                finish();
            }
        }

    }



    private void sendSignal ( int number ) {
        if ( btSocket != null ) {
            try {
                btSocket.getOutputStream().write(number);
            } catch (IOException e) {
                msg("Error");
            }
        }
    }

    private void Disconnect () {
        if ( btSocket!=null ) {
            try {
                btSocket.close();
            } catch(IOException e) {
                msg("Error");
            }
        }

        finish();
    }


    private void msg (String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

    /********************************GPS/Speedometer**********************************/
    @Override
    public void onLocationChanged(Location location) {
        if (location != null){
            CLocation myLocation = new CLocation(location,this.useMetricUnits());
            this.updateSpeed(myLocation);
        }
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }



    /********************************BT Methods**********************************/
    private class ConnectBT extends AsyncTask<Void, Void, Void> {
        private boolean ConnectSuccess = true;

        @Override
        protected  void onPreExecute () {
            progress = ProgressDialog.show(Main_Activity.this, "Connecting...", "Please Wait!!!");
        }

        @Override
        protected Void doInBackground (Void... devices) {
            try {
                if ( btSocket==null || !isBtConnected ) {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                }
            } catch (IOException e) {
                ConnectSuccess = false;
            }

            return null;
        }

        @Override
        protected void onPostExecute (Void result) {
            super.onPostExecute(result);

            if (!ConnectSuccess) {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            } else {
                msg("Connected");
                isBtConnected = true;
            }

            progress.dismiss();
        }
    }
}
