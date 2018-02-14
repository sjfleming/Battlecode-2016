package another_one_bots_the_dust;

import java.util.ArrayList;

import battlecode.common.*;

public class RoboArchon extends RobotPlayer
{
	public static int lastAdjacentScoutRound = 0;
	public static final int SCOUT_SHADOW_ROUND = 200;
	private static int botsBuiltFromSetBuildOrder = 0;
	private static RobotType[] buildOrder;
	public static Strategy myStrategy;

	public static void init() throws GameActionException
	{
		myStrategy = new StratArchonNormal();
		buildOrder = getSetBuildOrder();
		if (StratArchonBlitz.tryBuild(buildOrder[0]))
			botsBuiltFromSetBuildOrder ++;
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
		}

		// if we haven't finished initial set build order
		if (botsBuiltFromSetBuildOrder < buildOrder.length)
		{
			// build next in build order
			if (StratArchonBlitz.tryBuild(buildOrder[botsBuiltFromSetBuildOrder]))
				botsBuiltFromSetBuildOrder ++;
		}

		// for now, all archons just blitz all the time
		myStrategy.tryTurn();		

		// always do this, no reason not to
		tryRepair();
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
			Message.sendBuiltMessage(Strategy.Type.DEFAULT);
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

	public static RobotType[] getSetBuildOrder()
	{
		// look at our position and decide if we should build a viper, etc.
		MapLocation[] theirArchons = rc.getInitialArchonLocations(theirTeam);
		MapLocation[] ourArchons = rc.getInitialArchonLocations(ourTeam);
		
		int x = 0;
		int y = 0;
		for (MapLocation them : theirArchons)
		{
			x += them.x;
			y += them.y;
		}
		x = x/theirArchons.length;
		y = y/theirArchons.length;
		MapLocation theirCOM = new MapLocation(x,y);
		int myDist = here.distanceSquaredTo(theirCOM);

		int shortestDist = myDist;
		for (MapLocation us : ourArchons)
		{
			if (us.distanceSquaredTo(theirCOM) < shortestDist)
				shortestDist = us.distanceSquaredTo(theirCOM);
		}
		
		// if we are the only archon, and close to enemy
		if (ourArchons.length == 1 && shortestDist < 800)
		{
			buildOrder = new RobotType[5];
			buildOrder[0] = RobotType.SOLDIER;
			buildOrder[1] = RobotType.VIPER;
			buildOrder[2] = RobotType.GUARD;
			buildOrder[3] = RobotType.SOLDIER;
			buildOrder[4] = RobotType.SCOUT;
			return buildOrder;
		}

		// we are not the closest. look at zombie spawns. if too bad, change build order
		Zombie z = new Zombie(200);
		int bigZombies = z.getNumEarlyBigZombies();
		int fastZombies = z.getNumEarlyFastZombies();
		int rangedZombies = z.getNumEarlyRangedZombies();
		int stdZombies = z.getNumEarlyStdZombies();

		System.out.println(bigZombies + " big zombies");
		System.out.println(fastZombies + " fast zombies");
		System.out.println(rangedZombies + " ranged zombies");
		System.out.println(stdZombies + " std zombies");

		if (rangedZombies > 3 || bigZombies > 1)
		{
			buildOrder = new RobotType[4];
			buildOrder[0] = RobotType.SOLDIER;
			buildOrder[1] = RobotType.SOLDIER;
			buildOrder[2] = RobotType.SOLDIER;
			buildOrder[3] = RobotType.SCOUT;
			return buildOrder;
		}

		if (fastZombies > 10)
		{
			buildOrder = new RobotType[5];
			buildOrder[0] = RobotType.GUARD;
			buildOrder[1] = RobotType.GUARD;
			buildOrder[2] = RobotType.GUARD;
			buildOrder[3] = RobotType.SOLDIER;
			buildOrder[4] = RobotType.SCOUT;
			return buildOrder;
		}

		if (bigZombies + fastZombies + rangedZombies + stdZombies > 10)
		{
			buildOrder = new RobotType[5];
			buildOrder[0] = RobotType.SOLDIER;
			buildOrder[1] = RobotType.GUARD;
			buildOrder[2] = RobotType.SOLDIER;
			buildOrder[3] = RobotType.SOLDIER;
			buildOrder[4] = RobotType.SCOUT;
			return buildOrder;
		}
		
		// if we are the closest of multiple archons, rush
		if (myDist == shortestDist)
		{
			buildOrder = new RobotType[5];
			buildOrder[0] = RobotType.VIPER;
			buildOrder[1] = RobotType.SOLDIER;
			buildOrder[2] = RobotType.SOLDIER;
			buildOrder[3] = RobotType.GUARD;
			buildOrder[4] = RobotType.SCOUT;
			return buildOrder;
		}

		// otherwise, the default
		buildOrder = new RobotType[3];
		buildOrder[0] = RobotType.SCOUT;
		buildOrder[1] = RobotType.SOLDIER;
		buildOrder[2] = RobotType.SOLDIER;
		return buildOrder;
	}
}
