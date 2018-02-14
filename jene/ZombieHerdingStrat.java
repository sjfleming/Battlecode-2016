package jene;

import battlecode.common.*;

import java.util.*;

public class ZombieHerdingStrat extends RobotPlayer implements Strategy
{	
	private MapLocation myTarget = null;
	private static String stratName;
	private enum ScoutState
	{
		RUSHING,
		HERDING,
		KAMIKAZE;
	}
	public static final int SIGNAL_ROUND = 30; 	

	//start by rushing
	public static ScoutState myState = ScoutState.RUSHING;
	public static MapLocation myExploringTarget;
	public static MapLocation rushingStartLoc = null;
	public static MapLocation herdingDestLoc = null;	
	public static int herdDist = 100;

	public ZombieHerdingStrat() throws GameActionException
	{
		this.stratName = "ZombieHerdingStrat";
	}

	public boolean tryTurn() throws GameActionException
	{
		// state machine update
		updateState();
		Debug.setStringAK(myState.toString());

		// do turn according to state
		switch (myState)
		{
		case RUSHING:
			doScoutRushing();
			break;
		case HERDING:
			doScoutHerding();
			break;
		case KAMIKAZE:
			doScoutKamikaze();
			break;
		}

		// always send out info about sighted targets

		//Sighting.doSendSightingMessage();

		// and use spare bytecodes to look for stuff
		MapInfo.analyzeSurroundings();
		// and send the updates
		MapInfo.doScoutSendUpdates();

		Debug.setStringTS("Scout: " + myState);
		return true;
	}

	private static void updateState() throws GameActionException
	{
		RobotInfo[] zombies = Micro.getNearbyZombies();
		RobotInfo[] allies = Micro.getNearbyAllies();
		RobotInfo[] enemies = Micro.getNearbyEnemies();

		switch (myState)
		{
		case RUSHING:
			// If you are closer to the closest Zombie than any ally, switch to Herding state
			RobotInfo closestAlly = Micro.getClosestUnitTo(allies, here);
			RobotInfo closestZombie = Micro.getClosestUnitTo(zombies, here);

			if (closestZombie !=null)
			{
				if (closestAlly == null)
				{
					myState = ScoutState.HERDING;
					herdingDestLoc = Micro.getUnitCOM(rc.getInitialArchonLocations(theirTeam));
				}
				else if (here.distanceSquaredTo(closestZombie.location) <= here.distanceSquaredTo(closestAlly.location))
				{
					myState = ScoutState.HERDING;
					herdingDestLoc = Micro.getUnitCOM(rc.getInitialArchonLocations(theirTeam));
				}
			}
			break;
		case HERDING:
			// for now, just keep on herding until you are overrun or achieve ultimate glory

			if (shouldKamikaze(enemies, zombies))
			{
				myState = ScoutState.KAMIKAZE;
			}
			break;
		case KAMIKAZE:
			break;
		}		
	}
	public static boolean shouldHerd(RobotInfo[] allies, RobotInfo[] zombies) throws GameActionException
	{
		boolean canSeeArchon = false;

		// don't rush if no zombies
		if (zombies.length == 0) return false;
		
		// don't rush if the only zombie is a den
		if (zombies.length == 1 && zombies[0].type == RobotType.ZOMBIEDEN) return false;

		// don't rush if you see fast zombies
		for (RobotInfo ri : zombies)
		{
			if (ri.type == RobotType.FASTZOMBIE) return false;
		}

		RobotInfo closestZombie = Micro.getClosestUnitTo(zombies, rc.getLocation());
		int dist2ToZombie = rc.getLocation().distanceSquaredTo(closestZombie.location);
		for (RobotInfo ri : allies)
		{
			if (ri.type == RobotType.ARCHON) canSeeArchon = true;
			// don't rush if another scout is closer to zombie
			else if (ri.type == RobotType.SCOUT)
			{
				int otherDist2ToZombie = ri.location.distanceSquaredTo(closestZombie.location);
				if  (otherDist2ToZombie < dist2ToZombie ) return false;
			}
		}	
		// don't rush if you can't see your archon
		if (!canSeeArchon) return false;
		
		return true;

	}

	private static boolean shouldKamikaze(RobotInfo[] enemies, RobotInfo[] zombies) throws GameActionException
	{
		// don't rush if no enemies
		if (enemies.length == 0) return false;
		// don't rush if no zombies
		if (zombies.length == 0) return false;
		else return true;
	}

	private static void doScoutRushing() throws GameActionException
	{
		// go towards closest Zombie
		RobotInfo[] zombies = Micro.getNearbyZombies();

		// got towards zombie center of mass
		//MapLocation zCOM = Micro.getZombieCOM();
		RobotInfo closestZombie = Micro.getClosestUnitTo(zombies, here);
		if (closestZombie !=null)
		{
			if (!Action.tryRetreatTowards(closestZombie.location, Micro.getSafeMoveDirs()))
			{
				Action.tryGoToWithoutBeingShot(closestZombie.location,Micro.getCanMoveDirs());
			}
		}
	}

	private boolean doScoutHerding() throws GameActionException
	{
		int rud = Micro.getRoundsUntilDanger();		
		Direction herdingDir = here.directionTo(herdingDestLoc);
		//Direction perpDir = herdingDir.rotateRight().rotateRight();

		if (herdingDir == null) return false;

		// if there's a unit in your way, bug! It might be a neutral or zombie den
		if (rc.isLocationOccupied(here.add(herdingDir)))
		{
			Nav.tryGoTo(herdingDestLoc, Micro.getCanMoveDirs());
		}

		// if you are going off the edge of the map, rotate right and keep going!
		// check 3 squares ahead to avoid corners
		MapLocation lookAhead = here.add(herdingDir);
		if (!rc.onTheMap(lookAhead.add(herdingDir)))
		{
			Direction newDir = getMapEdgeDir(lookAhead,herdingDir);
			Debug.setStringAK("going off the map need to rotate!");
			if (newDir !=null)
			{
				herdingDestLoc = here.add(newDir.dx*herdDist, newDir.dy*herdDist);	
			}
		}

		if (rud > 5)
		{
			Clock.yield();// wait if not in immediate danger;
		}
		else
		{
			DirectionSet safeDirs = Micro.getSafeMoveDirs().and(Micro.getTurretSafeDirs());
			if (Nav.tryGoTo(herdingDestLoc, safeDirs)) return true;
			if (Nav.tryGoTo(herdingDestLoc,Micro.getCanMoveDirs())); return true;
		}
		
		// if we get there and there's nobody
		if (here.distanceSquaredTo(herdingDestLoc) < RobotType.SCOUT.sensorRadiusSquared && (Micro.getNearbyEnemies() == null || Micro.getNearbyEnemies().length == 0))
			herdingDestLoc = MapInfo.getExplorationWaypoint(); // get a random waypoint
		
		return false;
	}

	public static Direction getMapEdgeDir(MapLocation loc, Direction dir) throws GameActionException
	{
		// Tries the direction given in order of how close to the direction it I want to go.
		// Doesn't move directly back
		if (dir !=null)
		{
			for (int i=0;i<7;i++)
			{
				dir = dir.rotateRight();
				if (rc.onTheMap(loc.add(dir))) return dir;
			}
		}
		return null;
	}

	private static void doScoutKamikaze() throws GameActionException
	{
		RobotInfo[] zombies = Micro.getNearbyZombies();
		RobotInfo[] allies = Micro.getNearbyAllies();
		RobotInfo[] enemies = Micro.getNearbyEnemies();

		if (enemies.length != 0)
		{
			RobotInfo closestEnemy = Micro.getClosestUnitTo(enemies, here);
			if (!Action.tryRetreatTowards(closestEnemy.location, Micro.getSafeMoveDirs()))
			{
				Action.tryGoToWithoutBeingShot(closestEnemy.location,Micro.getCanMoveDirs());
			}
		}
	}
}

