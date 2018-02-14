package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class StratTempRetreat extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	private MapLocation retreatLocation = null;
	private int endRetreatRound = 0;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Temp retreat to " + retreatLocation
				+ " until " + endRetreatRound;
	}
	
	public StratTempRetreat(MapLocation dest, int rounds)
	{
		retreatLocation = dest;
		endRetreatRound = rc.getRoundNum() + rounds;
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
		
		// don't do this strategy when in danger
		if (Micro.getRoundsUntilDanger() < 15)
			return false;
		
		if (rc.getRoundNum() >= endRetreatRound)
			return false;

		// otherwise just try going there
		Nav.tryGoTo(retreatLocation, Micro.getBestAnyDirs());
		
		return true;
	}
}
