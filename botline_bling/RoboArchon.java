package botline_bling;

import battlecode.common.*;

public class RoboArchon extends RobotPlayer
{	
	static MapLocation nextTurretLoc = null;
	static int robotSchedule[] = {10, 10, 10, 6}; // 10-turret 6 - scout
	static int scheduleCounter = rand.nextInt(100);
	static int waitingToBuild = 0;
	static int maxWaitBuildTime = 10;
	static int maxWaitForRally = 600;
	static boolean arrivedAtRally = false;
	static int roundLastAttacked = 0;
	static MapLocation locLastAttacked = null;
	static double myHealth = rc.getType().maxHealth;

	public static void init() throws GameActionException
	{
		myHealth = rc.getHealth();
	}
	
	public static void turn() throws GameActionException
	{
		if (rc.getRoundNum() == RoboScout.SIGNAL_ROUND)
			Message.calcRallyLocation();

		if(rc.getRoundNum()== 1)
		{
			if (tryBuildEven(RobotType.SCOUT)) 
			{
				Message.sendBuiltMessage();
			}
		}
		
		if (!Micro.tryAvoidBeingShot())
		{
			if (amIBeingAttacked())
			{
				Micro.updateAllies();
				// find closest turtle location, and move away
				NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnits();
				if (rc.isCoreReady())
					Nav.goTo(Micro.getUnitCOM(Micro.nearbyAllies), safety);
			}
			else
			{
				if (!arrivedAtRally) 
				{
					arrivedAtRally = tryMoveNearRally();
				}
				else
				{
					nextTurretLoc = MapUtil.findClosestTurtle();
					
					// if it's too close to where i was last shot, change the nextTurretLoc
					if (nextTurretLoc != null && locLastAttacked != null  && nextTurretLoc.distanceSquaredTo(locLastAttacked)<2 )
						nextTurretLoc = here.add(here.directionTo(locLastAttacked).opposite());
					
					if	(!tryBuildUnits(nextTurretLoc))
					{
						Debug.setStringAK("trying to move to turret dest" + nextTurretLoc);
						tryMoveNearTurretDest(nextTurretLoc);
					}
				}
			}
		}
		
		//repair anyone nearby
		tryRepair();

	}	

	public static boolean tryRepair() throws GameActionException
	{
		RobotInfo[] nearbyFriends = rc.senseNearbyRobots(rc.getType().attackRadiusSquared,ourTeam);
		RobotInfo minBot = null;

		for (RobotInfo ri : nearbyFriends)
		{
			if (ri.type == RobotType.ARCHON)
				continue;

			if (minBot == null || (ri.maxHealth - ri.health) > (minBot.maxHealth - minBot.health) || minBot.zombieInfectedTurns < ri.zombieInfectedTurns)
				minBot = ri;
		}

		if (minBot != null && minBot.health < minBot.maxHealth-1)
		{
			rc.repair(minBot.location);
			return true;
		}
		return false;
	}

	public static boolean tryBuildEven(RobotType robotToBuild) throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		if (!rc.hasBuildRequirements(robotToBuild))
			return false;


		Direction dirToBuild = Direction.values()[rand.nextInt(8)];

		// make sure we are building on even square (see function name)
		if (MapUtil.isLocOdd(here.add(dirToBuild)))
			dirToBuild = dirToBuild.rotateRight();

		// rotate right two at a time
		for(int i=0; i<4; i++)
		{
			if (rc.canBuild(dirToBuild, robotToBuild))
			{
				rc.build(dirToBuild, robotToBuild);
				return true;
			}

			dirToBuild = dirToBuild.rotateRight().rotateRight();
		} 

		// failed to find any build locations
		return false;
	}

	public static boolean tryBuildOdd(RobotType robotToBuild) throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		if (!rc.hasBuildRequirements(robotToBuild))
			return false;


		Direction dirToBuild = Direction.values()[rand.nextInt(8)];

		// make sure we are building on odd square (see function name)
		if (!MapUtil.isLocOdd(here.add(dirToBuild)))
			dirToBuild = dirToBuild.rotateRight();

		// rotate right two at a time
		for(int i=0; i<4; i++)
		{
			if (rc.canBuild(dirToBuild, robotToBuild))
			{
				rc.build(dirToBuild, robotToBuild);
				return true;
			}

			dirToBuild = dirToBuild.rotateRight().rotateRight();
		} 

		// failed to find any build locations
		return false;
	}
	
	public static boolean tryMoveNearRally() throws GameActionException
	{
		if(Message.rallyLocation == null)
		{
			// have to wait until rally loc arrives. is this robust??
		}
		else if(here.distanceSquaredTo(Message.rallyLocation) > 4)
		{
			NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnits();
			if (rc.isCoreReady())
			{ 
				Nav.goTo(Message.rallyLocation, safety);
			}
		}
		else
		{
			return true;
		}
		return false;		
	}

	public static boolean tryBuildUnits(MapLocation nextTurretLoc) throws GameActionException
	{
		// so other archons might get a chance to build.
		if (rand.nextBoolean()) return false;
		
		RobotType nextRobot = RobotType.values()[robotSchedule[scheduleCounter%robotSchedule.length]];
		if (nextRobot == RobotType.SCOUT)
		{
			if (tryBuildEven(nextRobot)) 
			{
				scheduleCounter++;
				return true;
			}
		}
		else if (nextRobot == RobotType.TURRET) 
		{
			// Debug.setStringAK("NextTurretDest: " + nextTurretLoc);

			if (tryBuildOdd(nextRobot))
			{
				waitingToBuild = 0;
				scheduleCounter++;
				Message.sendBuiltMessage();
				return true;
			}
		}
		
		return false;
	}

	public static boolean tryMoveNearTurretDest(MapLocation nextTurretLoc) throws GameActionException
	{
		if (nextTurretLoc == null)
			return false;
		
		if (here.isAdjacentTo(nextTurretLoc))
			return false;
		
		if (rc.isCoreReady())
		{
			NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnits();
			Nav.goTo(nextTurretLoc, safety);
			return true;
		}
		else
		{
			return false;
		}
	}
	
	public static boolean amIBeingAttacked() throws GameActionException
	{
		if (rc.getRoundNum()-roundLastAttacked < 100 && roundLastAttacked > 0)
		{
			return true;
		}
		
		if (rc.getHealth() < myHealth)
		{
			myHealth = rc.getHealth();
			locLastAttacked = here;
			roundLastAttacked = rc.getRoundNum();
			return true;
		}
		
		return false;
	}
}
