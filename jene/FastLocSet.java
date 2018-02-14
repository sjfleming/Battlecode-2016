package jene;

import battlecode.common.*;
import java.util.*;

public class FastLocSet
{
    private static final int HASH = Math.max(GameConstants.MAP_MAX_WIDTH, GameConstants.MAP_MAX_HEIGHT);
    private int[][] value = new int[HASH][HASH];
    private List<MapLocation> locations = null;
    private boolean isUsingList = false;
    
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
        int x = loc.x % HASH;
        int y = loc.y % HASH;
        if (value[x][y] == 0)
        {
            value[x][y] = 1;
            if (isUsingList)
            	locations.add(loc);
        }
    }
    
    public void add(MapLocation loc, int val)
    {
        int x = loc.x % HASH;
        int y = loc.y % HASH;
        if (value[x][y] != val)
        {
            value[x][y] = val;
            if (isUsingList)
            	locations.add(loc);
        }
    }

    public void remove(MapLocation loc)
    {
        int x = loc.x % HASH;
        int y = loc.y % HASH;
        if (value[x][y] > 0)
        {
            value[x][y] = 0;
            if (isUsingList)
            	locations.remove(loc);
        }
    }
    
    public int get(MapLocation loc)
    {
    	return value[loc.x%HASH][loc.y%HASH];
    }

    public boolean contains(MapLocation loc)
    {
        return value[loc.x % HASH][loc.y % HASH] > 0;
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