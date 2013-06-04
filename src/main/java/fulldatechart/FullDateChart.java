package fulldatechart;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.Version;
import net.fortuna.ical4j.util.UidGenerator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FullDateChart {

    static class Slot implements Comparable<Slot> {
        final int month;
        final int day;

        public Slot(DateTime dt) {
            month = dt.getMonthOfYear();
            day = dt.getDayOfMonth();
        }

        @Override
        public int hashCode() {
            return toString().hashCode();
        }

        /**
         * Returns the next occurrence of this slot.
         * 
         * @return
         */
        public DateTime getNextOccurrence() {
            DateTime slot = DateTime.now().withMonthOfYear(month)
                    .withDayOfMonth(day).withTimeAtStartOfDay();
            if (slot.isBeforeNow())
                return slot.plusYears(1).withTimeAtStartOfDay();
            else
                return slot;
        }

        @Override
        public boolean equals(Object other) {
            return month == ((Slot) other).month && day == ((Slot) other).day;
        }

        @Override
        public String toString() {
            return String.format("%02d-%02d", month, day);
        }

        public int compareTo(Slot o) {
            if (o.month != month)
                return Integer.compare(month, o.month);
            else
                return Integer.compare(day, o.day);
        }
    }

    public static void main(String[] args) throws IOException, ParseException,
            ValidationException {
        if (args.length != 2) {
            System.out
                    .println("Usage: .../fulldatechart.jar /path/to/myfinds.gpx <myuserid>");
            System.exit(1);
        }

        File f = new File(args[0]);
        String ownerId = args[1];

        DateTimeFormatter fmt = DateTimeFormat.forPattern(
                "yyyy-MM-dd'T'HH:mm:ss'Z'").withZoneUTC();

        System.out.println("Parsing: " + f);
        Document document = Jsoup.parse(f, "UTF-8");

        Set<Slot> dates = new HashSet<Slot>();

        for (int i = 1; i <= 366; i++) {
            dates.add(new Slot(new DateTime().withYear(2012)
                    .withTimeAtStartOfDay().withDayOfYear(i)));
        }

        for (Element elem : document.select("groundspeak|date")) {
            Elements siblings = elem.siblingElements();
            String type = siblings.select("groundspeak|type").text();

            if ((type.equals("Found it") || type.equals("Attended"))
                    && siblings.select("groundspeak|finder").attr("id")
                            .equals(ownerId)) {
                DateTime dt = fmt.parseDateTime(elem.text());

                /*
                 * This seems to be necessary to get all log dates to conform to
                 * what's displayed on geocaching.com. Certain logs which have a
                 * date between 0000 and 0600 should really belong to the
                 * previous day.
                 */
                dt = dt.minusHours(6);
                dates.remove(new Slot(dt));
            }
        }

        Calendar cal = new Calendar();
        cal.getProperties()
                .add(new ProdId("-//JesperEs/fulldatechart 1.0//EN"));
        cal.getProperties().add(Version.VERSION_2_0);
        cal.getProperties().add(CalScale.GREGORIAN);

        UidGenerator ug = new UidGenerator("1");

        List<Slot> list = new ArrayList<Slot>();
        list.addAll(dates);
        Collections.sort(list);

        for (Slot slot : list) {
            DateTime dt = slot.getNextOccurrence();
            System.out.format("Next occurrence of empty slot: %s -> %s\n",
                    slot, dt);
            VEvent event = new VEvent(new Date(dt.withHourOfDay(19).toDate()),
                    "Cache-day");
            event.getProperties().add(ug.generateUid());
            cal.getComponents().add(event);
        }

        File output = new File("fulldatechart.ics");
        FileOutputStream fout = new FileOutputStream(output);
        try {
            CalendarOutputter out = new CalendarOutputter();
            out.output(cal, fout);
            System.out.println("Calendar written to "
                    + output.getAbsolutePath());
        } finally {
            fout.close();
        }
    }
}
