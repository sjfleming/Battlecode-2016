package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class StratArchonNormal extends RoboArchon implements Strategy
{
	private Strategy overrideStrategy = null;
	private int numBuilds;

	static int lastBuiltRound = 0;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		MapLocation loc = Waypoint.getClosestFriendlyWaypoint();
		return "Normal Archon " + (loc==null?"":here.distanceSquaredTo(loc));
	}
	
	public StratArchonNormal()
	{
		numBuilds = 0;
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
		
		if (earlyDangerRisk && rc.getRoundNum() < 200)
			Message.trySendSignal(120);
		
		// just took heavy mystery damage?
		if (tookHeavyDamageLastRound && Micro.getRoundsUntilDanger() > 10)
		{
			MapLocation retreatDest = here.add(lastMovedDirection.opposite(),10);
			overrideStrategy = new StratTempRetreat(retreatDest, 30);
			overrideStrategy.tryTurn();
			return true;
		}
		
		// first priority, avoid stuff
		if (Micro.getRoundsUntilDanger() < 3)
		{
			// broadcast a long-range "i need help" signal
			if ((rc.getRoundNum()%5) == 0)
				Message.sendMessageSignal(400,Message.Type.UNDER_ATTACK,0);
			
			// don't over-retreat in the early game
			if (rc.getRoundNum() > 100 || Micro.getNearbyAllies().length > 5)
			{
				MapLocation dest = Waypoint.getBestRetreatLocation();	
				Action.tryGoToSafestOrRetreat(dest);
				return true;
			}
		}
		// if we're relatively safe , only retreat a bit
		if (Micro.getRoundsUntilDanger() < 15 && Micro.getNearbyAllies().length < 5)
		{
			// send a normal, less-serious signal over a smaller range
			Message.trySendSignal(120);

			MapLocation dest = Waypoint.getBestRetreatLocation();	
			Action.tryGoToSafestOrRetreat(dest);
		}
		
		// go to neutral archons
		MapLocation neutralArchonLoc = MapInfo.getClosestNeutralArchon();
//		if (neutralArchonLoc != null)
//		{
//			overrideStrategy = new StratArchonBlitz(neutralArchonLoc);
//		}
		
		if (rc.getRoundNum() < SCOUT_SHADOW_ROUND)
			Message.sendArchonLocation(rc.senseRobot(rc.getID()));

		// next, try doing some building
		if (tryBuild())
			return true;

		// look for local waypoint
		MapLocation dest = neutralArchonLoc;
		if (dest ==  null)
			dest = senseClosestNeutral();
		if (dest == null)
			dest = senseClosestPart();
		// if we've got nothing to grab and we've found ourselves far from friendlies
		MapLocation closestFriendly = Waypoint.getClosestSafeWaypoint();
		if (dest == null && closestFriendly != null && here.distanceSquaredTo(closestFriendly) > 200)
			dest = closestFriendly;
		// if we've still got nowhere to go, check for nearby archon
		if (dest == null)
			dest = MapInfo.getClosestNeutralArchon();
		// otherwise, just chill
		
		// we don't always have to move...
		if (dest != null)
			Nav.tryGoTo(dest, Micro.getBestSafeDirs());
		
		return true;
	}

	private boolean tryBuild() throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		
		RobotType robotToBuild = null;
		Strategy.Type buildStrat = null;
		
		int buildPriority = RobotType.TURRET.partCost;

		// gtfo emergency guard override
		if (roundsSince(lastSafeRound) > 200 && roundsSince(lastBuiltRound) > 40 && rc.getHealth() < 200)
		{
			robotToBuild = RobotType.GUARD;
			buildStrat = Strategy.Type.DEFAULT;
			buildPriority = 0;
		}
		// the initial build order
		else if (numBuilds < buildOrder.length)
		{
			robotToBuild = buildOrder[numBuilds];
			if (robotToBuild == RobotType.SCOUT)
				buildStrat = Strategy.Type.EXPLORE;
			else
				buildStrat = Strategy.Type.MOB_MOVE;
			buildPriority += Math.min(0,50-roundsSince(lastBuiltRound));			
		}
		// need to build a shadow scout, top priority
		else if (roundsSince(RoboArchon.lastAdjacentScoutRound) > 20 && rc.getRoundNum() > SCOUT_SHADOW_ROUND)
		{
			buildPriority += 0;
			robotToBuild = RobotType.SCOUT;
			buildStrat = Strategy.Type.SHADOW_ARCHON;
		}
		else if (rand.nextInt() % 5 == 0 && rc.getRobotCount() > 30)
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
			numBuilds ++;
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
