package team023;

import battlecode.common.*;

import java.util.*;

public class StratTempRush extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	private int rushID = 0;
	private MapLocation lastRushLoc = null;
	private int lastRushUpdateRound = 0;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Rushing unit " + rushID;
	}
	
	public StratTempRush(int rushTarget)
	{
		rushID = rushTarget;
	}
	
	private boolean updateRushTarget() throws GameActionException
	{
		// if we can see it
		if (rc.canSenseRobot(rushID))
		{
			lastRushLoc = rc.senseRobot(rushID).location;
			lastRushUpdateRound = rc.getRoundNum();
			return true;
		}
		// if we're close and nobody, stop rushing
		if (here.distanceSquaredTo(lastRushLoc) <= 13)
			return false;
		
		if (roundsSince(lastRushUpdateRound) < 10)
			return true;
		
		return false;
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
		
		// no target, abort
		if (!updateRushTarget())
			return false;
		
		// shoot first
		if (Action.tryAttackSomeone())
			return true;

		// go to with reckless abandon
		Nav.tryGoTo(lastRushLoc, Micro.getCanMoveDirs());
		
		return true;
	}
}
