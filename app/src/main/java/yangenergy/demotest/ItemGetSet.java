package yangenergy.demotest;

import java.io.Serializable;

/**
 * Created by Admin on 04-05-2017.
 */
public class ItemGetSet implements Serializable {

    String img_url = "";
    String Title_name = "";
    String Details = "";
    String time = "";

    public String getImg_url() {
        return img_url;
    }

    public void setImg_url(String img_url) {
        this.img_url = img_url;
    }

    public String getTitle_name() {
        return Title_name;
    }

    public void setTitle_name(String title_name) {
        Title_name = title_name;
    }

    public String getDetails() {
        return Details;
    }

    public void setDetails(String details) {
        Details = details;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }
}
