package jene;

import battlecode.common.*;

import java.util.*;

public class FreeUnitStrategy extends RobotPlayer implements Strategy
{	
	private MapLocation myTarget = null;
	
	public FreeUnitStrategy() throws GameActionException
	{
		myTarget = MapInfo.getExplorationWaypoint();
	}
	
	public boolean tryTurn() throws GameActionException
	{
		if (Action.tryAttackSomeone())
			return true;
		
		// can't do anything, but can stay in state
		if (!rc.isCoreReady())
			return true;
		
		// try to retreat towards the ball or shoot if we're in danger
		if (Micro.getRoundsUntilDanger() < 4 && Action.tryRetreatTowards(myTarget, Micro.getSafeMoveDirs()))
			return true;
		
		// not doing anything else, so look for parts and DIG
		MapLocation closestPart = Rubble.senseClosestPart();
		Rubble.tryClearRubble(closestPart);
		
		// if we're close to the waypoint, pick another one
		if (here.distanceSquaredTo(myTarget) < 35)
			myTarget = MapInfo.getExplorationWaypoint();

		Nav.tryGoTo(myTarget,Micro.getSafeMoveDirs());
			return true;

		
	}
}
