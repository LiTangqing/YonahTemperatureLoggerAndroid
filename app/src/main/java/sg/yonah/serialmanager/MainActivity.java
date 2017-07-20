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

import im.delight.android.location.SimpleLocation;

public class MainActivity extends Activity {
    public final String ACTION_USB_PERMISSION = "sg.yonah.serialmanager.USB_PERMISSION";
    Button startButton, clearButton ;
    TextView textView, tvLocation;

    //serial var
    UsbManager usbManager;
    UsbDevice device;
    UsbSerialDevice serialPort;
    UsbDeviceConnection connection;

    //location var
    private SimpleLocation location;

    UsbSerialInterface.UsbReadCallback mCallback = new UsbSerialInterface.UsbReadCallback() {
        @Override
        public void onReceivedData(byte[] arg0) {
            String data;
            String toSend;
            String tripSignal = "";

            try {
                data = new String(arg0, "UTF-8");

                if (data.contains("??")){
                    tripSignal = "Arrival at Lat:"+ Double.toString(location.getLatitude())+", Long: "+Double.toString(location.getLongitude())+"\n";
                    tripSignal = "ARRIVAL";
                    tvAppend(textView,tripSignal);
                    //send sms.  how to get receipient?

                }else if(data.contains("!!")) {
                    tripSignal = "Departure at Lat:"+ Double.toString(location.getLatitude())+", Long: "+Double.toString(location.getLongitude())+"\n";
                    tripSignal = "DEPART";
                    tvAppend(textView,tripSignal);
                }
//                toSend = tripSignal + getTime();
//                toSend.concat(" = ");
                if (data.length()>5){
                    toSend = data+ "    "+ getTime() +"    "+tripSignal+'\n' ;
                    serialPort.write(toSend.getBytes());
                }

//                serialPort.write(arg0);
            } catch (UnsupportedEncodingException e) {
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
                serialPort.close();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //ui related
        startButton = (Button) findViewById(R.id.buttonStart);
        clearButton = (Button) findViewById(R.id.buttonClear);
        textView = (TextView) findViewById(R.id.textView);
        tvLocation = (TextView) findViewById(R.id.tvLocation);

        //location var
        location = new SimpleLocation(this);
        if (!location.hasLocationEnabled()) {
            // ask the user to enable location access
            SimpleLocation.openSettings(this);
        }

        // start motion control service(vibrate when phone leans)
        startService(new Intent(this,MotionControlService.class));

        // serial communication
        usbManager = (UsbManager) getSystemService(this.USB_SERVICE);
        IntentFilter filter = new IntentFilter();
        filter.addAction(ACTION_USB_PERMISSION);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        registerReceiver(broadcastReceiver, filter);
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
        textView.setText(" ");
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


    public String getTime() {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT+1:00"));
        Date currentLocalTime = cal.getTime();
        DateFormat date = new SimpleDateFormat("HH:mm:ss a");
        date.setTimeZone(TimeZone.getTimeZone("GMT+8:00"));   // specify time zone here
        return date.format(currentLocalTime);
    }

//    public String getLocation(SimpleLocation location){
//        String result = "lat: ";
//        result+=String.valueOf(location.getLatitude());
//        result+= ",long:";
//        result+=String.valueOf(location.getLongitude());
//        return result;
//    }

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
