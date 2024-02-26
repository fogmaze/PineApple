package com.example.pineapple;

import android.util.Log;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread{
    private final Callback callback;
    private final int port;
    private final Object lock = new Object();
    private  ServerSocket serverSocket = null;
    private boolean isRunning = false;
    public Server(Callback callback, int port) {
        this.callback = callback;
        this.port = port;
    }
    @Override
    public void run() {
        try {
            isRunning = true;
            serverSocket = new ServerSocket(port);
            Log.i("Server", "Server started");
            boolean isRunning = true;
            while (isRunning) {
                Socket socket = serverSocket.accept();
                Log.i("Server", "Connection accepted");
                isRunning = callback.onConnection(socket);
            }
            serverSocket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public interface Callback {
        boolean onConnection(Socket socket);
    }
}

