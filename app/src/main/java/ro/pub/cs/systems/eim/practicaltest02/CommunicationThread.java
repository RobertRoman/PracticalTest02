package ro.pub.cs.systems.eim.practicaltest02;


import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.HttpResponse;
import cz.msebera.android.httpclient.NameValuePair;
import cz.msebera.android.httpclient.client.HttpClient;
import cz.msebera.android.httpclient.client.ResponseHandler;
import cz.msebera.android.httpclient.client.entity.UrlEncodedFormEntity;
import cz.msebera.android.httpclient.client.methods.HttpGet;
import cz.msebera.android.httpclient.client.methods.HttpPost;
import cz.msebera.android.httpclient.impl.client.BasicResponseHandler;
import cz.msebera.android.httpclient.impl.client.DefaultHttpClient;
import cz.msebera.android.httpclient.message.BasicNameValuePair;
import cz.msebera.android.httpclient.protocol.HTTP;
import cz.msebera.android.httpclient.util.EntityUtils;

public class CommunicationThread extends Thread {
    private Boolean once = true;
    private ServerThread serverThread;
    private Socket socket;
    String informationType;
    String data;
    String cursInformation = null;
    BufferedReader bufferedReader;
    PrintWriter printWriter = null;

    public CommunicationThread(ServerThread serverThread, Socket socket) {
        this.serverThread = serverThread;
        this.socket = socket;
    }

    @Override
    public void run() {
        new Timer().scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {


                if (socket == null) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] Socket is null!");
                    return;
                }
                try {

                    if (once) {
                        bufferedReader = Utilities.getReader(socket);
                        printWriter = Utilities.getWriter(socket);
                        if (bufferedReader == null || printWriter == null) {
                            Log.e(Constants.TAG, "[COMMUNICATION THREAD] Buffered Reader / Print Writer are null!");
                            return;
                        }
                        Log.i(Constants.TAG, "[COMMUNICATION THREAD] Waiting for parameters from client curs type!");
                        informationType = bufferedReader.readLine();

                        data = serverThread.getData();
                        once = false;
                    }

                    Log.i(Constants.TAG, "[COMMUNICATION THREAD] Getting the information from the webservice...");
                    HttpClient httpClient = new DefaultHttpClient();
                    String pageSourceCode = "";

                    HttpGet httpGet = new HttpGet(Constants.WEB_SERVICE_ADDRESS + informationType + ".json");
                    Log.i("Test", httpGet.toString());
                    HttpResponse httpGetResponse = httpClient.execute(httpGet);
                    HttpEntity httpGetEntity = httpGetResponse.getEntity();
                    if (httpGetEntity != null) {
                        pageSourceCode = EntityUtils.toString(httpGetEntity);

                    }


                    if (pageSourceCode == null) {
                        Log.e(Constants.TAG, "[COMMUNICATION THREAD] Error getting the information from the webservice!");
                        return;
                    } else
                        Log.i(Constants.TAG, pageSourceCode);


                    JSONObject content = new JSONObject(pageSourceCode);
                    JSONObject currencies = content.getJSONObject("bpi");
                    JSONObject money = currencies.getJSONObject(informationType);
                    cursInformation = money.get("rate").toString();


                    Log.i("Test", cursInformation);
                    //String temperature = main.getString(Constants.TEMP);

                    serverThread.setData(cursInformation);

                    if (content.toString() == null) {
                        Log.e(Constants.TAG, "[COMMUNICATION THREAD] Curs data is null!");
                        return;
                    }


                    printWriter.println(cursInformation);
                    printWriter.flush();

                } catch (IOException ioException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                    if (Constants.DEBUG) {
                        ioException.printStackTrace();
                    }
                } catch (JSONException jsonException) {
                    Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + jsonException.getMessage());
                    if (Constants.DEBUG) {
                        jsonException.printStackTrace();
                    }
                } finally {
                    if (socket != null) {
                        try {
                            socket.close();
                        } catch (IOException ioException) {
                            Log.e(Constants.TAG, "[COMMUNICATION THREAD] An exception has occurred: " + ioException.getMessage());
                            if (Constants.DEBUG) {
                                ioException.printStackTrace();
                            }
                        }
                    }
                }
            }

        }, 0, 6000);//put here time 1000 milliseconds=1 second
    }
}
