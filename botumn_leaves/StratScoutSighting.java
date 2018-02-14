package botumn_leaves;

import battlecode.common.*;
import java.util.*;

public class StratScoutSighting extends RobotPlayer implements Strategy
{	
	private Strategy overrideStrategy = null;
	private int shadowTargetID = -1;

	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Shadowing unit " + shadowTargetID;
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
		
		// if we're not actively shadowing, we try to either do this
		// or find a soldier to shadow
		if (rc.getHealth() < 25)
		{
			overrideStrategy = new StratZombieHerding();
			overrideStrategy.tryTurn();
			return true;
		}
		
		for (RobotInfo ri : Micro.getNearbyAllies())
		{
			if (ri.type == RobotType.SOLDIER)
			{
				overrideStrategy = new StratScoutShadow(ri.ID);
				overrideStrategy.tryTurn();
				return true;
			}
		}
				
		// can't do anything, but can stay in state
		if (!rc.isCoreReady())
			return true;
		
		// otherwise, go towards the waypoint
		MapLocation closestDen = MapInfo.getClosestDen();
		if (closestDen != null)
			Action.tryGoToWithoutBeingShot(closestDen, Micro.getSafeMoveDirs());
		else
			Action.tryGoToWithoutBeingShot(rc.getInitialArchonLocations(theirTeam)[0], Micro.getSafeMoveDirs());

		return true;
	}
	

}
