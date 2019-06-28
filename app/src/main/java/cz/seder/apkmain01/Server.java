package cz.seder.apkmain01;

import android.os.Parcel;
import android.os.Parcelable;

public class Server implements Parcelable {

  String uuid;
  String description;

  String group;
  String name;
  String url;

  protected Server(Parcel in) {
    uuid = in.readString();
    description = in.readString();
    group = in.readString();
    name = in.readString();
    url = in.readString();
  }

  public static final Creator<Server> CREATOR = new Creator<Server>() {
    @Override
    public Server createFromParcel(Parcel in) {
      return new Server(in);
    }

    @Override
    public Server[] newArray(int size) {
      return new Server[size];
    }
  };

  public String getUuid() {
    return uuid;
  }

  public void setUuid(String uuid) {
    this.uuid = uuid;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(uuid);
    dest.writeString(description);
    dest.writeString(group);
    dest.writeString(name);
    dest.writeString(url);
  }
}