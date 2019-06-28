package cz.seder.apkmain01;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.*;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class ServerListActivity extends AppCompatActivity {

  @SuppressLint("ResourceType")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_server_list);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);

    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    refresh();
  }

  private void refresh() {
    TableLayout tableLayout = findViewById(R.id.table_servers);
    tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
    Parcelable[] servers = getIntent().getParcelableArrayExtra("servers");
    if (servers != null && servers.length > 0) {
      for (int i = 0; i < servers.length; i++) {
        Server server = (Server) servers[i];
        TableRow tr1 = new TableRow(this);
        if (tableLayout.getChildCount() % 2 == 0) tr1.setBackgroundColor(Color.parseColor("#9bd3e7"));
        {
          TextView view = new TextView(this);
          view.setPadding(4, 0, 0, 0);
          view.setText(server.getName());
          tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
        }
        {
          TextView view = new TextView(this);
          view.setPadding(4, 0, 0, 0);
          view.setText(server.getUrl());
          tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
        }
        tableLayout.addView(tr1, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
      }
    }
  }

  @Override
  protected void onStart() {
    super.onStart();
    getParent();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.server_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case R.id.menu_server_url: {
        loadServersFromUrl();
        return true;
      }
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void loadServersFromUrl() {
    try {
      StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
      StrictMode.setThreadPolicy(policy);

      URL url = new URL("http://seder.cz/android/servers.json");

      InputStream is = url.openStream();

      DataInputStream dis = new DataInputStream(is);

      byte[] buffer = new byte[1024];
      int length;

      FileOutputStream fos = openFileOutput("servers.json", Context.MODE_PRIVATE);

      while ((length = dis.read(buffer))>0) {
        fos.write(buffer, 0, length);
      }

      FileInputStream in = openFileInput("servers.json");
      InputStreamReader inputStreamReader = new InputStreamReader(in);
      BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
      StringBuilder sb = new StringBuilder();
      String line;
      try {
        while (true) {
          if (!((line = bufferedReader.readLine()) != null)) break;
          sb.append(line);
        }
      } catch (IOException e) {
        e.printStackTrace();
      }

      ArrayList<Server> serverList = new ArrayList<>();
      try {
        JSONArray jsonSrvs = new JSONArray(sb.toString());
        for (int i = 0; i < jsonSrvs.length(); i++) {
          JSONObject jsonServer = jsonSrvs.getJSONObject(i);
          Server server = new Server(Parcel.obtain());
          server.setName(jsonServer.getString("name"));
          server.setUrl(jsonServer.getString("url"));
          serverList.add(server);
        }
        getIntent().putExtra("servers", serverList.toArray(new Server[serverList.size()]));
      } catch (JSONException e) {
        e.printStackTrace();
      }
      is.close();
      inputStreamReader.close();
      bufferedReader.close();
    } catch (MalformedURLException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    refresh();
  }
}
