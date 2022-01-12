package com.bodekjan.usb;

//api help form https://stackoverflow.com/questions/13696620/google-maps-android-api-v2-authorization-failure
//copied and modified from https://github.com/kripthor/teensy31-micloc-java and the arduino teensy code from https://github.com/kripthor/teensy31-micloc

import static java.nio.charset.StandardCharsets.UTF_8;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.PointF;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.text.Editable;
import android.text.NoCopySpan;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements NoCopySpan {
    public final String ACTION_USB_PERMISSION = "com.bodekjan.usb.USB_PERMISSION";
    Button startButton, sendButton, clearButton, stopButton;
    TextView textView;
    EditText editText;
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;
    Context context = this;
    private String memberFieldString;
    Intent intent;
    String data;
    View view;
    ArrayList<String> array = new ArrayList<String>();
    Heavylifting hl;



    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        //Defining a Callback which triggers whenever data is read.
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            try {
                data = new String(arg0, "UTF-8");


                data.concat("/n");
                tvAppend(textView, data);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    };





    private final  BroadcastReceiver broadcastReceiver = new BroadcastReceiver() { //Broadcast Receiver to automatically start and stop the Serial connection.
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);
                if (granted) {
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            setUiEnabled(true);
                            serialPort.setBaudRate(9600);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvAppend(textView,"Serial Connection Opened!\n");




                        } else {
                            Log.d("SERIAL", "PORT NOT OPEN");
                        }
                    } else {
                        Log.d("SERIAL", "PORT IS NULL");
                    }
                } else {
                    Log.d("SERIAL", "PERM NOT GRANTED");
                }
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                onClickStart(startButton);
            } else if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
                onClickStop(stopButton);

            }
        }

    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        usbManager = (UsbManager) getSystemService(USB_SERVICE);
        startButton = findViewById(R.id.buttonStart);
        sendButton = findViewById(R.id.buttonSend);
        clearButton = findViewById(R.id.buttonClear);
        stopButton = findViewById(R.id.buttonStop);
        editText = findViewById(R.id.editText);
        textView = findViewById(R.id.textView);
        setUiEnabled(false);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
        hl= new Heavylifting();
        textView.setText("p: to get temp data hit a few time until you get some numbers\ns: sound buffer data \n +: increase sound threshold by 5 \n -: decrease sound threshold by 5\n");
        textView.addTextChangedListener(new TextWatcher() {

            public void afterTextChanged(Editable s) {

                String location = GetLocandandBearing();
                array.add(location);
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });
    }




    public void setUiEnabled(boolean bool) {
        startButton.setEnabled(!bool);
        sendButton.setEnabled(bool);
        stopButton.setEnabled(bool);
        textView.setEnabled(bool);

    }

    public void onClickStart(View view) {




        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x16C0 )//Teensy Vendor ID found https://community.appinventor.mit.edu/t/serial-usb-hardware-detection-fails-to-see-teensy-or-uno-please-update-device-filter-xml/15000
                {
                    PendingIntent pi = PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), 0);
                    usbManager.requestPermission(device, pi);
                    keep = false;


                } else {
                    connection = null;
                    device = null;
                }

                if (!keep)
                    break;
            }
        }


    }

    public void onClickSend(View view) {

        String string = editText.getText().toString();
        serialPort.write(string.getBytes());
        // tvAppend(textView, "\nData Sent : " + string + "\n");

    }

    public void onClickStop(View view) {
        setUiEnabled(false);
        serialPort.close();
        tvAppend(textView,"\nSerial Connection Closed! \n");

    }




    @SuppressLint("MissingPermission")
    public void onClickClear(View view) {
        textView.setText(" ");
//        byte[] b = new byte[0];
//
//        mCallback.onReceivedData(b);
    }



    private void tvAppend(TextView tv, CharSequence text) {
        final TextView ftv = tv;
        final CharSequence ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.append(ftext);
            }
        });
    }




    @SuppressLint("MissingPermission")
    public void onClickValid(View view) {


        data = textView.getText().toString();
        if (data != null || data != " ") {
            int duration = Toast.LENGTH_SHORT;
            Toast.makeText(context, "You got data:", duration).show();


            String[] divide = new String[0];


            //Splitting off the temperature data
            divide = data.split(":");
            //  tvAppend(textView, divide[0]);

            // MicLocTeensy31 micloc = new MicLocTeensy31();

            ArrayList<MicInfo> mics = new ArrayList<MicInfo>();
            MicInfo mic1 = new MicInfo();
            MicInfo mic2 = new MicInfo();
            MicInfo mic3 = new MicInfo();
            MicInfo mic4 = new MicInfo();

            //SETUP THE MIC COORDINATES
            mic1.location = new PointF(0, 0);
            mic2.location = new PointF(0, 0.0239f);
            mic3.location = new PointF(0.0239f, 0.0239f);
            mic4.location = new PointF(0.0239f, 0);

            mics.add(mic1);
            mics.add(mic2);
            mics.add(mic3);
            mics.add(mic4);

            double soundSpeed;

            LocationTable table = null;
            int scale = 1;





            String tempData, micData, bufferData;
            int nSamples, timeSamplesUSec, threshold;
            double samplesPerUsec, temperature, humidity;

            // while (true) {
            try {

                String[] fields;

                // PARSE SAMPLE INFO AND TEMPERATURE DATA

                tempData = divide[0];


                fields = tempData.split(",");
                timeSamplesUSec = Integer.parseInt(fields[1]);
                // Toast.makeText(this, "You got timeSamplesUSec:" + timeSamplesUSec, duration).show();
                nSamples = Integer.parseInt(fields[3]);
                //Toast.makeText(this, "You got nSamples:" + nSamples, duration).show();
                samplesPerUsec = Double.parseDouble(fields[5]);
                ///Toast.makeText(this, "You got samplesPerUsec:" + samplesPerUsec, duration).show();

                temperature = Double.parseDouble(fields[7]);
                //Toast.makeText(this, "You got temperature:" + temperature, duration).show();
                humidity = Double.parseDouble(fields[9]);
                //Toast.makeText(this, "You got humidity:" + humidity, duration).show();
                threshold = Integer.parseInt(fields[11]);
                Toast.makeText(this, " threshold" +threshold, duration).show();

                //CALC SOUND SPEED
                soundSpeed = SoundUtils.soundSpeed(temperature, humidity);
                System.out.println(" SoundSpeed: " + soundSpeed);
                Toast.makeText(this, "parsed temp threshold" +threshold, duration).show();

                //PARSE BUFFER DATA

                bufferData = divide[1];


                Toast.makeText(this, "parsed buffer", duration).show();

                //  tvAppend(textView, data);

                ////////////////////////////////////////////////////////////////////////////////
                /////////////////////////////////////////////////////////////////////////////////

                //PARSE SOUND BUFFER DATA

               

                System.out.println("|bufferData:|\t" + bufferData);
                System.out.flush();
                Log.i("Here", "bufferData:" + bufferData);
                //PARSE SOUND BUFFER DATA
                fields = bufferData.split(",");

                int dsize = Integer.parseInt(fields[1]);
                int event = Integer.parseInt(fields[3]) - (nSamples - dsize);
                Toast.makeText(context, "parsed buffer", duration).show();
                int[] data1 = new int[dsize];
                int[] data2 = new int[dsize];
                int[] data3 = new int[dsize];
                int[] data4 = new int[dsize];


                String mic = divide[2];
                //String mic = fields[5];
                fields = mic.split(" ");
                System.out.println("Read " + fields.length);
                readData(fields, data1, nSamples - dsize);

                //mic = micloc.serialIn.readLine();
                mic = divide[3];
                fields = mic.split(" ");
                System.out.println("Read " + fields.length);
                readData(fields, data2, nSamples - dsize);

                //mic = micloc.serialIn.readLine();
                mic = divide[4];
                fields = mic.split(" ");
                System.out.println("Read " + fields.length);
                readData(fields, data3, nSamples - dsize);

                //mic = micloc.serialIn.readLine();
                mic = divide[5];
                fields = mic.split(" ");
                System.out.println("Read " + fields.length);
                readData(fields, data4, nSamples - dsize);
                System.out.flush();


                //SIMPLE TEMP FILE PREFIX FOR IMAGES
                //fpc++;
                //fileprefix = "AA"+fpc;

                // SHOW RAW SIGNALS
                //createImage(fileprefix,data1, data2, data3, data4, event, 0, 0, 0, 0);

                // SIGNAL TREATMENT
                // NOISE CUTTING, NORMALIZATION AND WEIGHTING

                SoundUtils.normalize(data1);
                SoundUtils.normalize(data2);
                SoundUtils.normalize(data3);
                SoundUtils.normalize(data4);

                //CROSSCORRELATE THE SIGNALS FOR DTOA
                CrossCorrelation cc12 = new CrossCorrelation(data1, data2);
                CrossCorrelation cc13 = new CrossCorrelation(data1, data3);
                CrossCorrelation cc14 = new CrossCorrelation(data1, data4);

                System.out.println("\nPost normalization (MIC1 basis is sample 0)\nCC21@ " + cc12.maxindex);
                System.out.println("CC31@ " + cc13.maxindex);
                System.out.println("CC41@ " + cc14.maxindex);



                //LOCATE THE SOUND EVENT
                LocationTableEntry bestLte = new LocationTableEntry();
                bestLte = new LocationTableEntry();
                mic1.sample = 0;
                mic2.sample = cc12.maxindex;
                mic3.sample = cc13.maxindex;
                mic4.sample = cc14.maxindex;
                //TODO #'s from micloc witch is  a known answer
//            mic2.sample = 60;
//            mic3.sample = 360;
//            mic4.sample = 312;
                Log.i("Here", "cc12.maxindex:" + mic2.sample);
                Log.i("Here", "cc13.maxindex:" + mic3.sample);
                Log.i("Here", "cc14.maxindex:" + mic4.sample);

                for (int k = 0; k < mics.size(); k++) {
                    Log.i("Here", "mics:" + mics.get(k));
                }

                bestLte.calcTds(mics, samplesPerUsec);

//

//            Log.i("Here", "lte:"+bestLte);

//            Log.i("Here", " bestLte.calcTds" + bestLte.calcTds(mics, samplesPerUsec));

                //TO DO Perform validation on DTOAs
                //double maxdiff = (mic1.location.distance(mic3.location) / soundSpeed) * 1.01;

                boolean valid = true;
                if (valid) {

                    if (table == null) {
                        System.out.println("Generating table...");
                        Log.i("Here", "Got Here tabled generation");

                        table = new LocationTable(scale);
                        //setting size of table
                        //table.generateTable(mics, soundSpeed, -4000 / scale, -4000 / scale, 8000 / scale, 8000 / scale);
                        table.generateTable(mics, soundSpeed, -4 / scale, -4 / scale, 8 / scale, 8 / scale);

                        //System.out.println("Done. Tablesize: " + table.table.size());

                    }

//                            BufferedImage image = table.heatMap(bestLte, LocationTable.LOCMETHOD_2MICCROSSOVER, samplesPerUsec);

                    //System.out.println("Best probable location: "+table.bestLocation);
                    //Log.i("Here", "bestLte in main activity: " + bestLte);

                    table.getBestLocation(bestLte, LocationTable.LOCMETHOD_2MICCROSSOVER, samplesPerUsec);

                    // Objects.requireNonNull(table).getBestLocation(bestLte, LocationTable.LOCMETHOD_AVGALLERRORS, samplesPerUsec);

                    //long poop= table.getBestLocation(bestLte, LocationTable.LOCMETHOD_2MICCROSSOVER, samplesPerUsec);
                    Log.i("Here", "best location: " + table.bestLocation);
                    Log.i("Here", "bestLocationx: " + table.bestLocation.p.x);
                    Log.i("Here", "bestLocationy: " + table.bestLocation.p.y);


                    ////////////////////////////////////////////////////
                    //get your gps location and bearing

                    Location gps_loc = null;
                    Location network_loc = null;
                    Location final_loc;
                    String loc = array.get(0);
                    String[] cut;
                    cut =loc.split(",");
                    String lat = cut[1];
                    double latitude = Double.parseDouble(lat);
                    String lon = cut[3];
                    double longitude = Double.parseDouble(lon);
                    String bear = cut[5];
                    double bearing = Double.parseDouble(bear);


                    LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
                    MapsMarkerActivity map = new MapsMarkerActivity();

                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
                            && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {

                        return;
                    }

                    try {

                        gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                        network_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }

                    if (gps_loc != null) {
//                        final_loc = gps_loc;
//                        latitude = final_loc.getLatitude();
//                        longitude = final_loc.getLongitude();
//                        Log.i("Here", "your1 location lat: " + latitude + "long:" + longitude);
//                        //bearing = gps_loc.getBearing();
//                        //bearing = -57;
//                        Log.i("Here", "your1 bearing lat: " + bearing);



                        double pi = Math.PI;
                        //TODO use bearing to correctly calculate new lat/lon
                        double dx = table.bestLocation.p.x;
                        double dy = table.bestLocation.p.y;
//                        double dx = 400;
//                        double dy = 400;

// shifting the lat/lon by 'x' of meters to new lat long from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters

// number of km per degree = ~111km (111.32 in google maps, but range varies
                        // between 110.567km at the equator and 111.699km at the poles)
// 1km in degree = 1 / 111.32km = 0.0089
// 1m in degree = 0.0089 / 1000 = 0.0000089
                        double coefx = dx * 0.0000089;//gps to m
                        double coefy = dy * 0.0000089; //gps to m



//                        //rotational transformation of Cartesian coordinate from https://farside.ph.utexas.edu/teaching/celestial/Celestial/node122.html
                        double intermediatex = Math.cos(bearing) * coefx + Math.sin(bearing) * coefy;
                        double intermediatey = -1 * Math.sin(bearing) * coefx + Math.cos(bearing) * coefy;
//                        //double intermediatez=z;
//                        // shifting the lat/lon by 'x' of meters to new lat long from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
                        double new_latitude = intermediatey + latitude;

// pi / 180 = 0.018
                        double new_longitude = intermediatex + longitude / Math.cos(intermediatey * 0.018);
                        Log.i("Here", "your4 rotated location new lat: " + new_latitude + " new long:" + new_longitude);

                        Bundle b = new Bundle();
                        Intent mIntent = new Intent(this, MapsMarkerActivity.class);
                        b.putString("lat", String.valueOf(new_latitude));
                        b.putString("lon", String.valueOf(new_longitude));
                        mIntent.putExtras(b);
                        //mIntent.putExtras(b);
                        startActivity(mIntent);

                    } else if (network_loc != null) {
                        final_loc = network_loc;


                        double pi = Math.PI;
                        //TODO use bearing to correctly calculate new lat/lon
                        double dx = table.bestLocation.p.x;
                        double dy = table.bestLocation.p.y;
//                        double dx = 400;
//                        double dy = 400;

// shifting the lat/lon by 'x' of meters to new lat long from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters

// number of km per degree = ~111km (111.32 in google maps, but range varies
                        // between 110.567km at the equator and 111.699km at the poles)
// 1km in degree = 1 / 111.32km = 0.0089
// 1m in degree = 0.0089 / 1000 = 0.0000089
                        double coefx = dx * 0.0000089;//gps to m
                        double coefy = dy * 0.0000089; //gps to m



//                        //rotational transformation of Cartesian coordinate from https://farside.ph.utexas.edu/teaching/celestial/Celestial/node122.html
                        double intermediatex = Math.cos(bearing) * coefx + Math.sin(bearing) * coefy;
                        double intermediatey = -1 * Math.sin(bearing) * coefx + Math.cos(bearing) * coefy;
//                        //double intermediatez=z;
//                        // shifting the lat/lon by 'x' of meters to new lat long from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
                        double new_latitude = intermediatey + latitude;

// pi / 180 = 0.018
                        double new_longitude = intermediatex + longitude / Math.cos(intermediatey * 0.018);
                        Log.i("Here", "your4 rotated location new lat: " + new_latitude + " new long:" + new_longitude);

                        Bundle b = new Bundle();
                        Intent mIntent = new Intent(this, MapsMarkerActivity.class);
                        b.putString("lat", String.valueOf(new_latitude));
                        b.putString("lon", String.valueOf(new_longitude));
                        mIntent.putExtras(b);
                        //mIntent.putExtras(b);
                        startActivity(mIntent);

                    } else {
                        latitude = 0.0;
                        longitude = 0.0;
                        Log.i("Here", "your3 location lat: " + latitude + "long:" + longitude);
                        bearing = 0;
                        Log.i("Here", "your1 bearing lat: " + bearing);
                        double pi = Math.PI;
                        //TODO use bearing to correctly calculate new lat/lon
                        double dx = table.bestLocation.p.x;
                        double dy = table.bestLocation.p.y;
//                        double dx = 400;
//                        double dy = 400;

// shifting the lat/lon by 'x' of meters to new lat long from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters

// number of km per degree = ~111km (111.32 in google maps, but range varies
                        // between 110.567km at the equator and 111.699km at the poles)
// 1km in degree = 1 / 111.32km = 0.0089
// 1m in degree = 0.0089 / 1000 = 0.0000089
                        double coefx = dx * 0.0000089;//gps to m
                        double coefy = dy * 0.0000089; //gps to m



//                        //rotational transformation of Cartesian coordinate from https://farside.ph.utexas.edu/teaching/celestial/Celestial/node122.html
                        double intermediatex = Math.cos(bearing) * coefx + Math.sin(bearing) * coefy;
                        double intermediatey = -1 * Math.sin(bearing) * coefx + Math.cos(bearing) * coefy;
//                        //double intermediatez=z;
//                        // shifting the lat/lon by 'x' of meters to new lat long from https://stackoverflow.com/questions/7477003/calculating-new-longitude-latitude-from-old-n-meters
                        double new_latitude = intermediatey + latitude;

// pi / 180 = 0.018
                        double new_longitude = intermediatex + longitude / Math.cos(intermediatey * 0.018);
                        Log.i("Here", "your4 rotated location new lat: " + new_latitude + " new long:" + new_longitude);
                        Toast.makeText(context, "your4 rotated location new lat: " + new_latitude + " new long:" + new_longitude, duration).show();
                        Bundle b = new Bundle();
                        Intent mIntent = new Intent(this, MapsMarkerActivity.class);
                        b.putString("lat", String.valueOf(new_latitude));
                        b.putString("lon", String.valueOf(new_longitude));
                        mIntent.putExtras(b);
                        //mIntent.putExtras(b);
                        startActivity(mIntent);
                    }


                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_NETWORK_STATE}, 1);



                    Log.i("Here", "DONE: ");
                    int duration1 = Toast.LENGTH_SHORT;

                    Toast.makeText(context, "Done", duration1).show();

                } else {
                    System.out.println("Invalid sample.");
                }

            } catch (Exception e) {

                e.printStackTrace();
                // ERROR OCURRED, TRY TO RESYNC
                // THIS MUST BE SOLVED IN TEENSY AND HERE, NEEDS BETTER COMMUNICATIONS CODE.
                // SOME FLOW CONTROL SHOULD BE IMPLEMENTED
                // System.out.println("RESync...");
                try {
                    Thread.sleep(200);

                } catch (Exception e1) {
                    // TODO Auto-generated catch block
                    e1.printStackTrace();
                }
            }
            //}
        }else{
            int duration = Toast.LENGTH_SHORT;

            Toast.makeText(context, "You haven't received anything yet", duration).show();

        }
    }
    private static void readData(String[] fields, int[] data1, int i) {
        int k = 0;
        for (int j = i; k < data1.length; j++, k++) {
            if (j >= fields.length)
                j = 0;
            data1[k] = Integer.parseInt(fields[j], 16) - 128;
        }
    }


    private void writeToFile(String data) {
        try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(context.openFileOutput("config.txt", Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }
    }
    private String readFromFile() {

        String ret = "";

        try {
            InputStream inputStream = context.openFileInput("config.txt");

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                ret = stringBuilder.toString();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }

        return ret;
    }

    @SuppressLint("MissingPermission")
    public String GetLocandandBearing(){
        //////////////////////////////////////////////
        /////new
        ////////////////////////////////////////////////////
        //get your gps location and bearing

        Location gps_loc = null;
        Location network_loc = null;
        Location final_loc;
        double longitude;
        double latitude;
        double bearing;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

//                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                        && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE) != PackageManager.PERMISSION_GRANTED) {
//
//                    return;
//                }

        try {

            gps_loc = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            network_loc = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);

        } catch (Exception ee) {
            ee.printStackTrace();
        }

        if (gps_loc != null) {
            final_loc = gps_loc;
            latitude = final_loc.getLatitude();
            longitude = final_loc.getLongitude();
            //Log.i("Here", "your1 location lat: " + latitude + "long:" + longitude);
            bearing = gps_loc.getBearing();
            //bearing = -57;
            //Log.i("Here", "your1 bearing lat: " + bearing);
            String location = "lat," + latitude +",long," + longitude +",bearing," + bearing;

            return location;

        } else if (network_loc != null) {
            final_loc = network_loc;
            latitude = final_loc.getLatitude();
            longitude = final_loc.getLongitude();
            //Log.i("Here", "your2 location lat: " + latitude + "long:" + longitude);
            bearing = network_loc.getBearing();
            //Log.i("Here", "your1 bearing lat: " + bearing);

            String location = "lat," + latitude +",long," + longitude +",bearing," + bearing ;
            return location;


        } else {
            latitude = 0.0;
            longitude = 0.0;
            // Log.i("Here", "your3 location lat: " + latitude + "long:" + longitude);
            bearing = 0;
            //Log.i("Here", "your1 bearing lat: " + bearing);
            String location = "lat," + latitude +",long," + longitude +",bearing," + bearing ;

            return location;
        }

    }





}
