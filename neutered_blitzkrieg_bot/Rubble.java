package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class Rubble extends RobotPlayer
{
	public static boolean tryClearRubble(MapLocation target) throws GameActionException
	{
		//System.out.println("tryClearRubble got called for " + target.toString());
		
		if (target == null)
			return false;
		
		if (!rc.isCoreReady())
			return false;
		
		if (rc.canSense(target) && rc.senseRubble(here.add(here.directionTo(target))) < GameConstants.RUBBLE_OBSTRUCTION_THRESH)
			return false;
		
		// DIG to target
		
		// move off target
		if (target.equals(here))
		{
			if (!Action.tryMove(Micro.getCanMoveDirs().getRandomValid()))
			{
				Direction rubbleDir = getRandomAdjacentRubble();
				if (rubbleDir != null)
				{
					doClearRubble(rubbleDir);
					return true;
				}
			}
			else
			{
				return true;
			}
		}
		
		// clear rubble in the way of parts
		if (rc.senseRubble(here.add(here.directionTo(target))) > GameConstants.RUBBLE_OBSTRUCTION_THRESH)
		{
			doClearRubble(here.directionTo(target));
			return true;
		}
		
		return false;
	}
	
	public static boolean tryRandomClearRubbleInPath(MapLocation target) throws GameActionException
	{
		if (target == null)
			return false;
		
		if (!rc.isCoreReady())
			return false;
		
		// don't clear if we took damage recently
		if (roundsSince(lastDamageRound) < 5)
			return false;
		
		Direction towards = here.directionTo(target);
		
		if (Nav.isGoodDigLocation())
			return tryClearLowestAdjacentRubble(towards);
		
		if (rc.canSense(target) && rc.senseRubble(here.add(towards)) < GameConstants.RUBBLE_SLOW_THRESH)
			return false;
		
		// look for a direction where digging is minimal
		
		// straight ahead
		double rubAhead = rc.senseRubble(here.add(towards)) + rc.senseRubble(here.add(towards).add(towards));
		
		// ahead right
		//double rubRight = rc.senseRubble(here.add(here.directionTo(target).rotateRight())) + rc.senseRubble(here.add(here.directionTo(target).rotateRight()).add(here.directionTo(target).rotateRight()));
		
		// ahead left
		//double rubLeft = rc.senseRubble(here.add(here.directionTo(target).rotateLeft())) + rc.senseRubble(here.add(here.directionTo(target).rotateLeft()).add(here.directionTo(target).rotateLeft()));
		
		// get min rubble direction
		double rubble = rubAhead;
		
//		if (rubRight < rubAhead)
//		{
//			rubble = rubRight;
//			towards = here.directionTo(target).rotateRight();
//		}
//		if (rubLeft < rubRight && rubLeft < rubAhead)
//		{
//			rubble = rubLeft;
//			towards = here.directionTo(target).rotateLeft();
//		}
		rubble = rubble - 2 * GameConstants.RUBBLE_OBSTRUCTION_THRESH; // how much just to get through
		
		// fast moves in the right direction?  take them
		DirectionSet fastMovesSet = Micro.getCanFastMoveDirs();
		Direction towardFast = fastMovesSet.getDirectionTowards(towards);
		if (towardFast != null)
			return false;
		
		// probabilities
		double g = rand.nextGaussian();
		if ((rubble - 2*100) > g*1000+2000 || (Micro.getNearbyAllies().length < 3+g*3 && (rubble - 2*100) > GameConstants.RUBBLE_OBSTRUCTION_THRESH*5))
			return false;
		
		// DIG to target
		Debug.setStringSJF("rubble = " + rubble);
		if (rc.senseRubble(here.add(towards)) >= GameConstants.RUBBLE_OBSTRUCTION_THRESH)
		{
			doClearRubble(towards);
			return true;
		}
		if (rc.senseRubble(here.add(towards)) >= GameConstants.RUBBLE_SLOW_THRESH && rand.nextInt(3) > 0)
		{
			doClearRubble(towards);
			return true;
		}
		
		return false;
	}
	
	public static boolean tryClearLowestAdjacentRubble(Direction dir) throws GameActionException
	{
		double r1 = rc.senseRubble(here.add(dir));
		double r2 = rc.senseRubble(here.add(dir.rotateLeft()));
		double r3 = rc.senseRubble(here.add(dir.rotateRight()));
		
		if (!rc.onTheMap(here.add(dir.rotateLeft())))
			r2 = 1e100;
		if (!rc.onTheMap(here.add(dir.rotateRight())))
			r3 = 1e100;
		
		double r = Math.min(Math.min(r1, r2), r3);
		
		if (r == r1)
		{
			doClearRubble(dir);
			return true;
		}
		if (r == r2)
		{
			doClearRubble(dir.rotateLeft());
			return true;
		}
		if (r == r3)
		{
			doClearRubble(dir.rotateRight());
			return true;
		}
		return false;
	}
	
	public static Direction getRandomAdjacentRubble() throws GameActionException
	{
		DirectionSet dirs = DirectionSet.makeAll();
		dirs.remove(Direction.NONE);
		for(Direction dir : Direction.values())
		{
			if (!dirs.isValid(dir))
				continue;
			
			if (rc.senseRubble(here.add(dir)) > GameConstants.RUBBLE_OBSTRUCTION_THRESH && rc.senseRubble(here.add(dir)) < 5000)
			{
				return dir;
			}
		}
		
		return null;
	}
	
	// tries to clear lowest rubble on unoccupied squares
	public static boolean tryClearEscapeRubble() throws GameActionException
	{
		double lowestval = 1e10;
		Direction bestDir = null;
		for (Direction d : Direction.values())
		{
			if (d == Direction.NONE || d == Direction.OMNI)
				continue;
			MapLocation testloc = here.add(d);
			if (rc.onTheMap(testloc) && rc.senseRubble(testloc) < lowestval &&
					!rc.isLocationOccupied(testloc))
			{
				lowestval = rc.senseRubble(testloc);
				bestDir = d;
			}
		}
		
		if (bestDir != null)
		{
			doClearRubble(bestDir);
			return true;
		}
		
		return false;
	}
	
	public static void doClearRubble(Direction dir) throws GameActionException
	{
		if (rc.isCoreReady() && dir != null && dir != Direction.NONE
				&& dir != Direction.OMNI && rc.onTheMap(here.add(dir)))
			rc.clearRubble(dir);
	}
	
	public static MapLocation senseClosestPart() throws GameActionException
	{
		MapLocation[] parts = rc.sensePartLocations(rc.getType().sensorRadiusSquared);
		
		if (parts == null || parts.length == 0)
			return null;
		
		MapLocation closestPartLoc = null;
		for (MapLocation loc : parts)
		{
			// don't try to go to a part we can't get
			if (rc.senseRobotAtLocation(loc) == null)
				if (closestPartLoc == null || here.distanceSquaredTo(loc) < here.distanceSquaredTo(closestPartLoc))
					closestPartLoc = loc;
		}
		
		return closestPartLoc;
	}
}
