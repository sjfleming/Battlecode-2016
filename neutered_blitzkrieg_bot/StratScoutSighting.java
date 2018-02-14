package neutered_blitzkrieg_bot;

import battlecode.common.*;
import java.util.*;

public class StratScoutSighting extends RobotPlayer implements Strategy
{	
	private Strategy overrideStrategy = null;

	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Looking for sighting unit";
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
		
		if (rc.getHealth() < 15)
		{
			overrideStrategy = new StratScoutExplore();
			overrideStrategy.tryTurn();
			return true;
		}
		
		// if we're not actively shadowing, we try to either do this
		// or find a soldier to shadow
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
		MapLocation dest = MapInfo.getClosestDen();
		if (dest == null)
			dest = Waypoint.getBestEnemyLocation();
		
		if (dest != null)
			Action.tryGoToSafestOrRetreat(dest);
		else
			return false;

		return true;
	}
	

}
