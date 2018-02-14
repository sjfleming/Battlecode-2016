package another_one_bots_the_dust;

import battlecode.common.*;
import java.util.*;

public class StratArchonGTFO extends RoboArchon implements Strategy
{
	private Strategy overrideStrategy = null;

	private int lastSafeRound = 0;
	private int lastDangerRound = 0;
	private int lastBuiltRound = 0;
	
	private MapLocation gtfoDest = null;
	private MapLocation randomDest = null;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "GTFO to " + Message.getClosestArchon();
	}
	
	public StratArchonGTFO()
	{
		lastSafeRound = rc.getRoundNum();
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
		
		// are we actually safe? return to prev strat
		if (roundsSince(lastDangerRound) > 30)
			return false;
		
		if (randomDest == null || here.distanceSquaredTo(randomDest) < 35)
			randomDest = MapInfo.getExplorationWaypoint();
		
		MapLocation closestArchon = Message.getClosestArchon();
		
		if (closestArchon == null)
			gtfoDest = randomDest;
		else
			gtfoDest = closestArchon;
		
		// look for neutral robots, will help us retreat
		MapLocation neutDest = senseClosestNeutral();
		if (neutDest != null)
			gtfoDest = neutDest;

		// this is not your normal building, this is our oh-shit building
		if (tryBuild())
			return true;
		
		// are we in danger? then run
		if (Micro.getRoundsUntilDanger() < 10)
		{
			lastDangerRound = rc.getRoundNum();
			
			// periodically broadcast for help
			if ((rc.getRoundNum()%5) == 0)
				Message.sendMessageSignal(400,Message.Type.UNDER_ATTACK,0);
			
			Nav.tryGoTo(gtfoDest, Micro.getBestAnyDirs());
		}
		else if (Micro.getRoundsUntilDanger() > 20)
		{
			// update that we were safe
			lastSafeRound = rc.getRoundNum();
		}
				
		return true;
	}

	private boolean tryBuild() throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		
		if (rc.getHealth() > 200 || roundsSince(lastSafeRound)<200 || roundsSince(lastBuiltRound) < 40)
			return false;
		
		RobotType robotToBuild = null;
		Strategy.Type buildStrat = null;
		
		robotToBuild = RobotType.GUARD;
		buildStrat = Strategy.Type.DEFAULT;

		if (rc.getTeamParts() < robotToBuild.partCost)
			return false;

		if (!rc.hasBuildRequirements(robotToBuild))
			return false;

		Direction buildDir = Micro.getCanBuildDirectionSet(robotToBuild).getRandomValid();
		if (buildDir != null)
		{
			overrideStrategy = new StratBuilding(robotToBuild, buildDir, buildStrat);
			lastBuiltRound = rc.getRoundNum();
			return true;
		}
		
		return false;
	}
	
	public static MapLocation senseClosestPart() throws GameActionException
	{
		MapLocation[] parts = rc.sensePartLocations(rc.getType().sensorRadiusSquared);
		
		if (parts == null || parts.length == 0)
			return null;
		
		MapLocation closestPartLoc = null;
		for (MapLocation loc : parts)
		{
			// don't try to go to a part we can't get
			if (rc.senseRobotAtLocation(loc) == null && rc.senseRubble(loc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH && 
					(closestPartLoc == null || here.distanceSquaredTo(loc) < here.distanceSquaredTo(closestPartLoc)))
				closestPartLoc = loc;
		}
		
		return closestPartLoc;
	}
	
	public static MapLocation senseClosestNeutral() throws GameActionException
	{
		RobotInfo[] neutrals = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared,Team.NEUTRAL);
		
		if (neutrals == null || neutrals.length == 0)
			return null;
		
		MapLocation closestNeutralLoc = null;
		for (RobotInfo ri : neutrals)
		{
			if (ri.type == RobotType.ARCHON)
				return ri.location;
			if (closestNeutralLoc == null || here.distanceSquaredTo(ri.location) < here.distanceSquaredTo(closestNeutralLoc))
				closestNeutralLoc = ri.location;
		}
		
		return closestNeutralLoc; // will return an archon location even if it's not really the closest
	}
}
