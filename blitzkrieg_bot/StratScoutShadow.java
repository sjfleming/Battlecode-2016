package blitzkrieg_bot;

import battlecode.common.*;
import java.util.*;

public class StratScoutShadow extends RobotPlayer implements Strategy
{	
	private Strategy overrideStrategy = null;

	// the target for this scout and its last location and direction
	public int 		 	shadowTargetID = -1;
	public MapLocation	lastShadowLocation = null;
	public int		 	lastShadowRound = -10000;
	public Direction  	lastShadowMoveDir = null;

	public static final int SHADOW_LOST_TIMEOUT = 40;
	
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Shadowing unit " + shadowTargetID;
	}
	
	public StratScoutShadow(int targetID) throws GameActionException
	{
		if (!rc.canSenseRobot(targetID))
			return;
		
		shadowTargetID = targetID;
		lastShadowLocation = rc.senseRobot(targetID).location;
		lastShadowRound = rc.getRoundNum();
	}
	
	// this is called either by tryUpdateTarget() or by Message
	public void updateShadowLocation(MapLocation newLoc)
	{
		lastShadowRound = rc.getRoundNum();
		
		if (lastShadowLocation.equals(newLoc))
			return;
		
		// otherwise, update the direction
		lastShadowMoveDir = lastShadowLocation.directionTo(newLoc);
		lastShadowLocation = newLoc;
	}
	
	private boolean trySendArchonLocation(RobotInfo ri) throws GameActionException
	{
		if (ri.type != RobotType.ARCHON || ri.team != ourTeam)
			return false;
		if (ri.coreDelay < 6)
			return false;
		
		Message.sendArchonLocation(ri);
		return true;
	}
	
	private boolean tryUpdateTarget() throws GameActionException
	{
		// if we're still within sight range, just update the position
		if (rc.canSenseRobot(shadowTargetID))
		{
			RobotInfo ri = rc.senseRobot(shadowTargetID);
			updateShadowLocation(ri.location);
			trySendArchonLocation(ri);
			return true;
		}
		// if we aren't, check for a timeout before saying "oh god we lost the ball"
		if (rc.getRoundNum() < lastShadowRound + SHADOW_LOST_TIMEOUT)
			return true;
		
		// we lost the ball!
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
		
		// if we're getting beat up, let's try to draw the zombies away
		if (rc.getHealth() < 15)
		{
			overrideStrategy = new StratScoutExplore();
			overrideStrategy.tryTurn();
			return true;
		}
				
		// first check if we can still ball
		if (!tryUpdateTarget())
			return false;
		
		// can't do anything, but can stay in state
		if (!rc.isCoreReady())
			return true;
		
		// if we're close to the target, we shuffle around randomly
		if (here.distanceSquaredTo(lastShadowLocation) <= 2 && !Micro.isInDanger())
		{
			Action.tryAdjacentSafeMoveToward(lastShadowLocation);
			return true;
		}
		
		// otherwise, go towards the target
		Action.tryGoToSafestOrRetreat(lastShadowLocation);

		return true;
	}
	

}
