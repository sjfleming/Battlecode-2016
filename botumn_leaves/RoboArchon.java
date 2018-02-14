package botumn_leaves;

import java.util.ArrayList;
import battlecode.common.*;

public class RoboArchon extends RobotPlayer
{
	public static int lastAdjacentScoutRound = 0;
	
	public static void init() throws GameActionException
	{
		/*if (here.equals(MapInfo.farthestArchonLoc) && MapInfo.numInitialArchons > 1)
			myStrategy = new StratTurtleArchon();
		else*/
			myStrategy = new StratArchonNormal();
	}
	
	public static void turn() throws GameActionException
	{
		// do local check
		checkAdjacentScout();
		
		// always try this if we can, before moving
		tryActivateNeutrals();
		
		// for now, all archons just blitz all the time
		myStrategy.tryTurn();		
		
		// always do this, no reason not to
		tryRepair();
	}
	
	public static void checkAdjacentScout() throws GameActionException
	{
		RobotInfo[] nearby = rc.senseNearbyRobots(2, ourTeam);
		
		for (RobotInfo ri : nearby)
		{
			if (ri.type == RobotType.SCOUT)
				lastAdjacentScoutRound = rc.getRoundNum();
		}
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
			return true;
		}
		
		return false;
	}
	
	public static boolean tryRepair() throws GameActionException
	{
		RobotInfo[] nearbyFriends = rc.senseNearbyRobots(rc.getType().attackRadiusSquared, ourTeam);
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
