package com.projects.nikita.mysubwaytracker;

import android.net.Uri;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/** Public class MBTAFetcher is uses java.net library to connect to the mbta V3 api via HTTP
 *
 *  If connection to the URL is established, class requests mbta to provide arrival predictions
 *  of the trains to the Community College orange line train station in Boston.
 *  If request is submitted, mbta returns information in the JSON format which is then has
 *  to be parsed in order to retrieve only the needed information. */
public class MBTAFetcher {
    private static String TAG = "MBTAFetcher";
    private static String API_KEY = "40f0e789dd064bfda6f161c5c0d8955e";

    /* Private method established connection with the specified URL which is passed as the argument.
    *  If connected, receives a text representation of the JSON file with the requested information.
    *  @return byte[] */
    private byte[] getUrlBytes(String urlSpec) throws IOException {
        URL url = new URL(urlSpec);
        HttpURLConnection connection = (HttpURLConnection)url.openConnection();
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            InputStream in = connection.getInputStream();
            if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                throw new IOException(connection.getResponseMessage() +
                        ": with " +
                        urlSpec);
            }
            int bytesRead = 0;
            byte[] buffer = new byte[1024];
            while ((bytesRead = in.read(buffer)) > 0) {
                out.write(buffer, 0, bytesRead);
            }
            out.close();
            return out.toByteArray();
        } finally {
            connection.disconnect();
        }
    }

    private String getUrlString(String urlSpec) throws IOException {
        return new String(getUrlBytes(urlSpec));
    }

    /* Creating a url to get the needed information, receive JSON string representation of the
    *  JSON file. Convert this string to the JSONObject and call method to parse retrieved data.
    *  @return String[][] */
    public Date[][] fetchForestHillsArrivals() throws ParseException {
        Date[] arrivalsForestHills;
        Date[] arrivalsOakGrove;

        //2D array to hold data from both arrays above
        Date[][] arrivalsAll = new Date[2][2];
        try {
            // Ask for arrival predictions in the Forest Hills direction
            String urlFH = Uri.parse("https://api-v3.mbta.com/predictions?filter[stop]=70028&include=schedule//data/{index}/attributes/arrival_time")
                    .buildUpon()
                    .build().toString();
            String jsonStringFH = getUrlString(urlFH);
            Log.i(TAG, "Received JSON: " + jsonStringFH);
            JSONObject jsonBodyFH = new JSONObject(jsonStringFH);
            arrivalsForestHills = parseData(jsonBodyFH);

            // Ask for arrival predictions in the Oak Grove direction
            String urlOG = Uri.parse("https://api-v3.mbta.com/predictions?filter[stop]=70029&include=schedule//data/{index}/attributes/arrival_time")
                    .buildUpon()
                    .build().toString();
            String jsonStringOG = getUrlString(urlOG);
            Log.i(TAG, "Received JSON: " + jsonStringOG);
            JSONObject jsonBodyOG = new JSONObject(jsonStringOG);
            arrivalsOakGrove = parseData(jsonBodyOG);

            // Saving both arrays into 2D array of Date objects
            for(int i = 0; i < 2; i++){
                arrivalsAll[i][0] = arrivalsForestHills[i];
                arrivalsAll[i][1] = arrivalsOakGrove[i];
            }
        } catch (IOException ioe) {
            Log.e(TAG, "Failed to fetch items", ioe);
        } catch (JSONException je) {
            Log.e(TAG, "Failed to parse JSON", je);
        }
        return arrivalsAll;
    }

    private Date[] parseData(JSONObject jsonBody) throws JSONException, ParseException {
        JSONArray attributesJsonArray = jsonBody.getJSONArray("data");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
        Date[] arrivals = new Date[2];

        for (int i = 0; i < 2; i++) {
            JSONObject trainJsonObject = attributesJsonArray.getJSONObject(i);
            JSONObject trainAttributesJsonObject = trainJsonObject.getJSONObject("attributes");
            String arrival_time_str = trainAttributesJsonObject.getString("arrival_time").substring(0,10)
                    + ' ' + trainAttributesJsonObject.getString("arrival_time").substring(11,19);

            Log.d(TAG, arrival_time_str);
            Log.d(TAG, "train object " + i + ": " + trainJsonObject.toString());
            Log.d(TAG, "train ATTRIBUTES object " + i + ": " + trainAttributesJsonObject.toString());

            arrivals[i] = format.parse(arrival_time_str);
            Log.d(TAG, arrivals[i].toString());
        }
        return arrivals;
    }
}
