package com.example.kevin.signalclient;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private Socket socket;
    private BufferedReader sin;
    private PrintWriter sout;
    private ImageButton pingButton;
    private TextView status;
    private TextView refreshTime;
    private Boolean CONNECTION = false;
    private Thread statusThread;
    private Boolean paused =false;
    private ImageView rejectImage,acceptImage,pendingImage,idleImage;
    private ArrayList<ImageView> images;
    private boolean pending;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        status = findViewById(R.id.statusText);
        refreshTime = findViewById(R.id.refreshText);

        Button button = findViewById(R.id.connectButton);
        button.setOnClickListener(this);

         rejectImage = findViewById(R.id.rejectImage);
         acceptImage = findViewById(R.id.acceptImage);
         pendingImage = findViewById(R.id.pendingImage);
         idleImage = findViewById(R.id.idleImage);

        rejectImage.setImageAlpha(255);
        acceptImage.setImageAlpha(255);
        pendingImage.setImageAlpha(255);

        images = new ArrayList<ImageView>();
        images.add(rejectImage);
        images.add(acceptImage);
        images.add(pendingImage);
        images.add(idleImage);


        highlight(idleImage);
        status.setTypeface(null, Typeface.BOLD_ITALIC);
        startConnect();
        connect();



    }
    @Override
    protected void onPause() {
        super.onPause();
        paused=true;

    }

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        if (pending == false) highlight(idleImage);
    }

    @Override
    protected void onStop() {
        super.onStop();
        paused = true;
    }


    private void connect() { //this method runs the isConnected() continuously to update the state of the connection, and attempt connecting if the connection is lost

        statusThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while(!paused)
                {

                    try {
                        Thread thread = new Thread(connectionRunnable);

                        thread.start();
                        thread.sleep(1000);
                    }

                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }

                }
            }
        });
        statusThread.start();


    }
    private void startConnect(View view) // method to evoke by the button
    {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.1.238", 4443);
                    CONNECTION = true;
                }
                catch (Exception e) {
                    CONNECTION = false;
                    e.printStackTrace();
                }
            }
        });
        thread.start(); //this thread is temporary
    }

    private void startConnect() //this is method to attempt connection with server for the methods in this class
    {
        Thread thread = new Thread(new Runnable() { //off ui networking thread
            @Override
            public void run() {
                try {
                    socket = new Socket("192.168.1.238", 4443); //tries to establish connection, and outputs the connection status to the CONNECTION global boolean
                    CONNECTION = true;
                }
                catch (Exception e) {
                    CONNECTION = false;
                    e.printStackTrace();
                }
            }
        });
        thread.start(); //this thread is temporary
    }

    private Runnable connectionRunnable = new Runnable() { //this checks if the connection still exists and outputs the connection status to a textview by changing the global CONNECTED boolean
        @Override
        public void run() {

            try {
                if (CONNECTION == true) // if the previous connection is established
                {
                    if (isConnected() == false) //this is when the previously established connection is broken
                    {
                        CONNECTION = false;
                    }
                }
                else //if there was no previously established connection, then it attempts to connect
                {
                    startConnect();
                }

            }
            catch (Exception e)
            {
                e.printStackTrace();
                return;
            }

            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    status.setText((CONNECTION) ? "CONNECTED" : "DISCONNECTED");
                }
            });


        }
    };
    private boolean isConnected() throws Exception //this method returns a boolean determining the state of the program's connection with the server.
    {

        ExecutorService executorService = Executors.newSingleThreadExecutor(); //need this to implement the callable
        Callable<Boolean> callable = new Callable<Boolean>() { //I used the callable because it is a pain in the neck to return booleans from runnables (threading)
            @Override
            public Boolean call() throws Exception {
                try //try catch block determines state of the connection to the server
                {
                    Socket s = new Socket("192.168.1.238", 4443);
                    return true;

                }
                catch (IOException e)
                {
                    return false;
                }

            }
        };
        Future<Boolean> future = executorService.submit(callable); //need the future thing because it is the only type the executorservice can return from callable
        executorService.shutdown();
        return future.get(); //returns the state of the connection
    }



    private void recievePing() {
        Thread thread = new Thread(new Runnable() {

            @Override
            public void run() {
                try {
                    sin = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    final int CODE = Integer.parseInt(sin.readLine());
                    MainActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (CODE == 0) {
                                highlight(rejectImage);
                            } else {
                                highlight(acceptImage);
                            }
                            pending = false;
                        }
                    });


                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();


    }

    private void sendPing() {
        try {
            if (CONNECTION) {
                sout = new PrintWriter(socket.getOutputStream());
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        sout.println("1");
                        sout.flush();
                    }
                });
                thread.start();
                highlight(pendingImage);
                pending = true;


            }
            else
            {
                Toast.makeText(this, "Can't send request if not connected", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.connectButton:
                sendPing();

                recievePing();
        }
    }
    private void highlight(ImageView view)
    {
        for (int i = 0; i < images.size(); i++)
        {
            if (images.get(i) == view)
            {
                images.get(i).setImageAlpha(255);
            }
            else
            {
                images.get(i).setImageAlpha(123);
            }
        }
    }
}
