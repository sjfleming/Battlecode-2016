package jene;

import java.util.ArrayList;

import battlecode.common.*;

public class RoboArchon extends RobotPlayer
{
	public static Strategy myStrategy;

	public static void init() throws GameActionException
	{
		if (BlitzTeamStrat.shouldBlitz())
			myStrategy = new BlitzTeamStrat(rc.senseRobot(rc.getID()));
		else
			myStrategy = new ArchonNormalStrat();
		
		MapLocation[] ourArchons = rc.getInitialArchonLocations(ourTeam);
		
		if (ourArchons.length < 2)
			BlitzTeamStrat.tryBuild(RobotType.VIPER);
	}
	
	public static void turn() throws GameActionException
	{
		// always try this if we can, before moving
		if (!tryActivateNeutrals() && rc.senseNearbyRobots(1,Team.NEUTRAL) != null && rc.senseNearbyRobots(1,Team.NEUTRAL).length > 0)
			return; // wait to activate
		
		// always do this, no reason not to
		tryRepair();
		
		// try to find new neutral archon
		MapLocation nuetralArchonLoc = MapInfo.getClosestNeutralArchon();
		if (nuetralArchonLoc != null && Micro.getNearbyAllies().length > 1)
		{
			myStrategy = new BlitzTeamStrat(nuetralArchonLoc);
			//Message.sendMessageSignal(here.distanceSquaredTo(nuetralArchonLoc), MessageType.REMOVE_WAYPOINT, nuetralArchonLoc);
		}
		if (nuetralArchonLoc != null && here.distanceSquaredTo(nuetralArchonLoc) == 1)
		{
			MapInfo.removeWaypoint(nuetralArchonLoc);
		}
		
		// turtle if in good position and no other turtlers
		RobotInfo[] allies = Micro.getNearbyAllies();
		UnitCounts count = new UnitCounts(allies);
		int turretNum = count.Turrets;
		int soldierNum = count.Soldiers;
		if (soldierNum >= 5 && Micro.getNearbyHostiles().length == 0)
			myStrategy = new TurtleArchonStrategy();
		
		// archons who stopped blitzing do normal
		if (!myStrategy.tryTurn())
			myStrategy = new ArchonNormalStrat();
		
		// and this - look for & update nearby cool stuff
		MapInfo.analyzeSurroundings();
		
		Debug.setStringTS("D:" + MapInfo.zombieDenLocations.elements().size()
				+ ",P:" + MapInfo.goodPartsLocations.elements().size());
	}
	
	public static boolean tryActivateNeutrals() throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		
		// check adjacent squares for neutrals
		RobotInfo[] adjNeutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);
		
		if (adjNeutrals.length > 0)
		{
			// activate just the first one
			rc.activate(adjNeutrals[0].location);
			// remove it from parts list
			MapInfo.removeWaypoint(adjNeutrals[0].location);
			return true;
		}
		
		return false;
	}
	
	public static boolean tryRepair() throws GameActionException
	{
		RobotInfo[] nearbyFriends = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,ourTeam);
		RobotInfo minBot = null;

		for (RobotInfo ri : nearbyFriends)
		{
			if (ri.type == RobotType.ARCHON)
				continue;

			if (minBot == null || (ri.maxHealth - ri.health) > (minBot.maxHealth - minBot.health) || minBot.zombieInfectedTurns < ri.zombieInfectedTurns)
				minBot = ri;
		}

		if (minBot != null && minBot.health < minBot.maxHealth-1)
		{
			rc.repair(minBot.location);
			return true;
		}
		return false;
	}
}
