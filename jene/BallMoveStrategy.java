package jene;

import battlecode.common.*;

import java.util.*;

public class BallMoveStrategy extends RobotPlayer implements Strategy
{	
	private String stratName;
	
	// minimum and maximum radii that we wish to use for the ball
	// set on initialization
	private int minDistSq;
	private int maxDistSq;

	// the target for this ball
	// and its last location and whatnot
	public int 		 	ballTargetID = -1;
	public MapLocation	lastBallLocation = null;
	public int		 	lastBallRound = 0;
	public Direction  	lastBallMoveDir = null;

	public static final int BALL_LOST_TIMEOUT = 20;
	
	public BallMoveStrategy(int targetID, int minDSq, int maxDSq) throws GameActionException
	{
		this.stratName = "BallMoveStrat";	
		minDistSq = minDSq;
		maxDistSq = maxDSq;
		
		if (!rc.canSenseRobot(targetID))
			return;
		
		ballTargetID = targetID;
		lastBallLocation = rc.senseRobot(targetID).location;
		lastBallRound = rc.getRoundNum();
	}
	
	// this is called either by tryUpdateTarget() or by Message
	public void updateBallLocation(MapLocation newLoc)
	{
		lastBallRound = rc.getRoundNum();
		
		if (lastBallLocation.equals(newLoc))
			return;
		
		// otherwise, update the direction
		lastBallMoveDir = lastBallLocation.directionTo(newLoc);
		lastBallLocation = newLoc;
	}
	
	private boolean tryUpdateTarget() throws GameActionException
	{
		// if we're still within sight range, just update the position
		if (rc.canSenseRobot(ballTargetID))
		{
			updateBallLocation(rc.senseRobot(ballTargetID).location);
			return true;
		}
		// if we aren't, check for a timeout before saying "oh god we lost the ball"
		if (rc.getRoundNum() < lastBallRound + BALL_LOST_TIMEOUT)
			return true;
		
		// we lost the ball!
		return false;
	}

	public boolean tryTurn() throws GameActionException
	{
		//Debug.setStringAK("My Strategy: " + this.stratName);
		
		// first check if we can still ball
		if (!tryUpdateTarget())
			return false;
		
		Action.tryAttackSomeone();
		
		// can't do anything, but can stay in state
		if (!rc.isCoreReady())
			return true;
		
		// try to retreat towards the ball or shoot if we're in danger
		if (Micro.getRoundsUntilDanger() < 20)
		{
			if (lastBallLocation != null)
			{
				RobotInfo closestEnemy = Micro.getClosestUnitTo(Micro.getNearbyEnemies(), lastBallLocation);
				if (closestEnemy != null && here.distanceSquaredTo(closestEnemy.location) < lastBallLocation.distanceSquaredTo(closestEnemy.location))
				{
					Action.tryRetreatTowards(lastBallLocation, Micro.getSafeMoveDirs());
					return true;
				}
			}
			else
			{
				Action.tryGoToWithoutBeingShot(here, Micro.getSafeMoveDirs());
				return true;
			}
		}
		
		// if we're far, try moving closer using bugging nav probably
		if (here.distanceSquaredTo(lastBallLocation) > maxDistSq)
		{
			Nav.tryGoTo(lastBallLocation,Micro.getSafeMoveDirs());
			return true;
		}
		if (here.distanceSquaredTo(lastBallLocation) < minDistSq)
		{
			Action.tryAdjacentSafeMoveToward(here.directionTo(lastBallLocation).opposite());
			return true;
		}
		
		// otherwise get a list of all valid directions that keep us in the ball and using those
		DirectionSet ballDirs = Micro.getSafeMoveDirs().clone();
		
		 // avoid crowding other archons RR
        RobotInfo[] nearbyAllies = rc.senseNearbyRobots(9, ourTeam);
        for (Direction d : ballDirs.getDirections())
        {
            for (RobotInfo ri: nearbyAllies)
            {
                if ((ri.type == RobotType.ARCHON) && ri.ID!=ballTargetID && here.add(d).distanceSquaredTo(ri.location) < 2)
                {
                    ballDirs.remove(d);
                    //System.out.println("Avoiding a neighbor arhcon");
                    break;
                }    
            }
        }
		
		for (Direction d : ballDirs.getDirections())
		{
			int dSq = here.add(d).distanceSquaredTo(lastBallLocation);
			if (dSq < minDistSq || dSq > maxDistSq)
				ballDirs.remove(d);
		}
		
		// try mimicking the ball
		if (lastBallMoveDir != null)
		{
			// if it worked, set it to null to indicate we took care of it
			Direction d = ballDirs.getDirectionTowards(lastBallMoveDir);
			if (d != null)
			{
				//first try clear rubble if it's stopping you
				if (Action.tryClearRubble(d)) return true;
				
				Action.tryMove(d);
				lastBallMoveDir = null;
				return true;
			}
		}
		
		MapLocation ml = Micro.getUnitCOM(rc.senseNearbyRobots(24, ourTeam));
		Direction ar = here.directionTo(lastBallLocation).rotateRight().rotateRight();
		Direction al = here.directionTo(lastBallLocation).rotateLeft().rotateLeft();
		Direction moveDir = null;
		if (here.add(al).distanceSquaredTo(ml) > here.add(ar).distanceSquaredTo(ml))
			moveDir = ballDirs.getDirectionTowards(al);
		else
			moveDir = ballDirs.getDirectionTowards(ar);
		
		if (moveDir != null)
			Action.tryMove(moveDir);
		
		// not doing anything else, so look for parts and DIG
		MapLocation closestPart = Rubble.senseClosestPart();
		return Rubble.tryClearRubble(closestPart);
		
	}
}
