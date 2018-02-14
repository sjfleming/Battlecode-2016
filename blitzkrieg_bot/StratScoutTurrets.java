package blitzkrieg_bot;

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
		return Micro.getEnemyUnits().Turrets > 0 
				&& Micro.getFriendlyUnits().Scouts < 2
				&& Micro.getRoundsUntilDanger() > 5;
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
		
		RobotInfo[] closeFriends = rc.senseNearbyRobots(24, ourTeam);
		int nscouts = 0;
		MapLocation closestScout = null;
		
		for (RobotInfo ri : closeFriends)
		{
			if (ri.type != RobotType.SCOUT)
				continue;
			
			nscouts++;
			if (closestScout == null || here.distanceSquaredTo(ri.location) < here.distanceSquaredTo(closestScout))
				closestScout = ri.location;
		}
		
		if (nscouts < 3)
			lastNotCrowdedRound = rc.getRoundNum();
		
		// get back to exploring if there are too many scouts around
		if (roundsSince(lastNotCrowdedRound) > 100)
			return false;
		
		// otherwise go towards nearest turret
		Direction dir = here.directionTo(loc);
		DirectionSet dirs = Micro.getBestSafeDirs();
		
		// fall back to parent strategy if we're boned
		if (Micro.getRoundsUntilDanger() < 2)
			return false;
		
		MapLocation locleft = here.add(dir.rotateLeft().rotateLeft());
		MapLocation locright = here.add(dir.rotateRight().rotateRight());
		
		if (closestScout != null)
		{
			if (locleft.distanceSquaredTo(closestScout) < locright.distanceSquaredTo(closestScout))
				isClockwise = false;
			else
				isClockwise = true;
		}

		for (int i=0; i<4; i++)
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
