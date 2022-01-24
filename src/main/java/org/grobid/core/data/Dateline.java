package org.grobid.core.data;

import org.grobid.core.engines.EngineMedicalParsers;
import org.grobid.core.layout.LayoutToken;
import org.grobid.core.utilities.TextUtilities;

import java.util.*;

import static org.apache.commons.collections4.CollectionUtils.isNotEmpty;

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
    private List<LayoutToken> layoutTokens = new ArrayList<>();

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
        return this.placeName;
    }

    public void setPlaceName(String place) { placeName = place;}

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

    public List<LayoutToken> getLayoutTokens() {
        return layoutTokens;
    }

    public void setLayoutTokens(List<LayoutToken> tokens) {
        this.layoutTokens = tokens;
    }

    /**
     * TEI serialization via xom.
     */
    public void addLayoutTokens(List<LayoutToken> theTokens) {
        if (layoutTokens == null) {
            layoutTokens = new ArrayList<LayoutToken>();
        }
        layoutTokens.addAll(theTokens);
    }


    /**
     *  Remove invalid dates (and convert the date format into the format dd/mm/yyyy)
     */
    public static List<Dateline> sanityCheck(List<Dateline> datelines) {
        if (datelines == null)
            return null;
        if (datelines.size() == 0)
            return datelines;

        List<Dateline> result = new ArrayList<>();

        for(Dateline dateline : datelines) {
            if (dateline.getDate() != null && dateline.getDate().trim().length() != 0) {
                String originalDate = dateline.getDate();
                String date = originalDate.replaceAll("."," ").replaceAll("\\s+","/");
                dateline.setDate(date);
                result.add(dateline);
            }
        }

        return result;
    }
}
