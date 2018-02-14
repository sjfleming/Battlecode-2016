package botumn_leaves;

import battlecode.common.*;

import java.util.*;

// turtle move tries to allow units to move around close to or inside a turtle ball
public class StratTurtleArchon extends RoboArchon implements Strategy
{
	private MapLocation turtleLocation = null;
	private Strategy overrideStrategy = null;
	
	public String getName()
	{
		return "Turtling archon";
	}
	
	public StratTurtleArchon()
	{
		// find a good turtle location
		// for now, pick a random corner of the map
		turtleLocation = new MapLocation(
						rand.nextBoolean() ? MapInfo.mapMin.x : MapInfo.mapMax.x,
						rand.nextBoolean() ? MapInfo.mapMin.y : MapInfo.mapMax.y
					);
	}
	
	public boolean tryTurn() throws GameActionException
	{
		// do we have a strategy that takes precedence over this one?
		// (e.g. combat or building)
		if (overrideStrategy != null)
		{
			if (overrideStrategy.tryTurn())
				return true;
			else
				overrideStrategy = null;
		}
		
		// are we close to a corner? good enough
		if (MapInfo.closestCornerDistanceSq() < 53 || rc.getRoundNum() > 400)
			turtleLocation = here;
		
		// if we're far from the closest corner, let's go there
		if (here.distanceSquaredTo(turtleLocation) > 53)
		{
			// here is where should give an override strategy if shit be going down
			if (Micro.getRoundsUntilDanger() < 10)
			{
				Action.tryRetreatOrShootIfStuck();
			}
			else
			{
				if (new UnitCounts(Micro.getNearbyAllies()).Soldiers < 4)
					tryBuild();
				else
					Nav.tryGoTo(turtleLocation, Micro.getSafeMoveDirs());
			}
			return true;
		}
		
		// are we in any danger?
		if (Micro.getRoundsUntilDanger() < 10)
		{
			if (Micro.getRoundsUntilDanger() > 5)
				Action.tryRetreatTowards(Micro.getAllyCOM(), Micro.getSafeMoveDirs());
			else
				Action.tryRetreatOrShootIfStuck();
			return true;
		}
		
		if (tryBuild())
		{
			return true;
		}
		
			
		return true;
	}
	
	private boolean tryBuild() throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		
		
		// figure out what robot to try and build
		UnitCounts units = new UnitCounts(Micro.getNearbyAllies());
		
		RobotType robotToBuild = null;
		Strategy.Type stratToBuild = null;
		
		/*if (units.Soldiers < 2)
		{
			robotToBuild = RobotType.SOLDIER;
			stratToBuild = Strategy.Type.MOB_MOVE;
		}
		else */
		
		int buildPriority = RobotType.TURRET.partCost;
		
		if (roundsSince(RoboArchon.lastAdjacentScoutRound) > 40)
		{
			buildPriority += 0;
			robotToBuild = RobotType.SCOUT;
			stratToBuild = Strategy.Type.SHADOW_ARCHON;
		}
		else if (units.Turrets < 15 || rc.getTeamParts() > 2*RobotType.TURRET.partCost)
		{
			buildPriority += units.Turrets;
			robotToBuild = RobotType.TURRET;
			stratToBuild = Strategy.Type.TURTLE;
		}
		else
		{
			return false;
		}

		if (rc.getTeamParts() < buildPriority)
			return false;
		
		if (!rc.hasBuildRequirements(robotToBuild))
			return false;

		Direction buildDir = Micro.getCanBuildDirectionSet(robotToBuild).getRandomValid();
		if (buildDir != null)
		{
			overrideStrategy = new StratBuilding(robotToBuild, buildDir, stratToBuild);
			return true;
		}
		
		return false;
	}
}
