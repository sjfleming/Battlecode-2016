package team023;

import java.util.ArrayList;

import battlecode.common.*;

public class RoboArchon extends RobotPlayer
{
	public static int lastAdjacentScoutRound = 0;
	public static final int SCOUT_SHADOW_ROUND = 500;
	public static RobotType[] buildOrder;
	
	public static boolean earlyDangerRisk = false;

	public static void init() throws GameActionException
	{
		myStrategy = new StratArchonNormal();
		setBuildOrder();
	}

	public static void turn() throws GameActionException
	{
		// do local check
		checkAdjacentScout();

		// always try this if we can, before moving
		tryActivateNeutrals();

		if (Debug.DISPLAY_DEBUG)
		{
			for (MapLocation loc : MapInfo.zombieDenLocations.elements())
				rc.setIndicatorLine(here, loc, 255,255,255);
			for (MapLocation loc : MapInfo.neutralArchonLocations.elements())
				rc.setIndicatorLine(here, loc, 255,0,255);
			for (int i=0; i<5; i++)
				if (Waypoint.enemyTargetStore.targets[i] != null
				&& roundsSince(Waypoint.enemyTargetStore.targets[i].round) < 500)
					rc.setIndicatorLine(here,Waypoint.enemyTargetStore.targets[i].location,255,0,0);
			for (int i=0; i<5; i++)
				if (Waypoint.friendlyTargetStore.targets[i] != null
				&& roundsSince(Waypoint.friendlyTargetStore.targets[i].round) < 500)
					rc.setIndicatorLine(here,Waypoint.friendlyTargetStore.targets[i].location,0,255,0);
		}

		// for now, all archons just blitz all the time
		myStrategy.tryTurn();		

		// always do this, no reason not to
		tryRepair();
		
		if (rc.getRoundNum() == StratZDay.ZDAY_ARCHON_ROUND)
			Waypoint.calcBestZDayDest();
		
		// ZDay signal logic check, f the override
		StratZDay.tryArchonSendZDay();
	}

	public static void checkAdjacentScout() throws GameActionException
	{
		RobotInfo[] nearby = rc.senseNearbyRobots(2, ourTeam);

		for (RobotInfo ri : nearby)
		{
			if (ri.type == RobotType.SCOUT)
				lastAdjacentScoutRound = rc.getRoundNum();
		}
	}

	public static boolean tryActivateNeutrals() throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;

		// check adjacent squares for neutrals
		RobotInfo[] adjNeutrals = rc.senseNearbyRobots(2, Team.NEUTRAL);

		if (adjNeutrals.length > 0)
		{
			// activate just the first one
			rc.activate(adjNeutrals[0].location);
			Message.sendBuiltMessage(Strategy.Type.DEFAULT,adjNeutrals[0].ID);
			return true;
		}

		return false;
	}

	public static boolean tryRepair() throws GameActionException
	{
		RobotInfo minBot = null;

		for (RobotInfo ri : Micro.getNearbyAllies())
		{
			if (ri.type == RobotType.ARCHON)
				continue;
			if (here.distanceSquaredTo(ri.location) > rc.getType().attackRadiusSquared)
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
	
	public static void setBuildOrder()
	{
		// look at our position and decide if we should build a viper, etc.
		MapLocation[] ourArchons = rc.getInitialArchonLocations(ourTeam);
		
		int myDist = here.distanceSquaredTo(MapInfo.theirArchonCenter);

		int shortestDist = myDist;
		for (MapLocation us : ourArchons)
		{
			if (us.distanceSquaredTo(MapInfo.theirArchonCenter) < shortestDist)
				shortestDist = us.distanceSquaredTo(MapInfo.theirArchonCenter);
		}
		
		if (ourArchons.length == 1 && shortestDist < 1000)
		{
			buildOrder = new RobotType[]{
							RobotType.SOLDIER,
							RobotType.VIPER,
							RobotType.SCOUT,
							RobotType.SOLDIER
						};
			earlyDangerRisk = true;
			return;
		}

		// we are not the closest. look at zombie spawns. if too bad, change build order
		Zombie z = new Zombie(200);
		int bigZombies = z.getNumEarlyBigZombies();
		int fastZombies = z.getNumEarlyFastZombies();
		int rangedZombies = z.getNumEarlyRangedZombies();
		int stdZombies = z.getNumEarlyStdZombies();

		/*
		System.out.println(bigZombies + " big zombies");
		System.out.println(fastZombies + " fast zombies");
		System.out.println(rangedZombies + " ranged zombies");
		System.out.println(stdZombies + " std zombies");
	 	*/
		
		// if we are the closest of multiple archons, rush
		if (myDist == shortestDist)
		{
			buildOrder = new RobotType[]{
					RobotType.VIPER,
					RobotType.SCOUT,
					RobotType.SOLDIER,
					RobotType.SOLDIER
				};
			earlyDangerRisk = true;
			return;
		}
		
		if (rangedZombies > 3 || bigZombies > 1)
		{
			buildOrder = new RobotType[]{
					RobotType.SCOUT,
					RobotType.SOLDIER,
					RobotType.SOLDIER,
					RobotType.SOLDIER
				};
			return;
		}

		if (fastZombies > 10)
		{
			buildOrder = new RobotType[]{
					RobotType.SCOUT,
					RobotType.GUARD,
					/*RobotType.GUARD,
					RobotType.GUARD,*/
					RobotType.SOLDIER
				};
			earlyDangerRisk = true;
			return;
		}

		if (bigZombies + fastZombies + rangedZombies + stdZombies > 10)
		{
			buildOrder = new RobotType[]{
					RobotType.SCOUT,
					RobotType.SOLDIER,
					//RobotType.GUARD,
					RobotType.SOLDIER,
					RobotType.SOLDIER
				};
			earlyDangerRisk = true;
			return;
		}
		
		// otherwise, the default
		buildOrder = new RobotType[]{
				RobotType.SCOUT,
				RobotType.SOLDIER,
				RobotType.SOLDIER
			};
		return;
	}
}
