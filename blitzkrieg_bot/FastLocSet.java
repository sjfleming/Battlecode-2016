package blitzkrieg_bot;

import battlecode.common.*;
import java.util.*;

public class FastLocSet
{
    private static final int HASH = Math.max(GameConstants.MAP_MAX_WIDTH, GameConstants.MAP_MAX_HEIGHT);
    private int[][] value = new int[HASH][HASH];
    private List<MapLocation> locations = null;
    public boolean isUsingList = false;
    
    // default constructor, use the arrayList
    public FastLocSet()
    {
    	this(true);
    }
    
    public FastLocSet(boolean useList)
    {
    	if (useList)
    		locations = new ArrayList<MapLocation>();
    	this.isUsingList = useList;
    }

    public void add(MapLocation loc)
    {
        this.add(loc, 1);
    }
    
    public void add(MapLocation loc, int val)
    {
    	if (loc == null)
    		return;
    	
        int x = (loc.x+HASH) % HASH;
        int y = (loc.y+HASH) % HASH;
        if (value[x][y] == 0)
        {
            value[x][y] = val;
            if (isUsingList)
            	locations.add(loc);
        }
    }
    
    public void addOrSet(MapLocation loc, int val)
    {
    	if (loc == null)
    		return;
    	
        int x = (loc.x+HASH) % HASH;
        int y = (loc.y+HASH) % HASH;

        if (isUsingList && value[x][y] <= 0)
        	locations.add(loc);
    
        value[x][y] = val;
    }
    
    public void set(MapLocation loc, int val)
    {
        value[(loc.x+HASH) % HASH][(loc.y+HASH) % HASH] = val;
    }

    public int get(MapLocation loc)
    {
    	return value[(loc.x+HASH) % HASH][(loc.y+HASH) % HASH];
    }

    public void remove(MapLocation loc)
    {
        int x = (loc.x+HASH) % HASH;
        int y = (loc.y+HASH) % HASH;
        if (value[x][y] > 0)
        {
            value[x][y] = -1;
            if (isUsingList)
            	locations.remove(loc);
        }
    }
    
    public boolean contains(MapLocation loc)
    {
        return value[(loc.x+HASH) % HASH][(loc.y+HASH) % HASH] > 0;
    }
    
    public void clear()
    {
        value = new int[HASH][HASH];
        if (isUsingList)
        	locations.clear();
    }
    
    public List<MapLocation> elements()
    {
    	return locations;
    }
}