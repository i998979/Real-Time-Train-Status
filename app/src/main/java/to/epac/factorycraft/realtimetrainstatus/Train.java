package to.epac.factorycraft.realtimetrainstatus;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Train {
    public String line;
    public String dir;
    public String station;
    public String seq;
    public String time;
    public String dest;
    public String plat;
    public String ttnt;
    public String timetype;
    public String route;
    public String currtime;

    public Train(String line, String dir, String station, String seq, String time, String dest, String plat, String ttnt, String timetype, String route, String currtime) {
        this.line = line;
        this.dir = dir;
        this.station = station;
        this.seq = seq;
        this.time = time;
        this.dest = dest;
        this.plat = plat;
        this.ttnt = ttnt;
        this.timetype = timetype;
        this.route = route;

        try {
            Instant instant = Instant.parse(currtime);
            LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
            this.currtime = localDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (DateTimeParseException e) {
            this.currtime = currtime;
        }
    }
}