package botumn_leaves;

import battlecode.common.*;


import java.util.*;

public class StratExplore extends RobotPlayer implements Strategy
{	
	private MapLocation myExploringTarget;
	public int lastExploringRound = 0;
	
	public String getName()
	{
		return "Exploring";
	}
	
	public StratExplore() throws GameActionException
	{
		myExploringTarget = MapInfo.getExplorationWaypoint();
		lastExploringRound = rc.getRoundNum();
	}
	
	public boolean tryTurn() throws GameActionException
	{		
		// get a random waypoint and move towards it
		if (myExploringTarget == null || here.distanceSquaredTo(myExploringTarget) < 24 || !MapInfo.isOnMap(myExploringTarget) || roundsSince(lastExploringRound) > 250)
		{
			myExploringTarget = MapInfo.getExplorationWaypoint();
			lastExploringRound = rc.getRoundNum();
		}
		
		DirectionSet goodDirs = Micro.getSafeMoveDirs();
		
		if (Micro.getRoundsUntilDanger() < 10)
		{
			if (!Action.tryRetreatTowards(Message.recentEnemySignal, goodDirs))
				Action.tryRetreatOrShootIfStuck();
		}
		else
		{
			Nav.tryGoTo(myExploringTarget, goodDirs);
		}

		return true;
	}
}
