package org.grobid.core.data;

import org.grobid.core.utilities.TextUtilities;

/**
 * Class for representing a dateline.
 * For comparing dates by strict time flow, please use java.util.Date + java.util.Calendar (see the Grobid documentation concerning Date)
 */
public class Dateline  {
    private String placeName = null;
    private String date = null;
    private int time = -1;
    private String timeString = null;

    public Dateline() {
    }

    public Dateline(Dateline fromDateline) {
        this.placeName = fromDateline.placeName;
        this.date = fromDateline.date;
        this.time = fromDateline.time;
        this.timeString = fromDateline.timeString;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String placeName) {
        this.placeName = placeName;
    }

    public String getDate() { return date; }

    public void setDate(String date) { this.date = date; }

    public int getTime() { return time; }

    public void setTime(int t) { time = t; }

    public String getTimeString() {
        return this.timeString;
    }

    public void setTimeString(String timeString) {
        this.timeString = timeString;
    }

    public boolean isNotNull() {
        return (placeName != null) ||
            (date != null) ||
            (time != -1);
    }
}
