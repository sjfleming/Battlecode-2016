package botline_bling;

import battlecode.common.*;



public class RoboTurret extends RobotPlayer
{
	static MapLocation unpackDest = null;
	//static final int[] unpackSearchTableX = {-1,1,1,-1,-2,0,2,0,-2,-2,2,2,1,-1,-3,-3,-1,1};
	//static final int[] unpackSearchTableY = {1, 1, -1, -1, 0, 2, 0 ,-2,-2,2,2,-2,-3,-3,-1,1,3,3};
	
	static int lastFiredRound = 0;
	static final int TURRET_PACK_DELAY = 15;

	// AK: unpack search in this pattern
	// [][]18[]18[][]
	// []10[]06[]11[]
	// 16[]01[]02[]19
	// []05[]XX[]07[]
	// 15[]04[]03[]20
	// []09[]08[]12[]
	// [][]14[]13[][]
	
	public static void init() throws GameActionException
	{
	}
	
	public static void turn() throws GameActionException
	{
		if (rc.getType() == RobotType.TURRET)
			turnTurret();
		else
			turnTTM();
	}
	
	public static void turnTurret() throws GameActionException
	{
		// ok let's try to shoot first, ask questions later
		if (tryTurretAttack())
			return;

		// we didn't have anything to shoot at recently, so move around or something
		if (!MapUtil.isLocOdd(rc.getLocation()) && rc.getRoundNum() > lastFiredRound + TURRET_PACK_DELAY)
        	rc.pack();
	}
	
	public static boolean tryTurretAttack() throws GameActionException
	{
		// can we shoot at all?
		if (!rc.isWeaponReady())
			return false;
		
        // prioritize shooting at enemies first
        RobotInfo target = Micro.getClosestTurretTarget(rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, theirTeam), here);
        // if we didn't find any, try closest zombies
        if (target == null)
        	target = Micro.getClosestTurretTarget(rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Team.ZOMBIE), here);
        
        MapLocation targetLocation = null;
        
        // if we still didn't find any, check the sighted table
        if (target == null)
        	targetLocation = Zombie.getSightedTarget();
        else
        	targetLocation = target.location;
        
        if (targetLocation == null)
        	return false;
        
        if (rc.canAttackLocation(targetLocation))
        {
			rc.attackLocation(targetLocation);
			lastFiredRound = rc.getRoundNum();
			return true;
        }
        
        return false;
	}

	
	
	public static void turnTTM() throws GameActionException
	{
		// did we make it to a satisfying spot?
		if (MapUtil.isLocOdd(rc.getLocation()))
		{
			rc.unpack();
			return;
		}
		
		// unpacklocs are always on odd squares
		unpackDest = findNewUnpackDest(unpackDest);
		Debug.setStringTS("Dest: " + unpackDest);

		// and now move towards it
		if (rc.isCoreReady())
		{
			if (here.distanceSquaredTo(unpackDest) <= 2)
			{
				if (rc.canMove(here.directionTo(unpackDest)))
					rc.move(here.directionTo(unpackDest));
				else //something went wrong, find another
					unpackDest = findNewUnpackDest(unpackDest);
				//NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnits();
				//Nav.goTo(unpackDest, safety);
			}
			else
			{
				NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnits();
				Nav.goTo(unpackDest, safety);
			}
		}
	}

	// AK find new unpack location
	public static MapLocation findNewUnpackDest(MapLocation dest) throws GameActionException
	{
		// start in random direction
		Direction d0 = Direction.values()[rand.nextInt(8)];
		if (!MapUtil.isLocOdd(here.add(d0)))
			d0 = d0.rotateRight();
		
		// check four adjacent odd squares
		for (int i=0; i<4; i++)
		{
			MapLocation ml = here.add(d0);
			if (!rc.isLocationOccupied(ml) && rc.senseRubble(ml) < GameConstants.RUBBLE_SLOW_THRESH && rc.onTheMap(ml))
			{
				return ml;
			}
			// rotate right twice to keep parity
			d0 = d0.rotateRight().rotateRight();
		}
		
		if (dest != null)
			return dest;
		
		// all else fails, return a random direction or something
		return here.add(rand.nextInt(200)-100,rand.nextInt(200)-100);
			
		// or check if there are any close by
		//MapLocation newUnpackLoc = here;
		
		
		/*if (!MapUtil.isLocOdd(newUnpackLoc))
		{
			newUnpackLoc = newUnpackLoc.add(Direction.WEST); // now will be odd
		}
		
		for (int i = 0; i < 18; i++)
		{
			// if we found one, go there
			if (!rc.isLocationOccupied(newUnpackLoc) && rc.senseRubble(newUnpackLoc) < GameConstants.RUBBLE_SLOW_THRESH && rc.onTheMap(newUnpackLoc))
			{
				return newUnpackLoc;
			}
			newUnpackLoc = newUnpackLoc.add(unpackSearchTableX[i],unpackSearchTableY[i]);
		}*/
		// otherwise return the old one
	}	

}

