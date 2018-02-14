package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class Waypoint extends RobotPlayer
{
	// subclass for target round info
	public static class TargetInfo
	{
		public int 			round;
		public int 			value;
		public MapLocation 	location;
		
		public TargetInfo(MapLocation loc, int val)
		{
			round = rc.getRoundNum();
			value = val;
			location = loc;
		}
	}
	
	public static class TargetStore
	{
		public TargetInfo[] targets = new TargetInfo[5];
		private final int timeout;
		
		public TargetStore(int time)
		{
			timeout = time;
		}
		
		public void add(TargetInfo ti)
		{
			// invalid values for stuff (0 for zombies)
			if (ti.location == null || ti.value == 0)
				return;
			
			int oldestInd = 0;
			int lowestInd = 0;
			
			for (int i=0; i<targets.length; i++)
			{
				if (targets[i] == null)
				{
					targets[i] = ti;
					return;
				}
				if (targets[i].round < targets[oldestInd].round)
					oldestInd = i;
				if (targets[i].value < targets[lowestInd].value)
					lowestInd = i;
			}
			// replace a "dead" one first, if we can
			if (roundsSince(oldestInd) > timeout)
			{
				targets[oldestInd] = ti;
				return;
			}
			// otherwise the one with the lowest value
			if (ti.value > targets[lowestInd].value)
			{
				targets[lowestInd] = ti;
				return;
			}
		}
		
		// removes location from store
		public void remove(MapLocation loc)
		{
			for (int i=0; i<targets.length; i++)
			{
				if (targets[i] == null)
					continue;
				if (targets[i].location.equals(loc))
					targets[i] = null;
			}
		}
		
		public int size()
		{
			int sz = 0;
			for (int i=0; i<targets.length; i++)
				if (targets[i] != null && roundsSince(targets[i].round) < timeout)
					sz++;
			return sz;
		}
		
		public MapLocation getClosestRecent()
		{
			return getClosestRecent(0);
		}

		// attempts to find one larger than min_thresh
		// if it can't, returns closest one
		public MapLocation getClosestRecent(int min_thresh)
		{
			TargetInfo ti = null;
			TargetInfo ti_above = null;
			
			for (int i=0; i<targets.length; i++)
			{
				if (targets[i] == null)
					continue;
				
				if (roundsSince(targets[i].round) > timeout)
					continue;
				
				if (ti == null || here.distanceSquaredTo(targets[i].location) < here.distanceSquaredTo(ti.location))
					ti = targets[i];

				if (targets[i].value >= min_thresh &&
						(ti_above == null || 
						here.distanceSquaredTo(targets[i].location) < here.distanceSquaredTo(ti_above.location)))
					ti_above = targets[i];
			}
			
			if (ti_above != null)
				return ti_above.location;
			if (ti != null)
				return ti.location;
			
			return null;
		}
	}
	
	public static TargetStore enemyTargetStore = new TargetStore(200);
	public static TargetStore friendlyTargetStore = new TargetStore(200);
	
	private static MapLocation randomDest = null;
	private static int randomDestRound = -10000;
	
	public static MapLocation getRandomRetreatWaypoint()
	{
		// we haven't changed it in a while, or we're there
		if (roundsSince(randomDestRound) > 300 || here.distanceSquaredTo(randomDest) < 24)
		{
			randomDest = MapInfo.getRandomLocation();
			// make sure it's not too close to a corner...
			while (MapInfo.closestCornerDistanceSq(randomDest) < 81)
				randomDest = MapInfo.getRandomLocation();
		}
		
		return randomDest;
	}
	
	public static MapLocation getBestRetreatLocation()
	{
		// try retreating to nearby archons first
		MapLocation loc = getClosestFriendlyWaypoint();
		if (loc == null)
			loc = getRandomRetreatWaypoint();
		
		return loc;
	}
	
	public static MapLocation getClosestSafeWaypoint()
	{
		return friendlyTargetStore.getClosestRecent(12);
	}
	
	public static MapLocation getClosestFriendlyWaypoint()
	{
		return friendlyTargetStore.getClosestRecent(6);
	}
	
	public static MapLocation getBestEnemyLocation()
	{
		MapLocation closest = enemyTargetStore.getClosestRecent(1);
		if (closest != null && here.distanceSquaredTo(closest) <= 13)
			enemyTargetStore.remove(closest);
		return closest;
	}
}
