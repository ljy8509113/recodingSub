package play.and.eat.com.recording;

/**
 * Created by ljy on 2017-12-08.
 */

import android.os.Environment;
import android.os.Handler;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.nio.file.Files;

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
    private String severIp = "192.168.0.2";
    private int serverPort = 1234;
    TCPClientListener _listener;


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
    public void Connect(String ip, int port, TCPClientListener listener) {
        severIp = ip;
        serverPort = port;
//        dataToSend = null;
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
            Log.d("lee - ", cmd.getBytes().length + " // -----");
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
        sendRunnable = new SendRunnable(connectionSocket);
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
//                    byte[] data = new byte[4];
//                    //Read the first integer, it defines the length of the data to expect
//                    input.read(data, 0, data.length);
//                    int length = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();
//
//                    //Read the second integer, it defines the type of the data to expect
//                    input.read(data, 0, data.length);
//                    int type = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN).getInt();

//                    int read = 0;
//                    int downloaded = 0;

//                    if (type == TCPCommands.TYPE_CMD) {
//                        //We're expecting a command/message from the server (Like a list of files)
//                        //Allocate byte array large enough to contain the data to come
//                        data = new byte[length];
//                        StringBuilder sb = new StringBuilder();
//                        InputStream bis = new BufferedInputStream(input);
//
//                        //Read until all data is read or until we have read the expected amount
//                        while ((read = bis.read(data)) != -1) {
//                            downloaded += read;
//                            sb.append(new String(data, 0, read, "UTF-8")); //Append the data to the StringBuilder
//                            if (downloaded == length) //We have what we expected, break out of the loop
//                                break;
//                        }
//                    } else if (type == TCPCommands.TYPE_FILE_CONTENT) {
//                        //We're expecting a file/raw bytes from the server (Like a file)
//                        //We download the data 2048 bytes at the time
//                        byte[] inputData = new byte[2048];
//                        InputStream bis = new BufferedInputStream(input);
//
//                        //Read until all data is read or until we have read the expected amount
//                        while ((read = bis.read(inputData)) != -1) {
//                            //Buffer loop
//                            downloaded += read;
//                            if (downloaded == length)//We have what we expected, break out of the loop
//                                break;
//                        }
//                    }


                    StringBuffer sb = new StringBuffer();
                    byte[] b = new byte[1024];
                    Log.d("lee - ", input.read(b) + "  /  int");
                    for (int n; (n = input.read(b)) != -1; ) {
                        Log.d(TAG, "result 1: " + sb.toString());
                        sb.append(new String(b, 0, n));
                        _listener.onReceiver(sb.toString());
                        sb = new StringBuffer();
                    }
                    Log.d(TAG, "result 2: " + sb.toString());


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

        public SendRunnable(Socket server) {
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
                    if (dataType == TCPCommands.TYPE_FILE_CONTENT) {
//                        try {
//                            // 파일 내용을 읽으면서 전송
//                            File f = new File(this.path);
//                            FileInputStream fis = new FileInputStream(f);
//                            BufferedOutputStream bis = new BufferedOutputStream(this.out);
//
//                            FileOutputStream dos = new FileOutputStream(f);
//
//                            int len;
//                            int size = (int)f.length();
//                            byte[] headerByte = (dataType + "").getBytes();//ByteBuffer.allocate(4).putInt(dataType).array();
//                            String fileName = f.getName();
//                            Log.d("lee - ", "file name : " + fileName);
//                            byte[] fileNameData = fileName.getBytes();
//                            byte[] bodyData = new byte[size];
//                            byte[] sendData = new byte[size + headerByte.length + fileNameData.length];
//
//                            System.arraycopy(headerByte, 0, sendData, 0, headerByte.length);
//                            System.arraycopy(fileNameData, 0, sendData, headerByte.length, fileNameData.length);
//                            System.arraycopy(bodyData,0, sendData, headerByte.length + fileNameData.length, bodyData.length);
//
//                            Log.d("lee - ", "용량 : " + fis.read(sendData));
//
//                            while ((len = fis.read(sendData)) > 0) {
//                                Log.d("lee - ", "전송중 : " + len + " // 전체 : " + size);
//                                dos.write(sendData, 0, len);
//                            }
//
//                            dos.flush();
//                            dos.close();
//                            bis.close();
//                            fis.close();
//
//                            Log.d("lee - ", "전송완료 close");
//                        } catch (IOException e) {
//                            Log.d("lee - ", "file send error");
//                            e.printStackTrace();
//                        } finally {
//                            Log.d("lee - ", "전송완료");
//                            _listener.sendComplate();
//                        }

                        try {
                            PrintWriter printOut = new PrintWriter(new BufferedWriter(new OutputStreamWriter(this.out)), true);
                            printOut.flush();

                            File f = new File(this.path);
                            if(f.exists()){
                                Log.d("lee - ", "file " + f.length());
                            }

                            int size = (int)f.length();
                            byte[] headerByte = (dataType + "").getBytes();
                            String fileName = f.getName();
                            Log.d("lee - ", "file name : " + fileName);
                            byte[] fileNameData = fileName.getBytes();
                            byte[] bodyData = new byte[size];
                            byte[] sendData = new byte[size + headerByte.length + fileNameData.length];

                            System.arraycopy(headerByte, 0, sendData, 0, headerByte.length);
                            System.arraycopy(fileNameData, 0, sendData, headerByte.length, fileNameData.length);
                            System.arraycopy(bodyData,0, sendData, headerByte.length + fileNameData.length, bodyData.length);

                            DataInputStream dis = new DataInputStream(new FileInputStream(f)); //읽을 파일 경로 적어 주시면 됩니다.
                            DataOutputStream dos = new DataOutputStream(this.out);

                            Log.d("lee - ", "bytes : " + sendData.length);
                            Log.d("lee - ", "read : " + dis.read(sendData));
                            long totalReadBytes = 0;

                            int readBytes;
                            while ( (readBytes = dis.read(sendData)) > 0) { //길이 정해주고 딱 맞게 서버로 보냅니다.
                                Log.d("lee - ", "ing : " + readBytes);
                                dos.write(sendData, 0, readBytes);
                                totalReadBytes += readBytes;
                                Log.d("lee - ", "ing : " + totalReadBytes);
                            }

                            dos.close();
                            _listener.sendComplate();

                        } catch (FileNotFoundException e) {
                            e.printStackTrace();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        try {
//                        //Send the length of the data to be sent
//                        this.out.write(ByteBuffer.allocate(4).putInt(data.length).array());
//                        //Send the type of the data to be sent
//                        this.out.write(ByteBuffer.allocate(4).putInt(dataType).array());
                            //Send the data

                            byte[] headerByte = (dataType + "").getBytes();//IntToByteArray(dataType);
                            byte[] sendData = new byte[data.length + headerByte.length];

                            System.arraycopy(headerByte, 0, sendData, 0, headerByte.length);
                            System.arraycopy(this.data,0, sendData, headerByte.length, this.data.length);

                            String str1 = new String(sendData,0,sendData.length);

                            Log.d("lee - ", str1 + " : length : ");

                            this.out.write(sendData, 0, sendData.length);
                            //Flush the stream to be sure all bytes has been written out
                            this.out.flush();

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
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
                InetAddress serverAddr = InetAddress.getByName(severIp);
                startTime = System.currentTimeMillis();
                //Create a new instance of Socket
                connectionSocket = new Socket();

                //Start connecting to the server with 5000ms timeout
                //This will block the thread until a connection is established
                connectionSocket.connect(new InetSocketAddress(serverAddr, serverPort), 10000);

                long time = System.currentTimeMillis() - startTime;
                Log.d(TAG, "Connected! Current duration: " + time + "ms");
                _listener.connectionSuccess();

            } catch (Exception e) {
                e.printStackTrace();
            }
            Log.d(TAG, "Connetion thread stopped");
        }
    }

    public static class TCPCommands {
        public static int TYPE_CMD = 1; //For commands. They will be strings
        public static int TYPE_FILE_CONTENT = 2;//For files. They will be byte arrays
        public static String CMD_REQUEST_FILES = "server_get_files"; //When client request a list of files in that folder
        public static String CMD_REQUEST_FILES_RESPONSE = "server_get_files_response"; //When server respons with a list of files
        public static String CMD_REQUEST_FILE_DOWNLOAD = "server_download_file"; //When client request a file to be transfererad from the server.
    }

    public byte[] IntToByteArray( int data ) {
        byte[] result = new byte[4];

        result[0] = (byte) ((data & 0xFF000000) >> 24);
        result[1] = (byte) ((data & 0x00FF0000) >> 16);
        result[2] = (byte) ((data & 0x0000FF00) >> 8);
        result[3] = (byte) ((data & 0x000000FF) >> 0);

        return result;
    }

}
