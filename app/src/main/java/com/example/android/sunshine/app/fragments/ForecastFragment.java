package com.example.android.sunshine.app.fragments;

import android.app.ProgressDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.example.android.sunshine.app.R;
import com.example.android.sunshine.app.activities.DetailActivity;
import com.example.android.sunshine.app.utils.Constants;
import com.example.android.sunshine.app.utils.WeatherDataParser;

import org.json.JSONException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * A forecast fragment  containing a simple view.
 */
public class ForecastFragment extends Fragment {

    private ListView listView;
    ArrayAdapter<String> forecastAdapter;
    public ForecastFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        listView = (ListView) rootView.findViewById(R.id.listView_forecast);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                String forecast = forecastAdapter.getItem(position);
                Toast.makeText(getActivity(), "Forecast : " + forecast, Toast.LENGTH_SHORT).show();
                Intent detailIntent = new Intent(getActivity(), DetailActivity.class);
                detailIntent.putExtra(Constants.FORECAST_DETAIL,forecast);
                startActivity(detailIntent);
            }
        });
        Toast.makeText(getActivity(), "Fetching Weather information.", Toast.LENGTH_SHORT).show();
        return rootView;
    }

    private void updateWeather() {
        FetchWeatherTask fetchWeatherTask = new FetchWeatherTask();
        String location = PreferenceManager.getDefaultSharedPreferences(getActivity()).getString(
                getString(R.string.pref_location_key),getString(R.string.pref_location_default));
        fetchWeatherTask.execute(location);
    }
    @Override
    public void onStart() {
        super.onStart();
        updateWeather();
    }

    class FetchWeatherTask extends AsyncTask<String, Void, String[]>
    {
        ProgressDialog progressDialog;
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog = new ProgressDialog(getActivity()); //Here I get an error: The constructor ProgressDialog(PFragment) is undefined
            progressDialog.setMessage("Loading..");
            progressDialog.setTitle("Fetching Data");
            progressDialog.setIndeterminate(false);
            progressDialog.setCancelable(true);
            progressDialog.show();
        }

        @Override
        protected String[] doInBackground(String... strings) {
            String weatherJson = getWeatherDataFromServer(strings[0]);
            try {
                return  WeatherDataParser.getWeatherDataFromJson(getActivity().
                        getApplicationContext(), weatherJson, 7);
            } catch (JSONException e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(String s[]) {
            super.onPostExecute(s);
            if(s != null) {
                forecastAdapter = new ArrayAdapter<>(getActivity(),
                        R.layout.list_item_forecast, R.id.tvForcast,s);
                listView.setAdapter(forecastAdapter);
                progressDialog.dismiss();
            } else {
                Toast.makeText(getActivity(), "Problem in fetching data.", Toast.LENGTH_SHORT).show();
                progressDialog.dismiss();
            }
            Log.d("WEATHER_DATA","Data : "+ s);
        }


        private String getWeatherDataFromServer(String postalCode) {

            // These two need to be declared outside the try/catch
            // so that they can be closed in the finally block.
            HttpURLConnection urlConnection = null;
            BufferedReader reader = null;

            // Will contain the raw JSON response as a string.
            String forecastJsonStr = null;

            try {
                // Construct the URL for the OpenWeatherMap query
                // Possible parameters are avaiable at OWM's forecast API page, at
                // http://openweathermap.org/API#forecast

                String format = "json";
                String units = "metric";
                int numDays = 7;


                final String FORECAST_BASE_URL = "http://api.openweathermap.org/data/2.5/forecast/daily?";




                Uri builtUri = Uri.parse(FORECAST_BASE_URL).buildUpon()
                        .appendQueryParameter(Constants.QUERY_PARAM, postalCode)
                        .appendQueryParameter(Constants.FORMAT_PARAM, format)
                        .appendQueryParameter(Constants.UNITS_PARAM, units)
                        .appendQueryParameter(Constants.DAYS_PARAM, Integer.toString(numDays))
                        .appendQueryParameter(Constants.APPID_PARAM, Constants.OPEN_WEATHER_MAP_API_KEY)
                        .build();

                URL url = new URL(builtUri.toString());

               /* URL url = new URL("http://api.openweathermap.org/data/2.5/forecast/daily" +
                        "?q="+ postalCode +"&mode=json&units=metric&cnt=7" +
                        "&APPID=514700d44223c1e418709b9887f9bdd4");
*/
                // Create the request to OpenWeatherMap, and open the connection
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.connect();

                // Read the input stream into a String
                InputStream inputStream = urlConnection.getInputStream();
                StringBuffer buffer = new StringBuffer();
                if (inputStream == null) {
                    // Nothing to do.
                    return null;
                }
                reader = new BufferedReader(new InputStreamReader(inputStream));

                String line;
                while ((line = reader.readLine()) != null) {
                    // Since it's JSON, adding a newline isn't necessary (it won't affect parsing)
                    // But it does make debugging a *lot* easier if you print out the completed
                    // buffer for debugging.
                    buffer.append(line + "\n");
                }

                if (buffer.length() == 0) {
                    // Stream was empty.  No point in parsing.
                    return null;
                }
                forecastJsonStr = buffer.toString();
            } catch (IOException e) {
                Log.e("ForecastFragment", "Error ", e);
                // If the code didn't successfully get the weather data, there's no point in attemping
                // to parse it.
                return null;
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (final IOException e) {
                        Log.e("ForecastFragment", "Error closing stream", e);
                    }
                }
            }
            return forecastJsonStr;

        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.forecast_fragment, menu);

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(item.getItemId() ==  R.id.action_refresh) {
            Toast.makeText(getActivity(), "Fetching Weather information.", Toast.LENGTH_SHORT).show();
            new FetchWeatherTask().execute("411014");

            FetchWeatherTask weatherTask = new FetchWeatherTask();
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
            String location = prefs.getString(getString(R.string.pref_location_key),
                    getString(R.string.pref_location_default));
            weatherTask.execute(location);
        }
        return super.onOptionsItemSelected(item);

    }
}
