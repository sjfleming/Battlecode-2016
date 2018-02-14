package botumn_leaves;

import battlecode.common.*;
import java.util.*;

public class StratFreeUnit extends RobotPlayer implements Strategy
{	
	private MapLocation myTarget = null;
	
	public String getName()
	{
		return "Free unit";
	}
	
	public StratFreeUnit() throws GameActionException
	{
		myTarget = MapInfo.getExplorationWaypoint();
	}
	
	public boolean tryTurn() throws GameActionException
	{
		// can't do anything, but can stay in state
		if (!rc.isCoreReady())
			return true;
		
		// if we're close to the waypoint, pick another one
		if (here.distanceSquaredTo(myTarget) < 35)
			myTarget = MapInfo.getExplorationWaypoint();
		
		// try to retreat towards the ball or shoot if we're in danger
		if (Micro.getRoundsUntilDanger() < 10 && Action.tryRetreatTowards(myTarget, Micro.getSafeMoveDirs()))
			return true;
		
		if (Action.tryAttackSomeone())
			return true;

		Nav.tryGoTo(myTarget,Micro.getSafeMoveDirs());
			return true;

		
	}
}
