package jene;

import battlecode.common.*;


import java.util.*;

public class ExploreStrat extends RobotPlayer implements Strategy
{	
	private String stratName;
	public static MapLocation myExploringTarget;
	
	public ExploreStrat() throws GameActionException
	{
		int id = rc.getID();
		int rando = rand.nextInt(4);
		if (rando == 0)
			myExploringTarget = here.add(-100,-100);
		else if (rando == 1)
			myExploringTarget = here.add(100,-100);
		else if (rando == 2)
			myExploringTarget = here.add(-100,100);
		else
			myExploringTarget = here.add(100,100);
		this.stratName = "ExploreStrat";
	}
	
	public boolean tryTurn() throws GameActionException
	{
		// get a random waypoint and move towards it
		if (myExploringTarget == null || here.distanceSquaredTo(myExploringTarget) < 9 || !MapInfo.isOnMap(myExploringTarget))
		{
			myExploringTarget = MapInfo.getExplorationWaypoint();
		}
		
		DirectionSet goodDirs = Micro.getSafeMoveDirs();
		goodDirs = goodDirs.and(Micro.getTurretSafeDirs());
		
		if (Micro.getRoundsUntilDanger() < 5)
		{
			if (!Action.tryRetreatTowards(Message.recentEnemySignal, goodDirs))
				Action.tryRetreatOrShootIfStuck();
		}
//		else
//		{
//			if (!Nav.tryGoTo(myExploringTarget, goodDirs))
//				myExploringTarget = MapInfo.getExplorationWaypoint(); // so you don't get stuck
//			//Action.tryGoToWithoutBeingShot(myExploringTarget, goodDirs);
//		}

		Action.tryGoToWithoutBeingShot(myExploringTarget, goodDirs);
		Debug.setStringAK("My Strategy: " + this.stratName + ", Exploring to " + myExploringTarget);		
        // always send out info about sighted targets
		Sighting.doSendSightingMessage();
		
		// and use spare bytecodes to look for stuff
		MapInfo.analyzeSurroundings();
		// and send the updates AK don't do this automtically - herding one's can't do it
		MapInfo.doScoutSendUpdates();
		
		return true;
	}
	
	
	static final int SCOUT = RobotType.SCOUT.ordinal();
	static final int ARCHON = RobotType.ARCHON.ordinal();
	public static boolean shouldExplore(RobotInfo[] allies, RobotInfo[] zombies) throws GameActionException
	{

		int lowestScoutID = rc.getID();
		RobotInfo[] nearby = Micro.getNearbyAllies();
		int[] nearbyUnits = new int[RobotType.values().length];
		for (RobotInfo ri : nearby)
		{
			nearbyUnits[ri.type.ordinal()]++;
			if (ri.type == RobotType.SCOUT && ri.ID < lowestScoutID)
				lowestScoutID = ri.ID;
		}
		
		// explore if there are too many scouts nearby and an Archon is nearby
		if (nearbyUnits[ARCHON] > 0 && nearbyUnits[SCOUT] > 1 && rc.getID() == lowestScoutID) return true;
		
		// explore if there are no zombies and I'm near an Archon
		// probably I'm herding and the zombies got killed
		if (nearbyUnits[ARCHON] == 0 && zombies.length == 0) return true;	
		
		return false;
	}
}
