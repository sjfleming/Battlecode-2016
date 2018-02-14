package jene;

import battlecode.common.*;

import java.util.*;

public class ArchonNormalStrat extends RobotPlayer implements Strategy
{
	private String stratName;
	private static int scoutsBuilt = 0;
	private static int soldiersBuilt = 0;
	public static MapLocation dest = null;
	public static MapLocation rallyLoc = null;
	private static boolean reachedRally = false;
	
	static RobotType myNextBuildRobotType = RobotType.SCOUT;
	
	static int lastBuiltRound = 0;
	
	public ArchonNormalStrat()
	{
		this.stratName = "ArchonNormalStrat";
		
		// first rally, farthest from their archons
		MapLocation[] ourArchons = rc.getInitialArchonLocations(ourTeam);
		if (ourArchons.length < 2)
		{
			rallyLoc = here;
			reachedRally = true;
		}
		else // multiple archons
		{
			MapLocation[] theirArchons = rc.getInitialArchonLocations(theirTeam);
			MapLocation theirCOM = Micro.getUnitCOM(theirArchons);
			
			int farthestArchonDist = 0;
			MapLocation farthestArchonLoc = null;
			for (MapLocation archon : ourArchons)
			{
				if (archon.distanceSquaredTo(theirCOM) > farthestArchonDist)
				{
					farthestArchonDist = archon.distanceSquaredTo(theirCOM);
					farthestArchonLoc = archon;
				}
			}
			rallyLoc = farthestArchonLoc;
			if (rallyLoc == null)
				rallyLoc = here;
			if (here.distanceSquaredTo(rallyLoc) < RobotType.ARCHON.sensorRadiusSquared)
			{
				reachedRally = true;
			}
		}
		

		
	}
	
	public boolean tryTurn() throws GameActionException
	{
		//if (dest != null)
			//Debug.setStringAK("My Strategy: " + this.stratName + ", dest = " + dest.toString());
		//else
			//Debug.setStringAK("My Strategy: " + this.stratName);
		
		// first priority, avoid stuff
		if (Micro.isInDanger())
			Action.tryRetreatOrShootIfStuck();
		
		myNextBuildRobotType = getNextBuildRobotType();
		if (canBuildNow() && (scoutsBuilt < 1 || senseClosestPart() == null || rand.nextBoolean())) //AK give them some time to move
			doBuild();
		else
			doWaypoint();
		
		return true;
	}
	
	public static boolean doWaypoint() throws GameActionException
	{
		if (here.distanceSquaredTo(rallyLoc) < RobotType.ARCHON.sensorRadiusSquared)
		{
			reachedRally = true;
		}
		if (!reachedRally)
		{
			dest = rallyLoc;
		}
		else
		{
			// look for waypoint
			if (dest == null || here.distanceSquaredTo(dest) < 1)
				dest =  MapInfo.getClosestNeutralArchon();
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
				return true;
		}
		
		// if we cannot go that way safely, stop trying
//		Direction dir = Micro.getSafeMoveDirs().getDirectionTowards(here, dest);
//		if (dir == null || dir == Direction.NONE)
//		{
//			dest = MapInfo.getSymmetricLocation(dest);
//		}
		
		if (here.distanceSquaredTo(dest) > 10 && Action.tryGoToWithoutBeingShot(dest, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs())))
			return true;
		
		// not doing anything else, so look for parts and DIG
		if (!Rubble.tryClearRubble(dest))
		{
			Action.tryGoToWithoutBeingShot(dest, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
			return true;
		}
		else
		{
			Action.tryGoToWithoutBeingShot(dest, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
			return true;
		}
		
	}
	
	private static void doBuild() throws GameActionException
	{
		RobotType nextRobotType = myNextBuildRobotType;
		
		// return false quickly if we cannot build for an obvious reason
		if (nextRobotType == null)
			return;
		if (!rc.isCoreReady())
			return;
		
		tryBuild(nextRobotType);
		return;
	}
	
	public static boolean tryBuild(RobotType robotToBuild) throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		if (!rc.hasBuildRequirements(robotToBuild))
			return false;
		if (scoutsBuilt >= 1 && soldiersBuilt >= 5 && rc.getTeamParts() < 1.5*RobotType.TURRET.partCost)
			return false;

		Direction buildDir = getCanBuildDirectionSet(robotToBuild).getRandomValid();
		if (buildDir != null)
		{
			rc.build(buildDir, robotToBuild);
			if (robotToBuild == RobotType.SCOUT)
				scoutsBuilt ++;
			else if (robotToBuild == RobotType.SOLDIER)
				soldiersBuilt ++;
			return true;
		}
		
		return false;
	}

	public static DirectionSet getCanBuildDirectionSet(RobotType nextRobotType) throws GameActionException
	{
		// check nearby open squares
		DirectionSet valid = new DirectionSet();
		for (Direction dir : Direction.values()) // check all Directions around
		{
			if (rc.canBuild(dir, nextRobotType))
				valid.add(dir); // add this direction to the DirectionSet of valid build directions
		}
		return valid;
	}
	
	public static DirectionSet getParityAllowedDirectionSet(RobotType nextRobotType) throws GameActionException
	{
		DirectionSet allowedParity = null;
		if (nextRobotType == RobotType.TURRET)
		{
			// build turrets only on odd squares
			allowedParity = DirectionSet.getOddSquares(here);
		}
		else
		{
			// build all else only on even squares
			allowedParity = DirectionSet.getEvenSquares(here);
		}
		return allowedParity;
	}
	
	static final int SOLDIER = RobotType.SOLDIER.ordinal();
	static final int GUARD = RobotType.GUARD.ordinal();
	static final int TURRET = RobotType.TURRET.ordinal();
	static final int SCOUT = RobotType.SCOUT.ordinal();
	
	private static RobotType getNextBuildRobotType() throws GameActionException
	{
		RobotInfo[] nearby = Micro.getNearbyAllies();
		int[] nearbyUnits = new int[RobotType.values().length];
		for (RobotInfo ri : nearby)
			nearbyUnits[ri.type.ordinal()]++;
		
		int combatUnits = nearbyUnits[SOLDIER]
						  + nearbyUnits[GUARD]
						  + nearbyUnits[TURRET];
		
		//if (nearbyUnits[SCOUT] < 1 || nearbyUnits[SCOUT] < combatUnits/8)
		if (scoutsBuilt <= 2 && nearbyUnits[SCOUT] <= combatUnits/8)
			return RobotType.SCOUT;
		
		if (nearbyUnits[SOLDIER] < 10)
			return RobotType.SOLDIER;
		
//		if (nearbyUnits[TURRET] < 5)
//			return RobotType.TURRET;
		
		return rand.nextBoolean() ? RobotType.GUARD : RobotType.VIPER;
	}
	
	private static boolean canBuildNow() throws GameActionException
	{
		RobotType nextRobotType = myNextBuildRobotType;
		// return false quickly if we cannot build for an obvious reason
		if (nextRobotType == null)
			return false;
		if (!rc.isCoreReady())
			return false;
		if (!rc.hasBuildRequirements(nextRobotType))
			return false;
		if (Micro.isInDanger())
			return false;
		
		// find okay direction set
		DirectionSet canBuild = getCanBuildDirectionSet(nextRobotType);
		DirectionSet parity = getParityAllowedDirectionSet(nextRobotType);
		DirectionSet validBuildDirectionSet = canBuild.and(parity);
		
		// if we have a valid direction return true
		return (validBuildDirectionSet.any());
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
			if ( (closestPartLoc == null && rc.senseRobotAtLocation(loc) == null && rc.senseRubble(loc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH) || (closestPartLoc != null && here.distanceSquaredTo(loc) < here.distanceSquaredTo(closestPartLoc) && rc.senseRobotAtLocation(loc) == null && rc.senseRubble(loc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH))
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
			if (closestNeutralLoc == null || ri.type == RobotType.ARCHON || (ri.type != RobotType.ARCHON && here.distanceSquaredTo(ri.location) < here.distanceSquaredTo(closestNeutralLoc)))
				closestNeutralLoc = ri.location;
		}
		
		return closestNeutralLoc; // will return an archon location even if it's not really the closest
	}
}
