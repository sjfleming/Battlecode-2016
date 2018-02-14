package botline_bling;

import battlecode.common.*;

import java.util.*;

public class RoboScout extends RobotPlayer
{
	public static final int SIGNAL_ROUND = 30; 
	public static MapLocation target = null;
	public static boolean isFreeScout = false;
	public static MapLocation rally = null;
	
	public static void init() throws GameActionException
	{
		// first-spawn scout, send the message right away
		// to a distance of 100 units
		if (rc.getRoundNum() < SIGNAL_ROUND)
		{
			if (myArchon != null)
			{
				Message.sendMessageSignal(100, MessageType.SPAWN, myArchon.location);
			}
		}
		target = here.add(rand.nextInt(200)-100,rand.nextInt(200)-100);
		
//		// =========================================================
//		// for now, a hacked way of deciding if you are a free scout
//		Micro.updateAllies();
//		int numOtherScouts = 0;
//		for (RobotInfo bot : Micro.nearbyAllies)
//		{
//			if (bot.type == RobotType.SCOUT)
//				numOtherScouts += 1;
//		}
//		if (numOtherScouts >= 1)
//			isFreeScout = true;
//		// =========================================================
		isFreeScout = false;
		
		rally = here.add(0,1);
			
	}
	
	public static int[] getNumAlliedScoutsAndTurretsInRange() throws GameActionException
	{
		RobotInfo[] allies = rc.senseNearbyRobots(here, RobotType.SCOUT.sensorRadiusSquared, ourTeam);
		int numAlliedScoutsInRange = 0;
		int numAlliedTurretsInRange = 0;
		for (RobotInfo ri : allies)
		{
			if (ri.type == RobotType.SCOUT)
				numAlliedScoutsInRange += 1;
			else if (ri.type == RobotType.TURRET)
				numAlliedTurretsInRange += 1;
		}
		int[] packagedOutput = {numAlliedScoutsInRange, numAlliedTurretsInRange};
		return packagedOutput;
	}
	
	public static void doFreeScout() throws GameActionException
	{
		// if you see the turtle and no other scouts, then you are now a turtle scout
		int[] allyInfo = getNumAlliedScoutsAndTurretsInRange();
		int numAlliedScoutsInRange = allyInfo[0];
		int numAlliedTurretsInRange = allyInfo[1];
		if (numAlliedScoutsInRange == 0 && numAlliedTurretsInRange > 0)
		{
			isFreeScout = false;
			return;
		}
		
		// relay important sightings
		RobotInfo[] dens = doRelayDens();
		RobotInfo[] turrets = doRelayTurrets();
		
		if (!rc.isCoreReady())
			return;
		
		// pick a target: go to dens before spawn, go to enemies after
		int[] spawnRounds = rc.getZombieSpawnSchedule().getRounds();
		
		for (int round : spawnRounds) // go through zombie spawns
		{
			// if a spawn is in less than 20 rounds from now
			if (round - rc.getRoundNum() < 20 && rc.getRoundNum() - round > 0)
			{
				// rush the nearest den
				if (dens != null)
				{
					MapLocation closestDenLoc = dens[0].location;
					for (RobotInfo d : dens)
					{
						if (here.distanceSquaredTo(d.location) < here.distanceSquaredTo(closestDenLoc))
							closestDenLoc = d.location;
					}
					if (!Micro.tryAvoidBeingShot()) // avoidance micro only when we're not rushing stuff
			    	{
						NavSafetyPolicy safety = new SafetyPolicyAvoidZombies();
						Nav.goTo(closestDenLoc, safety);
						return;
			    	}
					return;
				}
			}
			
			// if a spawn happened less than 200 rounds ago
			if (rc.getRoundNum() - round < 200 && rc.getRoundNum() - round > 0)
			{
				// rush the nearest enemies
				if (turrets != null)
				{
					MapLocation closestTurretLoc = turrets[0].location;
					for (RobotInfo t : turrets)
					{
						if (here.distanceSquaredTo(t.location) < here.distanceSquaredTo(closestTurretLoc))
							closestTurretLoc = t.location;
					}
					NavSafetyPolicy safety = new SafetyPolicyRecklessAbandon();
					Nav.goTo(closestTurretLoc, safety);
					return;
				}
				RobotInfo[] nearbyEnemies = rc.senseNearbyRobots(here, RobotType.SCOUT.sensorRadiusSquared, theirTeam);
				if (nearbyEnemies != null && nearbyEnemies.length > 0)
				{
					MapLocation closestEnemyLoc = nearbyEnemies[0].location;
					for (RobotInfo en : nearbyEnemies)
					{
						if (here.distanceSquaredTo(en.location) < here.distanceSquaredTo(closestEnemyLoc))
							closestEnemyLoc = en.location;
					}
					NavSafetyPolicy safety = new SafetyPolicyRecklessAbandon();
					Nav.goTo(closestEnemyLoc, safety);
					return;
				}
			}
		}
		
		// and if we're still not involved in the action, go away from rally
		if (!Micro.tryAvoidBeingShot()) // avoidance micro only when we're not rushing stuff
    	{
			NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnits();
			Nav.goTo(here.add(here.directionTo(rally).opposite()), safety);
    	}
		
	}
	
	public static RobotInfo[] doRelayTurrets() throws GameActionException
	{
		// read signals and rebroadcast enemy turrets
		ArrayList<SignalLocation> knownEnemyTurretSignalLocs = Message.enemyTurretLocs;
        if (knownEnemyTurretSignalLocs != null && knownEnemyTurretSignalLocs.size() > 0)
        {
	        RobotInfo[] turrets = new RobotInfo[knownEnemyTurretSignalLocs.size()];
        	for (int i=0; i<knownEnemyTurretSignalLocs.size(); i++)
	        {
	        	MapLocation loc = knownEnemyTurretSignalLocs.get(i).loc;
	        	turrets[i] = new RobotInfo(0, theirTeam, RobotType.TURRET, loc, 0, 0, RobotType.TURRET.attackPower, RobotType.TURRET.maxHealth, RobotType.TURRET.maxHealth, 0, 0); 
	        }
	        sendSightedTarget(turrets);
	        return turrets;
        }
        return null;
	}
	
	public static RobotInfo[] doRelayDens() throws GameActionException
	{
        // dens
        ArrayList<SignalLocation> knownDenSignalLocs = Message.zombieDenLocs;
        if (knownDenSignalLocs != null && knownDenSignalLocs.size() > 0)
        {
	        RobotInfo[] dens = new RobotInfo[knownDenSignalLocs.size()];
        	for (int i=0; i<knownDenSignalLocs.size(); i++)
	        {
	        	MapLocation loc = knownDenSignalLocs.get(i).loc;
	        	dens[i] = new RobotInfo(0, Team.ZOMBIE, RobotType.ZOMBIEDEN, loc, 0, 0, RobotType.ZOMBIEDEN.attackPower, RobotType.ZOMBIEDEN.maxHealth, RobotType.ZOMBIEDEN.maxHealth, 0, 0); 
	        }
	        sendSightedTarget(dens);
	        return dens;
        }
        return null;
	}
	
	public static void doTurtleScout() throws GameActionException
	{
		// go to edge of turtle by moving away from rally location but staying in turtle
		NavSafetyPolicy safety = new SafetyPolicyAvoidAllUnitsAndStayInTurtle();
		if (getNumTurretsNearby(here, 6) > 7 || getNumTurretsNearby(here, 3) < 2) // we're not at the edge
		{
			Nav.goTo(here.add(here.directionTo(rally).opposite()), safety); // move only if we're not at the edge
			//Nav.goTo(MapUtil.findClosestTurtle(), safety);
			
			// free the scout if it is not at the turtle edge and sees more than one other scout
			int[] allyInfo = getNumAlliedScoutsAndTurretsInRange();
			int numAlliedScoutsInRange = allyInfo[0];
			if (numAlliedScoutsInRange > 1)
				isFreeScout = true;
		}
		//Nav.goTo(MapUtil.findClosestTurtle(), safety);
	}
	
	public static int getNumTurretsNearby(MapLocation loc, int squareDist)
    {
    	RobotInfo units[] = rc.senseNearbyRobots(loc, squareDist, ourTeam);
    	int numTurretsAdjacent = 0;
    	for (RobotInfo friend : units)
    	{
    		if (friend.type == RobotType.TURRET)
    			numTurretsAdjacent += 1;
    	}
    	return numTurretsAdjacent;
    }
	
	public static boolean sendSightedTarget(RobotInfo[] nearbyTargets) throws GameActionException
	{
		RobotInfo closestTarget = null;
		
		// prioritize the target with the biggest attack radius
		for (RobotInfo ri : nearbyTargets)
		{
			if (closestTarget == null || ri.type.attackRadiusSquared > closestTarget.type.attackRadiusSquared)
				closestTarget = ri;
		}
		
		if (closestTarget != null)
		{
			switch (closestTarget.type)
			{
				case TURRET:
					Message.sendMessageSignal(7,MessageType.ENEMY_TURRET,closestTarget.location);
					break;
				case ZOMBIEDEN:
					Message.sendMessageSignal(7,MessageType.ZOMBIE_DEN,closestTarget.location);
					break;
				default:
					Message.sendMessageSignal(7,MessageType.SIGHT_TARGET,closestTarget.location);
					break;
			}
			return true;
		}
		
		return false;
	}
	
	public static void turn() throws GameActionException
	{
        // send out info about sighted targets
		sendSightedTarget(rc.senseHostileRobots(here, RobotType.SCOUT.sensorRadiusSquared));
		
		if (rc.isCoreReady())
        {
        	if (isFreeScout)
        	{
        		doFreeScout();
        	}
        	else
        	{
        		if (!Micro.tryAvoidBeingShot()) // avoidance micro
            	{
        			doTurtleScout();
            	}
        	}
        }
	}
}
