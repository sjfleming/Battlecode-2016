package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class StratUnitRetreat extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	private MapLocation myDest = null;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Retreating to Archon at " + myDest;
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
		
		// if we're fully healed, let me in, coach!
		if (rc.getHealth()+1 >= rc.getType().maxHealth)
			return false;
		
		// or if we can see an archon and also enemies
		if (Micro.getFriendlyUnits().Archons > 0 && Micro.getRoundsUntilDanger() < 20)
			return false;
		
		myDest = Waypoint.getClosestFriendlyWaypoint();
		if (myDest == null)
			myDest = myBuilderLocation;
		
		Nav.tryGoTo(myDest, Micro.getBestAnyDirs());
		
		// should we shoot here? probably
		Action.tryAttackSomeone();
		
		//Rubble.doClearRubble(Rubble.getRandomAdjacentRubble());
		
		return true;
	}
}
