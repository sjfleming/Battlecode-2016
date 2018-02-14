package another_one_bots_the_dust;

import battlecode.common.*;

import java.util.*;

public class StratDig extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	private MapLocation myDest = null;
	private double myHealth = 0;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Retreating to Archon at " + myDest;
	}
	
	public StratDig(MapLocation target) throws GameActionException
	{
		this.myHealth = rc.getHealth();
		this.myDest = target;
	}
	
	public boolean tryTurn() throws GameActionException
	{
		// if we're getting attacked, stop and run
		if (rc.getHealth() < myHealth)
		{
			overrideStrategy = new StratUnitCombat();
		}
		
		// do we have a strategy that takes precedence over this one?
		if (overrideStrategy != null)
		{
			if (overrideStrategy.tryTurn())
				return true;
			else
				overrideStrategy = null;
		}
		
		// can we join in digging with someone else?
		Direction dir = here.directionTo(myDest);
		double rubAhead = rc.senseRubble(here.add(dir));
		double rubRight = rc.senseRubble(here.add(dir.rotateRight()));
		double rubLeft = rc.senseRubble(here.add(dir.rotateLeft()));
		double rubble = rubAhead;
		if (rubRight < rubAhead && rubRight < rubLeft)
		{
			dir = dir.rotateRight();
			rubble = rubRight;
		}
		if (rubLeft < rubAhead && rubLeft < rubRight)
		{
			dir = dir.rotateLeft();
			rubble = rubLeft;
		}
		
		if (dir == null)
			return false;
		
		if (rubble < 10)
			return false;
		
		Rubble.doClearRubble(dir);
		
		return true;
	}
	
	public static boolean shouldDigThroughSingleBlockage() throws GameActionException
	{
		if (Micro.getNearbyAllies() == null || Micro.getNearbyAllies().length < 2)
			return false;
		
		if (Nav.bugLastMoveDir != null && Nav.myDest != null)
		{
			boolean isBlockedAhead = rc.senseRubble(here.add(Nav.bugLastMoveDir)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH;
			boolean isBlocakgeWide = rc.senseRubble(here.add(Nav.bugLastMoveDir.rotateRight())) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH && rc.senseRubble(here.add(Nav.bugLastMoveDir.rotateLeft())) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH;
			boolean isBlockedTwoAhead = rc.senseRubble(here.add(Nav.bugLastMoveDir).add(Nav.bugLastMoveDir)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH;
			
			if (isBlockedAhead && !isBlockedTwoAhead && isBlocakgeWide)
			{
				return true;
			}
		}
		return false;
	}
}
