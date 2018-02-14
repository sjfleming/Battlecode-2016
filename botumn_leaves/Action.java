package botumn_leaves;

import battlecode.common.*;

import java.util.*;

public class Action extends RobotPlayer
{
	public static boolean tryAttackSomeone() throws GameActionException
	{
		// attack someone in range if possible, low health first, prioritizing zombies
		
		if (!rc.isWeaponReady())
			return false;
		
		//RobotInfo zombieTarget = Micro.getLowestHealthInMyRange(Micro.getNearbyZombies());
		//RobotInfo enemyTarget = Micro.getLowestHealthInMyRange(Micro.getNearbyEnemies());
		RobotInfo zombieTarget = Micro.getHighestPriorityTarget(Micro.getNearbyZombies());
		RobotInfo enemyTarget = Micro.getHighestPriorityTarget(Micro.getNearbyEnemies());
		
		if (zombieTarget != null && rc.canAttackLocation(zombieTarget.location))
		{
			rc.attackLocation(zombieTarget.location);
			return true;
		}
		
		if (enemyTarget != null && rc.canAttackLocation(enemyTarget.location))
		{
			rc.attackLocation(enemyTarget.location);
			return true;
		}
		
		// attack a sighted target if possible
		// only relevant if we're a turret
		if (rc.getType() != RobotType.TURRET)
			return false;
		
		MapLocation targetLoc = Sighting.getClosestSightedTarget();
		
		// disabled so that turrets only attack dens within sight range
		//if (targetLoc == null)
		//	targetLoc = MapInfo.getClosestDen();
		
		if (targetLoc != null && rc.canAttackLocation(targetLoc))
		{
			rc.attackLocation(targetLoc);
			return true;
		}
		
		return false;
	}
	
	// this function always runs away, no matta whats
	public static boolean tryRetreatOrShootIfStuck() throws GameActionException
	{
		Direction escapeDir = Micro.getBestEscapeDir();
		// if we can't escape (we're stuck), try to shoot
		if (escapeDir == null)
			return tryAttackSomeone();
		else
			return tryMove(escapeDir);
	}
	
	public static boolean tryGoToWithoutBeingShot(MapLocation target, DirectionSet dirSet) throws GameActionException
	{
		// uses Nav to move safely toward a given MapLocation
		// avoids being shot, but gives the minimum berth to hostiles
		
		// count rounds until danger
		// if < value
		// retreat
		// else
		// bug to dest
		
		int roundsUntilDanger = Micro.getRoundsUntilDanger();
		int dangerThreshold = 2;
		int roundsUntilShootAndMove = Micro.getRoundsUntilShootAndMove();
		
		// if we're in danger, try to retreat before we try to shoot
		if (roundsUntilDanger <= dangerThreshold)
			if (tryRetreatOrShootIfStuck() || tryAttackSomeone())
				return true;
		
		// if we are in a small amount of danger, try to shoot first
		if (roundsUntilDanger >= dangerThreshold + roundsUntilShootAndMove)
			if (tryAttackSomeone())// || tryRetreat())
				return true;
		
		return Nav.tryGoTo(target, dirSet);
	}
	
	public static boolean tryRetreatTowards(MapLocation target, DirectionSet dirSet) throws GameActionException
	{
		int roundsUntilDanger = Micro.getRoundsUntilDanger();
		int dangerThreshold = 2;
		int roundsUntilShootAndMove = Micro.getRoundsUntilShootAndMove();
		
		if (target == null)
			return tryRetreatOrShootIfStuck();
		
		// if we're in danger, try to retreat before we try to shoot
		if (roundsUntilDanger <= dangerThreshold)
			if (tryAdjacentSafeMoveToward(here.directionTo(target)) || tryAttackSomeone())
				return true;
		
		// if we are in a small amount of danger, try to shoot first
		if (roundsUntilDanger >= dangerThreshold + roundsUntilShootAndMove)
			if (tryAttackSomeone())// || tryRetreat())
				return true;
		
		return Nav.tryGoTo(target, dirSet);
	}
	
	public static boolean tryAdjacentSafeMoveToward(MapLocation loc) throws GameActionException
	{
		return tryAdjacentSafeMoveToward(here.directionTo(loc));
	}
	
	public static boolean tryAdjacentSafeMoveToward(Direction dir) throws GameActionException
    {
    	if (!rc.isCoreReady() || dir == null)
    		return false;
    	
    	// check safe moves
    	DirectionSet safeMoveDirs = Micro.getSafeMoveDirs();
    	if(!safeMoveDirs.any())
    		return false;
    	
    	// get safe direction closest to dir
    	Direction bestMoveDir = safeMoveDirs.getDirectionTowards(dir);
    	
    	// move if it's valid
    	if (bestMoveDir != null)
    		return tryMove(bestMoveDir);
    	
        return false;
    }
	
	// tryMove expects to be given a valid direction
	public static boolean tryMove(Direction d) throws GameActionException
	{
		// don't do anything, but don't throw error, this is ok
		if (d == Direction.NONE)
		{
			//System.out.println("Given a NONE!");
			return false;
		}
		
		// double check!
		if (d != null && rc.canMove(d) && rc.isCoreReady())
		{
			rc.move(d);
			here = rc.getLocation();
			lastMovedRound = rc.getRoundNum();
			return true;
		}
//			else
//			{
//				System.out.println("Movement exception: tried to move but couldn't!");
//				if (d == null)
//				{
//					System.out.println("Reason: null direction");
//					return false;
//				}
//				if (!rc.isCoreReady())
//					System.out.println("Reason: core not ready");
//				if (rc.isLocationOccupied(here.add(d)))
//					System.out.println("Reason: location occupied");
//				if (rc.senseRubble(here.add(d)) > GameConstants.RUBBLE_OBSTRUCTION_THRESH)
//					System.out.println("Reason: too much rubble");
//				
//				return false;
//			}
		return false;
	}
	
	public static boolean tryClearRubble(Direction dir) throws GameActionException
	{
		if (!rc.getType().canClearRubble())
			return false;
		
		if (!rc.isCoreReady() || dir.equals(Direction.OMNI)) // can't clear our own square
			return false;
		
		if (rc.senseRubble(here.add(dir)) >= GameConstants.RUBBLE_SLOW_THRESH)
		{
			rc.clearRubble(dir);
		}
		
		return false;
	}
}
