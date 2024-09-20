package to.epac.factorycraft.realtimetrainstatus;

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

    public Train(String line, String dir, String station, String seq, String time, String dest, String plat, String ttnt, String timetype, String route) {
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
    }
}