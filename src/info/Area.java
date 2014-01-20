package info;

public class Area
{
    public String areaName = "";
    public int areaId = 0;
    public int minAP = 2147483647;
    public int maxAP = -2147483648;
    public int exploreProgress = 0;

    public Area()
    {
    }

    public Area(String name, int id, int prog)
    {
        this.areaName = name;
        this.areaId = id;
        this.exploreProgress = prog;
    }
}

/* Location:           C:\MAWalkerMod\MAWalkerMod\
 * Qualified Name:     info.Area
 * JD-Core Version:    0.6.2
 */