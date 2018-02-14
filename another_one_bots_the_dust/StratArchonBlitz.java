package another_one_bots_the_dust;

import battlecode.common.*;

import java.util.*;

public class StratArchonBlitz extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;

	private RobotInfo myArchon = null;
	private MapLocation dest = null;

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
	
	
	public StratArchonBlitz(RobotInfo myArchon) throws GameActionException
	{
		this.myArchon = myArchon;
		
		tryBuild(RobotType.VIPER);
	}
	
	public StratArchonBlitz(MapLocation dest) throws GameActionException
	{
		this.dest = dest;
	}
	
	public boolean tryTurn() throws GameActionException
	{
		//Debug.setStringAK("My Strategy: " + this.stratName);
		switch (rc.getType())
		{
		case ARCHON:
			dest = MapInfo.getClosestNeutralArchon();
			
			if (dest == null)
			{
				MapLocation partLoc = senseClosestPart();
				MapLocation neutralLoc = senseClosestNeutral();
				RobotInfo[] hostiles = Micro.getNearbyHostiles();
				
				// avoid all hostiles
				if (hostiles != null && hostiles.length > 0)
				{
					if (!Action.tryRetreatTowards(neutralLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs())))
					{
						if (!Action.tryRetreatTowards(partLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs())))
						{
							Action.tryRetreatOrShootIfStuck();
						}
					}
					return false;
				}
				
				// run around and grab stuff as fast as possible
				if (neutralLoc != null)
				{
					if (here.distanceSquaredTo(neutralLoc) < 9 && rc.senseRubble(here.add(here.directionTo(neutralLoc))) > GameConstants.RUBBLE_OBSTRUCTION_THRESH)
						Rubble.doClearRubble(here.directionTo(neutralLoc));
					Action.tryGoToWithoutBeingShot(neutralLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
					Debug.setStringRR("going to neutral at " + neutralLoc.toString());
				}
					
				else if (partLoc != null)
				{
					Action.tryGoToWithoutBeingShot(partLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
					Debug.setStringRR("going to partLoc at " + partLoc.toString());
				}
						
				else if (here.distanceSquaredTo(MapInfo.getSymmetricLocation(here)) > 100)
				{
					MapLocation symLoc = MapInfo.getSymmetricLocation(here);
					Action.tryGoToWithoutBeingShot(symLoc, Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs()));
					Debug.setStringRR("going to mapsymmetry loc at " + MapInfo.getSymmetricLocation(here).toString());
				}
				
				else
					return false;
				
				return true;
			}
			else // we have a priority destination, a neutral archon, go fast
			{
				if (here.distanceSquaredTo(dest) == 1)
				{
					dest = null;
					return false;
				}
				
				Debug.setStringSJF("trying to book it to neutral archon at " + dest.toString());
				
				// if there's rubble in the way, clear it if it's reasonable
				double rub = rc.senseRubble(here.add(here.directionTo(dest)));
				double furtherRub = rc.senseRubble(here.add(here.directionTo(dest)).add(here.directionTo(dest)));
				if (rub > GameConstants.RUBBLE_OBSTRUCTION_THRESH && rub < 3000 && furtherRub < GameConstants.RUBBLE_OBSTRUCTION_THRESH)
				{
					Rubble.tryClearRubble(dest);
				}
				
				if (!Action.tryGoToWithoutBeingShot(dest, Micro.getSafeMoveDirs()))
				{
					Rubble.tryClearRubble(dest);
					System.out.println("I'm clearing rubble to get to the neutral archon");
				}
				
				return true;
			}
			
			
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
