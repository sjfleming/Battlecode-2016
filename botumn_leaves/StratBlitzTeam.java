package botumn_leaves;

import battlecode.common.*;

import java.util.*;

public class StratBlitzTeam extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;

	private RobotInfo myArchon = null;

	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Blitzing";
	}
	
	public static boolean shouldBlitz()
	{
		MapLocation[] theirArchons = rc.getInitialArchonLocations(theirTeam);
		MapLocation[] ourArchons = rc.getInitialArchonLocations(ourTeam);
		
		if (ourArchons.length < 2)
			return false;
		
		int x = 0;
		int y = 0;
		for (MapLocation them : theirArchons)
		{
			x += them.x;
			y += them.y;
		}
		x = x/theirArchons.length;
		y = y/theirArchons.length;
		MapLocation theirCOM = new MapLocation(x,y);
		int myDist = here.distanceSquaredTo(theirCOM);
		
		int shortestDist = myDist;
		for (MapLocation us : ourArchons)
		{
			if (us.distanceSquaredTo(theirCOM) < shortestDist)
				shortestDist = us.distanceSquaredTo(theirCOM);
		}
		
		MapLocation[] parts = rc.sensePartLocations(RobotType.ARCHON.sensorRadiusSquared);
//		if (parts != null && parts.length > 0)
//			return true;
		if (myDist == shortestDist)
			return true;
		
		return false;
	}
	
	
	public StratBlitzTeam(RobotInfo myArchon) throws GameActionException
	{
		this.myArchon = myArchon;
		
		tryBuild(RobotType.SCOUT);
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
						
		switch (rc.getType())
		{
		case ARCHON:
			MapLocation partLoc = senseClosestPart();
			MapLocation neutralLoc = senseClosestNeutral();
			MapLocation mapPartLoc = MapInfo.getClosestPart();
			RobotInfo[] hostiles = Micro.getNearbyHostiles();
			
			// avoid all hostiles
			if (hostiles != null && hostiles.length > 0)
			{
				if (!Action.tryRetreatTowards(neutralLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs())))
					if (!Action.tryRetreatTowards(partLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs())))
						if (!Action.tryRetreatTowards(mapPartLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs())))
							Action.tryRetreatOrShootIfStuck();
				return true;
			}
			
			// run around and grab stuff as fast as possible
			if (neutralLoc != null)
				Action.tryGoToWithoutBeingShot(neutralLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
				
			else if (partLoc != null)
				Action.tryGoToWithoutBeingShot(partLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
			
			else if (mapPartLoc != null)
				Action.tryGoToWithoutBeingShot(mapPartLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
			
			else
				Action.tryGoToWithoutBeingShot(MapInfo.getExplorationWaypoint(), Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
			
			return true;
			
		case SCOUT:
			// scout for the archon
			if (!rc.canSenseRobot(myArchon.ID))
				return false;
			
			MapLocation archonLoc = rc.senseRobot(myArchon.ID).location; // have to update this
			Debug.setStringSJF("my archon is at: " + archonLoc.toString());
			if (here.distanceSquaredTo(archonLoc) > Math.floor(RobotType.ARCHON.sensorRadiusSquared/2))
			{
				Action.tryGoToWithoutBeingShot(archonLoc, Micro.getSafeMoveDirs());
			}
			
			return true;
		
		default:
			// not sure, probably shoot and run away from Archon
			if (!Action.tryAttackSomeone())
			{
				if (myArchon == null)
					return false;
				
				if ( !Nav.tryGoTo(here.add(here.directionTo(myArchon.location).opposite()), Micro.getSafeMoveDirs()) )
					if ( !Nav.tryGoTo(here.add(here.directionTo(myArchon.location).opposite()), Micro.getCanFastMoveDirs()) )
						if ( !Nav.tryGoTo(here.add(here.directionTo(myArchon.location).opposite()), Micro.getCanMoveDirs()) )
							return false;
			}
			return true;
		}
	}
	
	public MapLocation senseClosestPart() throws GameActionException
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
	
	public MapLocation senseClosestNeutral() throws GameActionException
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
	
	public static boolean tryBuild(RobotType robotToBuild) throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		if (!rc.hasBuildRequirements(robotToBuild))
			return false;

		Direction buildDir = Micro.getCanBuildDirectionSet(robotToBuild).getRandomValid();
		if (buildDir != null)
		{
			rc.build(buildDir, robotToBuild);
			return true;
		}
		
		return false;
	}
}
