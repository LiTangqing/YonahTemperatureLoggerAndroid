package sg.yonah.serialmanager;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;

import java.io.UnsupportedEncodingException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import android.app.Activity;
import android.view.View;
import android.widget.Button;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import im.delight.android.location.SimpleLocation;

public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "sg.yonah.serialmanager.USB_PERMISSION";
    public final String POSTURL = "http://128.199.202.243/api/trip/";
    Button startButton, clearButton ;
    TextView tvConnectivity, tvLocation,tvLog, tvResponse;
    JSONObject logJsonBody;
    JSONObject tripJsonBody;
    JSONArray logJsonArray;

    //serial var
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    //location var
    private SimpleLocation location;

    //call back one receive serial packet
    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            String toSend;
            String boxSignal = "";

            try {
                data = new String(arg0, "UTF-8");
                String currTime = getTime();

                //send back log to arduino, w timestamp tagged
                if (data.length() > 7){
                    /*
                    reason why check length larger than 7: we observe some msg received might be broken
                    e.g. only receive half of msg
                    thus check for complete msg here
                    */

                    toSend = data+ "    "+ currTime +"    "+'\n';
                    serialPort.write(toSend.getBytes());

                    String temp = data.split(":")[1];
                    logJsonBody.put("time",currTime);
                    logJsonBody.put("temp",temp);

                    //extra check for box activity
                    if (data.contains("??")){
                        //box open
                        //update ui
                        boxSignal = "Box opened at Lat:"+ Double.toString(location.getLatitude())+", Long: "+Double.toString(location.getLongitude())+"\n";
                        tvAppend(tvLocation,boxSignal);

                        //update json body
                        logJsonBody.put("box_activity",boxSignal);
                        logJsonArray.put(logJsonBody);

                        tripJsonBody.put("lati_boxopen",Double.toString(location.getLatitude()));
                        tripJsonBody.put("longi_boxopen",Double.toString(location.getLongitude()));
                        tripJsonBody.put("logs",logJsonArray);

                        //formulate POST request and add to request queue
                        JsonObjectRequest jsObjRequest = new JsonObjectRequest
                                (Request.Method.POST, POSTURL, tripJsonBody, new Response.Listener<JSONObject>() {

                                    @Override
                                    public void onResponse(JSONObject response) {
                                        tvResponse.setText("Response: " + response.toString());
                                    }
                                }, new Response.ErrorListener() {

                                    @Override
                                    public void onErrorResponse(VolleyError error) {
                                        // TODO Auto-generated method stub
                                    }
                                });

                        Singleton.getInstance(getApplicationContext()).addToRequestQueue(jsObjRequest);

                    }else if(data.contains("!!")) {
                        //box closes
                        //update ui
                        tvUpdate(tvLog,data);
                        boxSignal = "Box closed at Lat:"+ Double.toString(location.getLatitude())+", Long: "+Double.toString(location.getLongitude())+"\n";
                        tvAppend(tvLocation,boxSignal);

                        // log down
                        logJsonBody.put("box_activity",boxSignal);
                        logJsonArray.put(logJsonBody);

                    } else{
                        logJsonArray.put(logJsonBody);
                        tvUpdate(tvLog,data);
                    }
                }

                //send back box activity also if there any (with geolocation tagged)
                if (boxSignal!=""){
                    toSend = boxSignal+"\n";
                    serialPort.write(toSend.getBytes());
                }

            } catch (UnsupportedEncodingException | JSONException e) {
                e.printStackTrace();
            }
        }
    };

    //Broadcast Receiver to automatically start and stop the Serial connection.
    private final BroadcastReceiver broadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(ACTION_USB_PERMISSION)) {
                boolean granted = intent.getExtras().getBoolean(UsbManager.EXTRA_PERMISSION_GRANTED);

                if (granted) {
                    //permission is granted
                    connection = usbManager.openDevice(device);
                    serialPort = UsbSerialDevice.createUsbSerialDevice(device, connection);
                    if (serialPort != null) {
                        if (serialPort.open()) { //Set Serial Connection Parameters.
                            serialPort.setBaudRate(115200);
                            serialPort.setDataBits(UsbSerialInterface.DATA_BITS_8);
                            serialPort.setStopBits(UsbSerialInterface.STOP_BITS_1);
                            serialPort.setParity(UsbSerialInterface.PARITY_NONE);
                            serialPort.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                            serialPort.read(mCallback);
                            tvUpdate(tvConnectivity,"Serial Connection Opened!\n");

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
                serialPort.close();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ui－related
        startButton = (Button) findViewById(R.id.buttonStart);
        clearButton = (Button) findViewById(R.id.buttonClear);
        tvLog = (TextView) findViewById(R.id.tvLog);
        tvLocation = (TextView) findViewById(R.id.tvLocation);
        tvConnectivity = (TextView) findViewById(R.id.tvConnectivity);
        tvResponse = (TextView)findViewById(R.id.tvResponse);

        //location var
        location = new SimpleLocation(this);
        if (!location.hasLocationEnabled()) {
            // ask the user to enable location access
            SimpleLocation.openSettings(this);
        }

        // start motion control service(vibrate when phone leans)
        startService(new Intent(this,MotionControlService.class));

        //request queue n jsonbody
        logJsonBody = new JSONObject();
        tripJsonBody = new JSONObject();
        logJsonArray = new JSONArray();
        RequestQueue queue = Singleton.getInstance(getApplicationContext()).
                getRequestQueue();

        // serial communication
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);

        //start
        startButton.performClick();
    }



    public void onClickStart(View view) {

        HashMap<String, UsbDevice> usbDevices = usbManager.getDeviceList();
        if (!usbDevices.isEmpty()) {
            boolean keep = true;
            for (Map.Entry<String, UsbDevice> entry : usbDevices.entrySet()) {
                device = entry.getValue();
                int deviceVID = device.getVendorId();
                if (deviceVID == 0x2341)//Arduino Vendor ID
                //if(!usbManager.hasPermission(device))
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

//
//    public void onClickStop(View view) {
//        setUiEnabled(false);
//        serialPort.close();
//        tvAppend(textView,"\nSerial Connection Closed! \n");

//    }

    public void onClickClear(View view) {
        tvLog.setText(" ");
        tvConnectivity.setText(" ");
        tvLocation.setText(" ");
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

    private void tvUpdate(TextView tv, String text) {
        final TextView ftv = tv;
        final String ftext = text;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ftv.setText(ftext);
            }
        });
    }


    public String getTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("HH:mm:ss a");
        date.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));   // specify time zone here
        return date.format(currentLocalTime);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // make the device update its location
        location.beginUpdates();
    }

    @Override
    protected void onPause() {
        // stop location updates (saves battery)
        location.endUpdates();

        // ...

        super.onPause();
    }

}
