package eu.gpatsiaouras.ledstripcontrol;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;
import android.view.DragEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerView;
import com.flask.colorpicker.OnColorChangedListener;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity implements
        SeekBar.OnSeekBarChangeListener, SharedPreferences.OnSharedPreferenceChangeListener,
        View.OnClickListener {

    SeekBar seekBarRed;
    SeekBar seekBarGreen;
    SeekBar seekBarBlue;
    //Buttons
    Button mBtnTurnOff;
    Button mBtnWhite;
    // Data
    String mIp;
    int mPort;

    Context mContext;

    Thread serverThread;
    UdpServerThread udpServerThread;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        seekBarRed = (SeekBar) findViewById(R.id.sb_red);
        seekBarGreen = (SeekBar) findViewById(R.id.sb_green);
        seekBarBlue = (SeekBar) findViewById(R.id.sb_blue);

        seekBarRed.setOnSeekBarChangeListener(this);
        seekBarGreen.setOnSeekBarChangeListener(this);
        seekBarBlue.setOnSeekBarChangeListener(this);

        mBtnTurnOff = (Button) findViewById(R.id.quick_turn_off);
        mBtnWhite = (Button) findViewById(R.id.quick_turn_white);

        mBtnTurnOff.setOnClickListener(this);
        mBtnWhite.setOnClickListener(this);

        mContext = this;

        setupValuesFromSharedPreferences();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("ONCREATE", "Creating new Server Thread");
        udpServerThread = new UdpServerThread();
        serverThread = new Thread(udpServerThread);
        serverThread.start();
    }

    @Override
    protected void onResume() {
        super.onResume();

        new SendUdpPackage().execute("status");
    }

    @Override
    protected void onStop() {
        super.onStop();
        udpServerThread.stop();
        serverThread = null;
    }

    private void setupValuesFromSharedPreferences() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        mIp = sharedPreferences.getString(getResources().getString(R.string.pref_ip_key), "");
        try {
            mPort = Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.pref_port_key), "0"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
        if (mPort == 0 || mIp == null) {
            Intent intent = new Intent(this, SettingsActivity.class);
            intent.putExtra("message", getResources().getString(R.string.first_time_settings));
            startActivity(intent);
        }
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        int red = seekBarRed.getProgress();
        int green = seekBarGreen.getProgress();
        int blue = seekBarBlue.getProgress();

        String msgString = String.valueOf(red)+"-"+String.valueOf(green)+"-"+String.valueOf(blue);
        new SendUdpPackage().execute(msgString);
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        mIp = sharedPreferences.getString(getResources().getString(R.string.pref_ip_key), "");
        try {
            mPort = Integer.parseInt(sharedPreferences.getString(getResources().getString(R.string.pref_port_key), "0"));
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.quick_turn_off) {
            seekBarRed.setProgress(0);
            seekBarGreen.setProgress(0);
            seekBarBlue.setProgress(0);
        } else if (v.getId() == R.id.quick_turn_white) {
            seekBarRed.setProgress(255);
            seekBarGreen.setProgress(255);
            seekBarBlue.setProgress(255);
        }
    }

    private class SendUdpPackage extends AsyncTask<String, String, Void> {
        @Override
        protected Void doInBackground(String... params) {
            String messageStr = params[0];
            try {
                DatagramSocket s = new DatagramSocket();
                InetAddress local = InetAddress.getByName(mIp);
                int msg_length = messageStr.length();
                byte[] message = messageStr.getBytes();
                DatagramPacket p = new DatagramPacket(message, msg_length, local, mPort);
                s.send(p);
                s.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }

    }

    private class UdpServerThread implements Runnable{
        DatagramSocket datagramSocket;

        public void stop() {
            datagramSocket.close();
        }
        @Override
        public void run() {
            try {
                Log.d("SocketServer","Server started");
                byte[] buffer = new byte[255];
                if (datagramSocket == null)
                    datagramSocket = new DatagramSocket(mPort);
                DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
                while (true) {
                    datagramSocket.receive(receivedPacket);
                    String text = new String(receivedPacket.getData(), 0, receivedPacket.getLength());
                    System.out.println("Packet Received is: "+text);
                    updateSeekbars(text);
                }

            } catch (IOException e) {
                Log.d("SocketServer","Server stopped");
                e.printStackTrace();
            } finally {
                if (datagramSocket != null)
                    datagramSocket.close();
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int selectedItem = item.getItemId();
        if (selectedItem == R.id.item_settings) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }
        return true;
    }

    public void updateSeekbars(String rgbData) {
        if (rgbData == "")
            return;
        Log.d("SEEKBARS", rgbData);
        String[] rgb = rgbData.split("-");
        seekBarRed.setProgress(Integer.parseInt(rgb[0]));
        seekBarGreen.setProgress(Integer.parseInt(rgb[1]));
        seekBarBlue.setProgress(Integer.parseInt(rgb[2]));
    }
}
