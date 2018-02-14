package another_one_bots_the_dust;

import battlecode.common.*;
import java.util.*;

public class StratScoutTurrets extends RobotPlayer implements Strategy
{	
	private Strategy overrideStrategy = null;
	
	private boolean isClockwise = true;
	private int lastNotCrowdedRound = 0;
	private MapLocation lastTurretLocation = null;

	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Turret monitoring";
	}
	
	// we oughta scout turrets if there are turrets to scout
	public static boolean shouldScoutTurrets()
	{
		return Micro.getFriendlyUnits().Turrets > 0 && Micro.getFriendlyUnits().Scouts == 0;
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
		
		// can't do anything, but can stay in state
		if (!rc.isCoreReady())
			return true;
		
		MapLocation loc = null;
		for (RobotInfo ri : Micro.getNearbyEnemies())
		{
			if (ri.type != RobotType.TURRET)
				continue;
			
			if (loc == null || here.distanceSquaredTo(ri.location) < here.distanceSquaredTo(loc))
				loc = ri.location;
		}
		
		// if there's nothing to go to nearby, do something else
		if (loc == null)
		{
			if (lastTurretLocation == null)
				return false;
			else
				loc = lastTurretLocation;
		}
		else
		{
			lastTurretLocation = loc;
		}
		
		if (new UnitCounts(rc.senseNearbyRobots(24, ourTeam)).Scouts < 3)
			lastNotCrowdedRound = rc.getRoundNum();
		
		// get back to exploring if there are too many scouts around
		if (roundsSince(lastNotCrowdedRound) > 100)
			return false;
		
		// otherwise go towards nearest turret
		Direction dir = here.directionTo(loc);
		DirectionSet dirs = Micro.getBufferDirs();

		for (int i=0; i<5; i++)
		{
			if (dirs.isValid(dir))
			{
				Action.tryMove(dir);
				return true;
			}
			dir = isClockwise ? dir.rotateLeft() : dir.rotateRight();
		}
		// didn't find one :(
		// change direction for next time
		isClockwise = !isClockwise;
		//Nav.tryGoTo(loc, Micro.getSafeMoveDirs());
		
		return true;
	}
	

}
