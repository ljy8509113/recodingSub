package play.and.eat.com.recording;

/**
 * Created by ljy on 2017-12-08.
 */

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;

import play.and.eat.com.recording.play.and.eat.com.recording.listener.TCPClientListener;

public class TCPClient {
    private static String TAG = "TCPClient"; //For debugging, always a good idea to have defined
    boolean receiveThreadRunning = false;
    private long startTime = 0l;

    private Socket connectionSocket;
    //Runnables for sending and receiving data
    private SendRunnable sendRunnable;
    private ReceiveRunnable receiveRunnable;
    //Threads to execute the Runnables above
    private Thread sendThread;
    private Thread receiveThread;
    //    byte[] dataToSend;
    private String serverIp = "192.168.0.2";
    private int serverPort = 1234;
    TCPClientListener _listener;
    String _uuid = "";

    /**
     * Returns true if TCPClient is connected, else false
     *
     * @return Boolean
     */
    public boolean isConnected() {
        return connectionSocket != null && connectionSocket.isConnected() && !connectionSocket.isClosed();
    }

    /**
     * Open connection to server
     */
    public void Connect(String ip, int port, TCPClientListener listener, String uuid) {
        serverIp = ip;
        serverPort = port;
        _uuid = uuid;
        _listener = listener;
        new Thread(new ConnectRunnable()).start();
    }

    /**
     * Close connection to server
     */
    public void Disconnect() {
        stopThreads();
        try {
            connectionSocket.close();
            Log.d(TAG, "Disconnected!");
        } catch (IOException e) {
        }
    }

    /**
     * Send data to server
     *
     * @param path byte array to send
     */
    public void WriteData(String path) {
        if (isConnected()) {
            startSending();
            sendRunnable.Send(path);
        }
    }

    /**
     * Send command to server
     *
     * @param cmd Commands as string to send
     */
    public void WriteCommand(String cmd) {
        if (isConnected()) {
            startSending();
            Log.d("lee - ", cmd.getBytes().length + " // ----- WriteCommand");
            sendRunnable.SendCMD(cmd.getBytes());
        }
    }

    private void stopThreads() {
        if (receiveThread != null)
            receiveThread.interrupt();

        if (sendThread != null)
            sendThread.interrupt();
    }

    private void startSending() {
        sendRunnable = new SendRunnable(connectionSocket, this.serverIp);
        sendThread = new Thread(sendRunnable);
        sendThread.start();
    }

    private void startReceiving() {
        receiveRunnable = new ReceiveRunnable(connectionSocket);
        receiveThread = new Thread(receiveRunnable);
        receiveThread.start();
    }

    public class ReceiveRunnable implements Runnable {
        private Socket sock;
        private InputStream input;

        public ReceiveRunnable(Socket server) {
            sock = server;
            try {
                input = sock.getInputStream();

            } catch (Exception e) {
            }
        }

        @Override
        public void run() {
            Log.d(TAG, "Receiving started");
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                if (!receiveThreadRunning)
                    receiveThreadRunning = true;

                startTime = System.currentTimeMillis();
                try {
                    StringBuffer sb = new StringBuffer();
                    byte[] b = new byte[1024];
                    //Log.d("lee - ", input.read(b) + "  /  int");
                    for (int n; (n = input.read(b)) != -1; ) {
                        //Log.d(TAG, "result 1: " + sb.toString());
                        sb.append(new String(b, 0, n));
                        _listener.onReceiver(sb.toString());
                        sb = new StringBuffer();
                    }
                    //Stop listening so we don't have e thread using up CPU-cycles when we're not expecting data
                    stopThreads();
                } catch (Exception e) {
                    Log.d("lee - socket", e.toString());
                    Disconnect(); //Gets stuck in a loop if we don't call this on error!
                }
            }
            receiveThreadRunning = false;
            Log.d(TAG, "Receiving stopped");
        }

    }

    public class SendRunnable implements Runnable {

        byte[] data;
        String path = "";
        private OutputStream out;
        private boolean hasMessage = false;
        int dataType = 1;
        String ip = "";

        public SendRunnable(Socket server, String ip) {
            this.ip = ip;
            try {
                this.out = server.getOutputStream();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        /**
         * Send data as bytes to the server
         *
         * @param path
         */
        public void Send(String path) {
            this.data = "file".getBytes();
            this.path = path;
            dataType = TCPCommands.TYPE_FILE_CONTENT;
            this.hasMessage = true;
        }

        /**
         * Send a command/message to the server
         *
         * @param bytes
         */
        public void SendCMD(byte[] bytes) {
            this.data = bytes;
            dataType = TCPCommands.TYPE_CMD;
            this.hasMessage = true;
        }

        @Override
        public void run() {
            Log.d(TAG, "Sending started");
            while (!Thread.currentThread().isInterrupted() && isConnected()) {
                if (this.hasMessage) {
                    startTime = System.currentTimeMillis();
                    try {
                        String str1 = new String(this.data,0,this.data.length);
                        Log.d("lee - ", str1 + " : length : " + this.data.length);

                        this.out.write(this.data, 0, this.data.length);
                        //Flush the stream to be sure all bytes has been written out
                        this.out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    this.hasMessage = false;

                    long time = System.currentTimeMillis() - startTime;
                    Log.d(TAG, "Command has been sent! Current duration: " + time + "ms // data : " + this.data.length);
                    this.data = null;
                    if (!receiveThreadRunning)
                        startReceiving(); //Start the receiving thread if it's not already running
                }
            }
            Log.d(TAG, "Sending stopped");
        }
    }

    public class ConnectRunnable implements Runnable {
        public void run() {
            try {
                Log.d(TAG, "C: Connecting...");
                InetAddress serverAddr = InetAddress.getByName(serverIp);
                startTime = System.currentTimeMillis();
                //Create a new instance of Socket
                connectionSocket = new Socket();

                //Start connecting to the server with 5000ms timeout
                //This will block the thread until a connection is established
                connectionSocket.connect(new InetSocketAddress(serverAddr, serverPort), 10000);

                long time = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Connected! Current duration: " + time + "ms");
                //_listener.connectionSuccess();

            } catch (Exception e) {
                if(e.getMessage().toLowerCase().contains("no route to host") || e.getMessage().toLowerCase().contains("connection refused")){
                    Log.d("lee", "Toast");
                    _listener.error("IP 혹은 PORT 확인이 필요합니다.");
                }else{
                    Log.d("lee", "connectionSnap");
                    _listener.connectionSnap();
                }
                Log.d("lee socket mas", e.getMessage());
            }

            Log.d(TAG, "Connetion thread stopped");
            if(connectionSocket.isConnected()){
                Log.d(TAG, "is connection success");
                _listener.connectionSuccess();
            }else{
                Log.d(TAG, "is connection not");
            }
        }
    }

    public static class TCPCommands {
        public static int TYPE_CMD = 1; //For commands. They will be strings
        public static int TYPE_FILE_CONTENT = 2;//For files. They will be byte arrays
    }
}
