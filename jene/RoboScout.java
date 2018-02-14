package jene;

import battlecode.common.*;

import java.util.*;

public class RoboScout extends RobotPlayer
{
	public static Strategy myStrategy;
	
	public static void init() throws GameActionException
	{
		myStrategy = new ExploreStrat();
		// start off balling around closest archon
//		if (myBuilderLocation != null)
//			myStrategy = new ExploreStrat();
//		else
//			myStrategy = new FreeUnitStrategy();
//		 
//		RobotInfo[] allies = rc.senseNearbyRobots(RobotType.SCOUT.sensorRadiusSquared, ourTeam);
//		RobotInfo archon = null;
//		if (allies != null && allies.length > 0)
//		{
//			for (RobotInfo ri : allies)
//			{
//				if (ri.type == RobotType.ARCHON)
//				{
//					archon = ri;
//					continue;
//				}
//			}
//			if (archon != null)
//				myStrategy = new BlitzTeamStrat(RobotType.SCOUT, archon);
//		}
	}
	

	
	public static void turn() throws GameActionException
	{
		RobotInfo[] zombies = Micro.getNearbyZombies();
		RobotInfo[] allies = Micro.getNearbyAllies();
		RobotInfo[] enemies = Micro.getNearbyEnemies();
		boolean sendUpdates = true;
		
		// Default is Ball (see init)
		
		// zombie herd if necessary
		if (ZombieHerdingStrat.shouldHerd(allies, zombies))
		{
			myStrategy = new ZombieHerdingStrat();
			ZombieHerdingStrat.rushingStartLoc = here;
			
			Direction rushDir = here.directionTo(Micro.getClosestUnitTo(zombies, here).location);			
			sendUpdates = false;
		}
		
		// explore if you are free to do so
//		if (ExploreStrat.shouldExplore(allies, zombies))
//		{
//			myStrategy = new ExploreStrat();
//		}

		// Free unit if everything else breaks
//		if (!myStrategy.tryTurn())
//			myStrategy = new FreeUnitStrategy();
		myStrategy.tryTurn();
		
		Sighting.doSendSightingMessage();
		MapInfo.analyzeSurroundings();

		if (sendUpdates) // zombie herding zombies don't have time to send updates
		{
			MapInfo.doScoutSendUpdates();
		}
	}
}
