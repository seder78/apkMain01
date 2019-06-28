package cz.seder.apkmain01;

import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class MainActivity extends AppCompatActivity {

  private Server[] servers = null;
  private int progressStatus = 0;
  private int order = 0;
  private Handler handler = new Handler();
  TableLayout tableLayout = null;


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    refreshWithFrame();
  }


  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.main_menu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {

    switch (item.getItemId()) {
      case R.id.menu_refresh: {
        refreshWithFrame();
        return true;
      }
      case R.id.menu_servers: {
        Intent intent = new Intent(this, ServerListActivity.class);
        intent.putExtra("servers", servers);
        startActivity(intent);

        return true;
      }
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void refreshWithFrame() {
    tableLayout = findViewById(R.id.table_layout);
    servers = null;
    tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
    tableLayout.invalidate();
    tableLayout.refreshDrawableState();

    String filename = "servers.json";


    final ProgressBar pb = (ProgressBar) findViewById(R.id.progressBar);
    final TextView tv = (TextView) findViewById(R.id.tv);
    final FrameLayout fl = (FrameLayout) findViewById(R.id.frame);
    progressStatus = 0;
    order = 0;

    if (servers == null) {
      FileOutputStream outputStream;
      FileInputStream in = null;
      try {
        in = openFileInput(filename);
      } catch (Exception e) {
      }

      if (in != null) {
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
        } catch (JSONException e) {
          e.printStackTrace();
        }


        servers = serverList.toArray(new Server[serverList.size()]);
      }
    }

    fl.setVisibility(View.GONE);
    if (servers != null && servers.length > 0) {
      fl.setVisibility(View.VISIBLE);
      new Thread(new Runnable() {

        @Override
        public void run() {
          int pct = 100 / servers.length;


          for (final Server server : servers) {
            final ArrayList<MyTableRow> rows = new ArrayList<>();
            progressStatus += pct;
            order++;
            // Update the progress bar
            handler.post(new Runnable() {
              @Override
              public void run() {
                pb.setProgress(progressStatus);
                tv.setText((order + "/" + servers.length) + " - " + server.getName());
              }
            });

            URL obj = null;
            try {
              obj = new URL(server.getUrl() + "/rest/status");
            } catch (MalformedURLException e) {
              e.printStackTrace();
              continue;
            }
            HttpURLConnection con = null;
            try {
              con = (HttpURLConnection) obj.openConnection();
            } catch (IOException e) {
              rows.add(createErrRow(server));
              continue;
            }
            con.setRequestProperty("Accept", "application/json");
            con.setRequestProperty("Content-Type", "application/json");

            try {
              con.setRequestMethod("GET");
            } catch (ProtocolException e) {
              rows.add(createErrRow(server));
              continue;
            }

            try {
              con.setConnectTimeout(2000);
              int code = con.getResponseCode();
              if (code != 200) {
                rows.add(createErrRow(server));
                continue;
              }
            } catch (IOException e) {
              rows.add(createErrRow(server));
              continue;
            }

            StringBuffer rawBuffer = new StringBuffer();
            String line;

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
              while ((line = reader.readLine()) != null) {
                rawBuffer.append(line);
              }
            } catch (IOException e) {
            }

            try {
              JSONObject reader = new JSONObject(rawBuffer.toString());
              JSONArray jsonArray = reader.getJSONArray("domains");
              for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                MyTableRow row = new MyTableRow();
                row.setRefresh(new SimpleDateFormat("HH:mm:ss").format(new Date()));
                row.setServer(server.getName());
                row.setSite(jsonObject.getString("site"));
                row.setStage(jsonObject.getString("stage"));
                row.setStatus(jsonObject.getString("status"));
                JSONArray tickets = jsonObject.getJSONArray("tickets");
                if (tickets != null) {
                  row.setTickets(String.valueOf(tickets.length()));
                }
                rows.add(row);
              }
            } catch (JSONException e) {
              e.printStackTrace();
            }

            try {
              Thread.sleep(20);
            } catch (InterruptedException e) {
              e.printStackTrace();
            }

            // Update the progress bar
            handler.post(new Runnable() {
              @Override
              public void run() {
//              pb.setProgress(progressStatus);
//              tv.setText((order + "/" + servers.length) + " - " + server.getName());
                if (order == servers.length) {
                  fl.setVisibility(View.GONE);
                }
                createTableRows(tableLayout, rows);
              }
            });

          }
        }
      }).start();
    }
  }

  private void refresh() {
    servers = null;
    TableLayout tableLayout = findViewById(R.id.table_layout);
    tableLayout.removeViews(1, tableLayout.getChildCount() - 1);
    tableLayout.invalidate();
    tableLayout.refreshDrawableState();

    String filename = "servers.json";
//    String string = "[\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud\",\n" +
//      "    \"name\": \"Renti.cz\",\n" +
//      "    \"url\": \"http://90.183.26.31:9144\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud\",\n" +
//      "    \"name\": \"demo.jcaris.cz\",\n" +
//      "    \"url\": \"http://demo.jcaris.cz:9124\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Private\",\n" +
//      "    \"name\": \"CarIS @ Golem\",\n" +
//      "    \"url\": \"http://golem:9124\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Private\",\n" +
//      "    \"name\": \"CarIS @ Yetti\",\n" +
//      "    \"url\": \"http://yetti:9124\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Localhost\",\n" +
//      "    \"name\": \"CarIS @ SAD_I7U\",\n" +
//      "    \"url\": \"http://localhost:9124\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"uschovna_demo_0\",\n" +
//      "    \"url\": \"http://90.183.26.43:9136\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"uschovna_demo_1\",\n" +
//      "    \"url\": \"http://90.183.26.43:9138\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"uschovna_demo_2\",\n" +
//      "    \"url\": \"http://90.183.26.43:9140\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"bazar_demo_0\",\n" +
//      "    \"url\": \"http://90.183.26.43:9130\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"bazar_demo_1\",\n" +
//      "    \"url\": \"http://90.183.26.43:9132\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"bazar_demo_2\",\n" +
//      "    \"url\": \"http://90.183.26.43:9134\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"autopujcovna_demo_0\",\n" +
//      "    \"url\": \"http://90.183.26.43:9124\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"autopujcovna_demo_1\",\n" +
//      "    \"url\": \"http://90.183.26.43:9126\"\n" +
//      "  },\n" +
//      "  {\n" +
//      "    \"group\": \"Cloud-Docker\",\n" +
//      "    \"name\": \"autopujcovna_demo_2\",\n" +
//      "    \"url\": \"http://90.183.26.43:9128\"\n" +
//      "  }\n" +
//      "]";

    if (servers == null) {
      FileOutputStream outputStream;
      FileInputStream in = null;
      try {
        in = openFileInput(filename);
      } catch (Exception e) {
//        try {
//          outputStream = openFileOutput(filename, Context.MODE_PRIVATE);
//          outputStream.write(string.getBytes());
//          outputStream.close();
//          in = openFileInput(filename);
//        } catch (FileNotFoundException e1) {
//          e1.printStackTrace();
//        } catch (IOException e1) {
//          e1.printStackTrace();
//        }
      }

      if (in != null) {
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
        } catch (JSONException e) {
          e.printStackTrace();
        }


        servers = serverList.toArray(new Server[serverList.size()]);
      }
    }
    if (servers != null && servers.length > 0) {
      RetrieveFeedTask task = new RetrieveFeedTask();
      task.execute(servers);
      try {
        List<MyTableRow> rows = task.get();
        createTableRows(tableLayout, rows);
      } catch (ExecutionException e) {
        e.printStackTrace();
      } catch (InterruptedException e) {
        e.printStackTrace();
      }
    }

  }

  public Server[] getServers() {
    return servers;
  }

  private void createTableRows(TableLayout tableLayout, List<MyTableRow> rows) {
    for (MyTableRow row : rows) {
      TableRow tr1 = new TableRow(this);
      if (tableLayout.getChildCount() % 2 == 0) tr1.setBackgroundColor(Color.parseColor("#9bd3e7"));
      {
        TextView view = new TextView(this);
        view.setPadding(4, 0, 0, 0);
        view.setText(row.getRefresh());
        tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
      }
      {
        TextView view = new TextView(this);
        view.setPadding(4, 0, 0, 0);
        view.setText(row.getServer());
        tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
      }
      {
        TextView view = new TextView(this);
        view.setPadding(4, 0, 0, 0);
        view.setText(row.getSite());
        tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
      }
      {
        TextView view = new TextView(this);
        view.setPadding(4, 0, 0, 0);
        view.setText(row.getStage());
        tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
      }
      {
        TextView view = new TextView(this);
        view.setPadding(4, 0, 0, 0);
        view.setText(row.getStatus());
        tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
      }
      {
        TextView view = new TextView(this);
        view.setPadding(4, 0, 0, 0);
        view.setText(row.getTickets());
        tr1.addView(view, new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT, 1));
      }
      tableLayout.addView(tr1, new TableLayout.LayoutParams(TableLayout.LayoutParams.MATCH_PARENT, TableLayout.LayoutParams.MATCH_PARENT));
    }
  }

  private MyTableRow createErrRow(Server server) {
    MyTableRow row = new MyTableRow();
    row.setRefresh(new SimpleDateFormat("HH:mm:ss").format(new Date()));
    row.setServer(server.getName());
    row.setStatus("Not connection");
    return row;
  }

  class RetrieveFeedTask extends AsyncTask<Server, Void, List<MyTableRow>> {
    protected List<MyTableRow> doInBackground(Server... servers) {
      ArrayList<MyTableRow> rows = new ArrayList<>();
      for (Server server : servers) {
        URL obj = null;
        try {
          obj = new URL(server.getUrl() + "/rest/status");
        } catch (MalformedURLException e) {
          e.printStackTrace();
          continue;
        }
        HttpURLConnection con = null;
        try {
          con = (HttpURLConnection) obj.openConnection();
        } catch (IOException e) {
          rows.add(createErrRow(server));
          continue;
        }
        con.setRequestProperty("Accept", "application/json");
        con.setRequestProperty("Content-Type", "application/json");

        try {
          con.setRequestMethod("GET");
        } catch (ProtocolException e) {
          rows.add(createErrRow(server));
          continue;
        }

        try {
          con.setConnectTimeout(2000);
          int code = con.getResponseCode();
          if (code != 200) {
            rows.add(createErrRow(server));
            continue;
          }
        } catch (IOException e) {
          rows.add(createErrRow(server));
          continue;
        }

        StringBuffer rawBuffer = new StringBuffer();
        String line;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream(), "UTF-8"))) {
          while ((line = reader.readLine()) != null) {
            rawBuffer.append(line);
          }
        } catch (IOException e) {
        }

        try {
          JSONObject reader = new JSONObject(rawBuffer.toString());
          JSONArray jsonArray = reader.getJSONArray("domains");
          for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            MyTableRow row = new MyTableRow();
            row.setRefresh(new SimpleDateFormat("HH:mm:ss").format(new Date()));
            row.setServer(server.getName());
            row.setSite(jsonObject.getString("site"));
            row.setStage(jsonObject.getString("stage"));
            row.setStatus(jsonObject.getString("status"));
            JSONArray tickets = jsonObject.getJSONArray("tickets");
            if (tickets != null) {
              row.setTickets(String.valueOf(tickets.length()));
            }
            rows.add(row);
          }
        } catch (JSONException e) {
          e.printStackTrace();
        }

      }

      return rows;
    }

    private MyTableRow createErrRow(Server server) {
      MyTableRow row = new MyTableRow();
      row.setRefresh(new SimpleDateFormat("HH:mm:ss").format(new Date()));
      row.setServer(server.getName());
      row.setStatus("Not connection");
      return row;
    }


    protected void onPostExecute(Boolean feed) {
      // TODO: check this.exception
      // TODO: do something with the feed
    }
  }

  class MyTableRow {
    String refresh;
    String server;
    String site;
    String stage;
    String status;
    String tickets;

    public String getRefresh() {
      return refresh;
    }

    public void setRefresh(String refresh) {
      this.refresh = refresh;
    }

    public String getServer() {
      return server;
    }

    public void setServer(String server) {
      this.server = server;
    }

    public String getSite() {
      return site;
    }

    public void setSite(String site) {
      this.site = site;
    }

    public String getStage() {
      return stage;
    }

    public void setStage(String stage) {
      this.stage = stage;
    }

    public String getStatus() {
      return status;
    }

    public void setStatus(String purpose) {
      this.status = purpose;
    }

    public String getTickets() {
      return tickets;
    }

    public void setTickets(String tickets) {
      this.tickets = tickets;
    }
  }
}
