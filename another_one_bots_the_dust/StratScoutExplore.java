package another_one_bots_the_dust;

import battlecode.common.*;


import java.util.*;

public class StratScoutExplore extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;

	// linear distance of the spacing of exploration waypoints
	private static final int EXPLORE_POINT_DIST = 7;
	// square distance of how close we need to be as a scout to 'visit'
	private static final int EXPLORE_VISIT_SQDIST = 8;
	
	private int			myExploringQuadrant;
	private FastLocSet 	myExploringTargets;
	private MapLocation myExploringTarget;
	
	private int lastNewTargetRound = 0;
	
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Explored Q" + myExploringQuadrant + " "
				+ myExploringTargets.elements().size() + " left";
	}
	
	public int getQuadrant(MapLocation loc)
	{
		int quad = (loc.x < MapInfo.mapCenter.x) ? 0 : 1;
		quad |= (loc.y < MapInfo.mapCenter.y) ? 0 : 2;
		return quad;
	}
	
	public StratScoutExplore() throws GameActionException
	{
		// set the quadrant to my build location
		myExploringQuadrant = rand.nextInt(4);

		// first scouts built
		if (rc.getRoundNum() < 50)
		{
			MapLocation[] ourArchons = rc.getInitialArchonLocations(ourTeam);
			int archonInd = 0;
			// find which archon we are closest to
			for (int i=0; i<ourArchons.length; i++)
				if (here.distanceSquaredTo(ourArchons[i]) < here.distanceSquaredTo(ourArchons[archonInd]))
					archonInd = i;
			
			switch (ourArchons.length)
			{
			case 1:
				// set it to explore quadrant we are in
				myExploringQuadrant = getQuadrant(here);
				break;
			case 2:
				// explore ours, or ours rotated by 1
				// (should help with the symmetry)
				myExploringQuadrant = getQuadrant(MapInfo.ourArchonCenter);
				if (archonInd > 0)
					myExploringQuadrant = (myExploringQuadrant+1)%4;
				break;
			case 3:
			case 4:
				// explore the quadrants offset by whichever archon we are closest to
				myExploringQuadrant = getQuadrant(MapInfo.ourArchonCenter);
				myExploringQuadrant = (myExploringQuadrant+archonInd)%4;
				break;
			default:
				break;
			}
		}
		
		resetTargets();
	}
	
	private void resetTargets()
	{
		myExploringTargets = new FastLocSet();
		// populate myExploringTargets with an array of locations
		// in the specified map quadrant that the scout has to visit within
		// EXPLORE_VISIT_SQDIST units to remove
		
		MapLocation minpt = MapInfo.mapMin.add(EXPLORE_POINT_DIST/2,EXPLORE_POINT_DIST/2);
		MapLocation maxpt = MapInfo.mapMax.add(-EXPLORE_POINT_DIST/2,-EXPLORE_POINT_DIST/2);
		MapLocation stoppt = MapInfo.mapCenter.add(EXPLORE_POINT_DIST/2,EXPLORE_POINT_DIST/2);
		
		boolean flipX = ((myExploringQuadrant&1)==0);
		boolean flipY = ((myExploringQuadrant/2)==0);
		
		int dx = flipX ? EXPLORE_POINT_DIST : -EXPLORE_POINT_DIST;
		int dy = flipY ? EXPLORE_POINT_DIST : -EXPLORE_POINT_DIST;
		
		int x = flipX ? minpt.x : maxpt.x;
		for (int xl=minpt.x; xl<stoppt.x; xl += EXPLORE_POINT_DIST)
		{
			int y = flipY ? minpt.y : maxpt.y;
			for (int yl=minpt.y; yl<stoppt.y; yl += EXPLORE_POINT_DIST)
			{
				// add the point
				myExploringTargets.add(new MapLocation(x,y));
				y += dy;
			}
			x += dx;
		}
		
		// tell it to go to the first target, which is the corner
		myExploringTarget = myExploringTargets.elements().get(0);
		lastNewTargetRound = rc.getRoundNum();		
	}
	
	private void updateTargets()
	{
		// first, remove any targets within distance yay
		Iterator<MapLocation> it = myExploringTargets.elements().iterator();
		while (it.hasNext())
		{
			MapLocation loc = it.next();
			if (here.distanceSquaredTo(loc) <= EXPLORE_VISIT_SQDIST)
			{
				it.remove();
				myExploringTargets.remove(loc);
			}
		}
		
		// now do we need to update my target?
		// if it's off the map, re-initialize
		if (!MapInfo.isOnMap(myExploringTarget))
		{
			resetTargets();
		}

		// no targets for me? reset all of them
		if (myExploringTargets.elements().size() == 0)
		{
			myExploringQuadrant = (myExploringQuadrant+1)%4;
			resetTargets();
		}
		
		if (myExploringTarget == null || !myExploringTargets.contains(myExploringTarget) ||
				roundsSince(lastNewTargetRound) > 100)
		{
			int ind = rand.nextInt(myExploringTargets.elements().size());
			myExploringTarget = myExploringTargets.elements().get(ind);
			lastNewTargetRound = rc.getRoundNum();
		}
	}
	
	public boolean tryTurn() throws GameActionException
	{
		// do we have a strategy that takes precedence over this one?
		if (overrideStrategy != null)
		{
			if (overrideStrategy.tryTurn())
				return true;
			else
				overrideStrategy = null;
		}

		if (StratScoutTurrets.shouldScoutTurrets())
		{
			overrideStrategy = new StratScoutTurrets();
			overrideStrategy.tryTurn();
			return true;
		}
		
		// update all of our targets, visited and otherwise
		updateTargets();

		// try to go to the target with the best dirs possible
		Nav.tryGoTo(myExploringTarget, Micro.getBestAnyDirs());

		return true;
	}
}
