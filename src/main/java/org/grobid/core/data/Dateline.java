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
    private String note = null;

    public Dateline() {
    }

    public Dateline(Dateline fromDateline) {
        this.placeName = fromDateline.placeName;
        this.date = fromDateline.date;
        this.time = fromDateline.time;
        this.timeString = fromDateline.timeString;
        this.note = fromDateline.note;
    }

    public String getPlaceName() {
        return placeName;
    }

    public void setPlaceName(String place) {
        placeName = place;
    }

    public String getDate() { return date; }

    public void setDate(String d) { date = d; }

    public int getTime() { return time; }

    public void setTime(int t) { time = t; }

    public String getTimeString() {
        return timeString;
    }

    public void setTimeString(String ts) {
        timeString = ts;
    }

    public String getNote() { return note; }

    public void setNote(String nt) { note = nt; }

    public boolean isNotNull() {
        return (placeName != null) ||
            (date != null) ||
            (timeString != null) ||
            (time != -1)  ||
            (note != null);
    }
}
