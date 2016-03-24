package com.example.yezikun.accelerometertest;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.SensorEventListener;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.hardware.SensorManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.media.CameraProfile;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Typeface;
import android.graphics.Matrix;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

import java.net.URL;
import java.net.HttpURLConnection;
import java.io.InputStreamReader;
import java.util.Date;

public class MainActivity extends Activity {

    boolean isfirsttouch = true;

    SensorManager mSensorManager;
    Sensor mAccelerometer;
    Sensor mMSensor;
    Button btn_getPosition,btn_plus,btn_minus,btn_camera;
    TextView textview_show,tv_position;
    ImageView iv;
    Bitmap newb ;
    Canvas canvasTemp;
    Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
    float[] linear_acceleration=new float[3];
    float[] angle=new float[3];
    float[] rotation=new float[9];//旋转矩阵
    float[] currentPosition=new float[3];//当前位置
    float[] formerPosition=new float[3];//前一步的位置
    float[] absPosition=new float[3];
    float[] initialPosition=new float[3];//开始的绝对位置

    int canvasHight=1500;
    int canvasWidth=1400;

    float accel_avg=9.8f,accele1;
    float[] mag=new float[3];//记录磁场
    float minAccel=0,maxAccel=0;//记录一步之中最大最小的加速度（处理过后的加速度）
    int stepCounter=0;//记录总的步数

    int counter=0;
    boolean flag=false;//检测计步器长时间没有反应 则跟新数组

    int timeCounter=0;//用于设定采样时间

    ArrayList arrayList=new ArrayList();//用于处理加速度 过程动态数组
    ArrayList arrayList2=new ArrayList();//记录一步之中的所有加速度

    DecimalFormat nf= new DecimalFormat("0");
    DecimalFormat nf2= new DecimalFormat("0.00");

    float scale=1.0f;

    String myURL="http://127.0.0.1/vlc/location_to_android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        currentPosition[0] = currentPosition[1] = currentPosition[2] = 0;
        absPosition[0]=absPosition[1]=0;
        initialPosition[0]=initialPosition[1]=0;


        getService();

        initView();

        setListeners();

        setCanvas();


    }

    private void getService() {
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mMSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    private void initView() {
        btn_getPosition=(Button)findViewById(R.id.btn_getPosition);
        btn_plus=(Button)findViewById(R.id.btn_plus);
        btn_minus=(Button)findViewById(R.id.btn_minus);
        btn_camera=(Button)findViewById(R.id.btn_camera);

        iv=(ImageView)findViewById(R.id.imageView);

        textview_show=(TextView)findViewById(R.id.textview_show);

        tv_position=(TextView)findViewById(R.id.tv_position);
    }

    private void setCanvas() {
        newb = Bitmap.createBitmap(canvasWidth,canvasHight, Bitmap.Config.ARGB_8888);
        canvasTemp = new Canvas(newb);
        canvasTemp.drawColor(Color.WHITE);

        p.setColor(Color.BLACK);
        iv.setImageBitmap(newb);

    }

    @Override
    protected  void onResume(){
        super.onResume();
        if (mAccelerometer == null){
            Toast.makeText(MainActivity.this,
                    "No Accelerometer Sensor! quit-",
                    Toast.LENGTH_LONG).show();
        }else{


            mSensorManager.registerListener(myListener,mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            mSensorManager.registerListener(myListener, mMSensor, SensorManager.SENSOR_DELAY_FASTEST);

        }



    }


    SensorEventListener myListener=new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {

            if(event.sensor.getType()==Sensor.TYPE_MAGNETIC_FIELD){
                Log.v("Tag","Mag!");

                mag=event.values;
            }

            if(event.sensor.getType()==Sensor.TYPE_ACCELEROMETER) {
                Log.v("Tag","Accel!");


                final float alpha = 0.8f;//低通滤波参数

                linear_acceleration = event.values;

                float accele = (float)Math.sqrt(linear_acceleration[0] * linear_acceleration[0] + linear_acceleration[1] * linear_acceleration[1] + linear_acceleration[2] * linear_acceleration[2]);

                
                //arrayList每隔40ms采集一个加速度 并滤波，取平均值的处理
                if(timeCounter==5) {
                    // 用低通滤波器分离出重力加速度
                    accel_avg = accel_avg * alpha + accele * (1 - alpha);

                    // 用高通滤波器剔除重力干扰
                    float accel_hp = accele - accel_avg;



                    if (arrayList.isEmpty()) {
                        arrayList2.add(accel_hp);
                        arrayList.add(accel_hp);
                        Log.v("Tag", "add!");


                    } else {
                        int size = arrayList.size();
                        if (size == 1) {
                            arrayList.add(accel_hp);
                            arrayList2.add(accel_hp);

                            Log.v("Tag", "size1");


                        } else {

                            Log.v("Tag", "size2");
                            arrayList.add(accel_hp);
                            float[] acceleArray = new float[3];
                            float accele11 = 0;

                            for (int i = 0; i < 3; i++) {
                                acceleArray[i] = getFloatNumber(arrayList,size-i);

                                accele11 += acceleArray[i];

                            }
                            accele1 = accele11 / 3.0f;

                            arrayList2.add(accele1);
                            int size2=arrayList2.size();
                            float[] accele3=new float[3];
                            accele3[0]=getFloatNumber(arrayList2,size2-3);
                            accele3[1]=getFloatNumber(arrayList2,size2-2);
                            accele3[2]=getFloatNumber(arrayList2,size2-1);

                            if (accele3[1] > maxAccel && accele3[1] > 0.7&& accele3[1]>accele3[0]&&accele3[1]>accele3[2]) {
                                maxAccel = accele3[1];
                                counter = 0;
                                flag = true;
                            }
                            if (accele3[1] < minAccel && accele3[1] < -0.7&& accele3[1]<accele3[0]&&accele3[1]<accele3[2]) {
                                minAccel = accele3[1];
                            }


                            if (maxAccel > 0.7 && minAccel < -0.7 && counter >= 5) {
                                //最小最大加速度超过阈值且两个值之间的时间差超过设定值 记录一步

                                //利用加速度估算步长
                                float stepLength=getCurrentStepLength();

                                //跟新计步器
                                updateStepCounter();

                                //利用磁场与加速度获得当前方位角
                                getCurrentOrientation();


                                //跟新位置
                                updatePosition(stepLength);

                                //跟新Canvas
                                updateCanvas();

                            } else {
                                if (counter > 10) {
                                    //长时间未检测到
                                    staySmooth();
                                }
                                if (flag) counter++;
                            }

                        }

                    }


                    //builder.append((int) (accel_hp * 10.0));
                    //builder.append(" ");
                    //builder.append((event.timestamp - lastTimestamp) / 1000000);// 采样时间差
                    //builder.append("\n");
                    //lastTimestamp = event.timestamp;

                    timeCounter=0;
                }else timeCounter++;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private float getFloatNumber(ArrayList arrayList,int i){
        Log.v("Tag", "getnumber!");

        return Float.parseFloat(String.valueOf(arrayList.get(i)));
    }

    private void updateCanvas() {

        float former_x = 700 + ((formerPosition[1]-initialPosition[1]) * 50);
        float former_y = 750 + ((formerPosition[0]-initialPosition[0]) * 50);
        float current_x = 700 + ((currentPosition[1]-initialPosition[1]) * 50);
        float current_y = 750 + ((currentPosition[0]-initialPosition[0]) * 50);

        tv_position.setText("N:" + nf2.format(currentPosition[0]) + "m,  E:" + nf2.format(currentPosition[1]) + "m");

        canvasTemp.drawLine(former_x, former_y, current_x, current_y, p);

        iv.setImageBitmap(newb);
    }

    private void updatePosition(float stepLength) {
        formerPosition[0] = currentPosition[0];
        formerPosition[1] = currentPosition[1];

        currentPosition[0] += (stepLength * Math.cos(angle[0]));
        currentPosition[1] += (stepLength * Math.sin(angle[0]));

    }

    private float getCurrentStepLength() {
        float length=0.53f*(float)(Math.sqrt(Math.sqrt(maxAccel-minAccel)));
        return length;
    }

    private void getCurrentOrientation() {

        boolean flag1 = SensorManager.getRotationMatrix(rotation, null, linear_acceleration, mag);
        if (flag1) {

            SensorManager.getOrientation(rotation, angle);


        }
    }

    private void staySmooth() {
        counter = 0;
        maxAccel = 0;
        minAccel = 0;
        flag = false;
    }

    private void updateStepCounter() {
        stepCounter++;
        counter = 0;
        maxAccel = 0;
        minAccel = 0;
        flag = false;
        textview_show.setText(nf.format(stepCounter));

    }


    private void setListeners(){
        btn_getPosition.setOnClickListener(getPosition);
        btn_plus.setOnClickListener(plus);
        btn_minus.setOnClickListener(minus);
        btn_camera.setOnClickListener(camera);

    }

    String mCurrentPhotoPath;
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );


        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }


    static final int REQUEST_TAKE_PHOTO  = 1;
    private Button.OnClickListener camera = new Button.OnClickListener(){
        public void onClick(View V){

            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException ex) {
                    // Error occurred while creating the File
                }
                // Continue only if the File was successfully created
                if (photoFile != null) {
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                            Uri.fromFile(photoFile));
                    startActivityForResult(takePictureIntent, REQUEST_TAKE_PHOTO);
                }

            }
        }
    };

    private Button.OnClickListener getPosition= new Button.OnClickListener(){
        public void onClick(View V){

            if(isfirsttouch)
            {
                currentPosition[0]=(float)-1.23;
                currentPosition[1]=(float)-1.53;
                Toast.makeText(getApplicationContext(), "成功修正", Toast.LENGTH_SHORT)
                        .show();
                canvasTemp.drawColor(Color.WHITE);
                tv_position.setText("N:" + nf2.format(currentPosition[0]) + "m,  E:" + nf2.format(currentPosition[1]) + "m");
                textview_show.setText(nf.format(stepCounter));
                isfirsttouch = false;
            }
            else{
                currentPosition[0]=(float)0.61;
                currentPosition[1]=(float)-1.53;
                Toast.makeText(getApplicationContext(), "成功修正", Toast.LENGTH_SHORT)
                        .show();
                canvasTemp.drawColor(Color.WHITE);
                tv_position.setText("N:" + nf2.format(currentPosition[0]) + "m,  E:" + nf2.format(currentPosition[1]) + "m");
                textview_show.setText(nf.format(stepCounter));
            }

            //urlConn();
            /*
            try{
                URL url = new URL(myURL);

                HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();
                httpconn.setRequestMethod("GET");

                httpconn.connect();

                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    Toast.makeText(getApplicationContext(), "连接成功!",
                            Toast.LENGTH_SHORT).show();
                }

                Toast.makeText(getApplicationContext(), myURL, Toast.LENGTH_LONG)
                        .show();
                InputStream in = httpconn.getInputStream();
                final ByteArrayOutputStream bo = new ByteArrayOutputStream();
                byte[] buffer = new byte[1024];
                in.read(buffer); // Read from Buffer.
                bo.write(buffer); // Write Into Buffer.
                String value = new String(buffer, "UTF-8");

                Toast.makeText(getApplicationContext(), value, Toast.LENGTH_LONG)
                        .show();
            }  catch (MalformedURLException e){
                Toast.makeText(getApplicationContext(), "Malformed失败", Toast.LENGTH_SHORT)
                        .show();
                e.printStackTrace();
            }  catch (ProtocolException e){
                Toast.makeText(getApplicationContext(), "Protocol失败", Toast.LENGTH_SHORT)
                        .show();
                e.printStackTrace();
            } catch (IOException e){
                Toast.makeText(getApplicationContext(), "IO失败", Toast.LENGTH_SHORT)
                        .show();
                e.printStackTrace();
            }
            */

        }
    };

    private void urlConn() {

        try {
            //Toast.makeText(getApplicationContext(), "HELLO ANDROID!", Toast.LENGTH_SHORT).show();

            URL url = new URL(myURL);
            HttpURLConnection httpconn = (HttpURLConnection) url.openConnection();

                if (httpconn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                Toast.makeText(getApplicationContext(), "连接成功!",
                        Toast.LENGTH_SHORT).show();
                    // InputStreamReader
                    InputStreamReader isr = new InputStreamReader(httpconn.getInputStream(), "utf-8");
//获取绝对位置？
                    if(initialPosition[0]==0&&initialPosition[1]==0){

                        initialPosition[0]=absPosition[0];
                        initialPosition[1]=absPosition[1];

                    }
                    formerPosition[0]=absPosition[0];
                    formerPosition[1]=absPosition[1];
                    currentPosition[0]=absPosition[0];
                    currentPosition[1]=currentPosition[1];

                    isr.close();

                }

            httpconn.disconnect();


        } catch (Exception e) {
            Toast.makeText(getApplicationContext(), "连接失败", Toast.LENGTH_SHORT)
                    .show();
            e.printStackTrace();
        }

    }


    private Button.OnClickListener plus= new Button.OnClickListener(){
        public void onClick(View V){
            currentPosition[0] = currentPosition[1] = currentPosition[2] = 0;
            absPosition[0]=absPosition[1]=0;
            initialPosition[0]=initialPosition[1]=0;
            minAccel=0;
            maxAccel=0;
            stepCounter=0;
            counter=0;
            flag=false;
            arrayList.clear();
            arrayList2.clear();
            canvasTemp.drawColor(Color.WHITE);
            iv.setImageBitmap(newb);
            tv_position.setText("N:" + nf2.format(currentPosition[0]) + "m,  E:" + nf2.format(currentPosition[1]) + "m");
            textview_show.setText(nf.format(stepCounter));

        }
    };
    private Button.OnClickListener minus= new Button.OnClickListener(){
        public void onClick(View V){
            Matrix matrix = new Matrix();
            scale=0.8f;
            matrix.postScale(scale, scale); // 长和宽放大缩小的比例
            Bitmap newb1 = Bitmap.createBitmap(newb, 0, 0, canvasWidth,canvasHight , matrix, true);
            iv.setImageBitmap(newb1);
        }
    };
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        mSensorManager.unregisterListener(myListener);

    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        
        mSensorManager.unregisterListener(myListener);

    }
}
