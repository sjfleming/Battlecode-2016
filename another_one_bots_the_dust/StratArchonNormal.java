package another_one_bots_the_dust;

import battlecode.common.*;
import java.util.*;

public class StratArchonNormal extends RoboArchon implements Strategy
{
	private Strategy overrideStrategy = null;

	static int lastBuiltRound = 0;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Normal Archon " + Message.getClosestArchon();
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
		
		// first priority, avoid stuff
		if (Micro.getRoundsUntilDanger() < 5)
		{
			if ((rc.getRoundNum()%5) == 0)
				Message.sendMessageSignal(400,Message.Type.UNDER_ATTACK,0);
			if (rc.getRoundNum() > 100 || (Micro.getNearbyAllies() != null && Micro.getNearbyAllies().length > 5))
			{
				MapLocation dest = Message.getClosestArchon();
				if (dest == null)
					dest = MapInfo.farthestArchonLoc;
	
				Nav.tryGoTo(dest, Micro.getCanMoveDirs());
				return true;
			}
		}
		if (Micro.getRoundsUntilDanger() < 20 && (rc.getRoundNum() > 200 || (Micro.getNearbyAllies() != null && Micro.getNearbyAllies().length > 5) ))
		{
			Message.sendSignal(120);
			MapLocation retreatloc = MapInfo.farthestArchonLoc;
			if (retreatloc == null)
			{
				Action.tryRetreatOrShootIfStuck();
				return true;
			}
			else if (here.distanceSquaredTo(retreatloc) > 9)
			{
				Action.tryGoToWithoutBeingShot(retreatloc, Micro.getSafeMoveDirs());
				return true;
			}
		}
		
		if (rc.getRoundNum() < SCOUT_SHADOW_ROUND)
			Message.sendArchonLocation(rc.senseRobot(rc.getID()));

		// next, try doing some building
		if (tryBuild())
			return true;

		// look for waypoint
		MapLocation dest = senseClosestNeutral();
		if (dest == null)
			dest = senseClosestPart();
		if (dest == null)
			dest = Micro.getAllyCOM();
		if (dest == null)
			dest = MapInfo.farthestArchonLoc;
		
		// look for waypoint
		/*dest =  MapInfo.getClosestNeutralArchon();
		if (dest == null || here.distanceSquaredTo(dest) < 1)
		{
			MapLocation closestPart = senseClosestPart();
			MapLocation closestNeutral = senseClosestNeutral();
			if (closestNeutral != null)
				dest = closestNeutral;
			else
				dest = closestPart;
			if (closestNeutral != null && closestPart != null && here.distanceSquaredTo(closestPart) < here.distanceSquaredTo(closestNeutral))
				dest = closestPart;
		}
		if (dest == null || here.distanceSquaredTo(dest) < 1)
			dest = MapInfo.getClosestPart();
		if (dest == null)
			return true;*/
		
		Nav.tryGoTo(dest, Micro.getSafeMoveDirs());
		
		return true;
	}

	private boolean tryBuild() throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		
		// AK wait a few rounds so we can move
		if ((rc.getRoundNum()+rc.getID())%3 != 0)
			return false;
		
		// figure out what robot to try and build
		//UnitCounts units = new UnitCounts(Micro.getNearbyAllies());
		
		RobotType robotToBuild = null;
		Strategy.Type buildStrat = null;
		
		int buildPriority = RobotType.TURRET.partCost;
		
		// need to build a shadow scout, top priority
		if (roundsSince(RoboArchon.lastAdjacentScoutRound) > 20 && rc.getRoundNum() > SCOUT_SHADOW_ROUND)
		{
			buildPriority += 0;
			robotToBuild = RobotType.SCOUT;
			buildStrat = Strategy.Type.SHADOW_ARCHON;
		}
		else if (rand.nextInt() % 8 == 0 && rc.getRobotCount() > 40)
		{
			// build viper!
			buildPriority += Math.min(0,50-roundsSince(lastBuiltRound));
			//robotToBuild = rand.nextBoolean() ? RobotType.GUARD : RobotType.SOLDIER;
			robotToBuild = RobotType.VIPER;
			buildStrat = Strategy.Type.MOB_MOVE;
		}
		else if (rand.nextInt() % 8 < 6)
		{
			buildPriority += Math.min(0,50-roundsSince(lastBuiltRound));
			//robotToBuild = rand.nextBoolean() ? RobotType.GUARD : RobotType.SOLDIER;
			robotToBuild = RobotType.SOLDIER;
			buildStrat = Strategy.Type.MOB_MOVE;
		}
		else if (rand.nextBoolean())
		{
			buildPriority += Math.min(0,50-roundsSince(lastBuiltRound));
			robotToBuild = RobotType.SCOUT;
			buildStrat = Strategy.Type.SHADOW_SOLDIER;
		}
		else
		{
			Math.min(0,50-roundsSince(lastBuiltRound));
			robotToBuild = RobotType.SCOUT;
			buildStrat = Strategy.Type.EXPLORE;
		}
		
		if (rc.getTeamParts() < buildPriority)
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
