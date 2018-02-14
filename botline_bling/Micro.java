package botline_bling;

import java.util.ArrayList;

import battlecode.common.*;

public class Micro extends RobotPlayer
{
	// Main useful methods:
	// tryRetreatFromEveryone()
	// tryAvoidBeingShot()
	// tryRetreatIfOverpowered()
	
	public static Direction dirToClosestZombie = Direction.NONE;
	public static Direction dirToClosestEnemy = Direction.NONE;
	public static RobotInfo[] nearbyEnemies = new RobotInfo[0];
	public static RobotInfo[] nearbyZombies = new RobotInfo[0];
	public static RobotInfo[] nearbyAllies = new RobotInfo[0];
	public static boolean amISafe = true;
	private static double enemyTotalDamagePerTurn = 0;
	private static double zombieTotalDamagePerTurn = 0;
	private static double allyTotalDamagePerTurn = 0;
	private static double enemyTotalHealth = 0;
	private static double zombieTotalHealth = 0;
	private static double allyTotalHealth = 0;
	private static int turnsUntilFirstZombieAttacks = 100;
	private static RobotInfo enemyPriorityTarget = null;
	private static RobotInfo closestEnemy = null;
	private static RobotInfo closestZombie = null;
	private static RobotInfo lowestHealthEnemy = null;
	private static RobotInfo lowestHealthZombie = null;
	private static boolean enemyViperInSightRange = false;
	private static boolean inZombieSensorRange = false;
	
	public static void updateEnemyInfo() throws GameActionException
	{
		nearbyEnemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, theirTeam);
        nearbyZombies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Team.ZOMBIE);
        
        // additionally make use of scouted enemy turrets by adding them to enemy list
        ArrayList<SignalLocation> knownEnemyTurretSignalLocs = Message.enemyTurretLocs;
        if (knownEnemyTurretSignalLocs != null && knownEnemyTurretSignalLocs.size() > 0)
        {
	        RobotInfo[] enemiesNearAndFar = new RobotInfo[nearbyEnemies.length+knownEnemyTurretSignalLocs.size()];
        	for (int i=0; i<nearbyEnemies.length; i++)
        	{
        		enemiesNearAndFar[i] = nearbyEnemies[i];
        	}
	        for (int i=0; i<knownEnemyTurretSignalLocs.size(); i++)
	        {
	        	MapLocation loc = knownEnemyTurretSignalLocs.get(i).loc;
	        	enemiesNearAndFar[i+nearbyEnemies.length] = new RobotInfo(0, theirTeam, RobotType.TURRET, loc, 0, 0, RobotType.TURRET.attackPower, RobotType.TURRET.maxHealth, RobotType.TURRET.maxHealth, 0, 0); 
	        }
	        nearbyEnemies = enemiesNearAndFar; // replace with concatenated array
        }
	}
	
	public static void updateEnemyInfo(RobotInfo[] opponents, RobotInfo[] zombies) throws GameActionException
	{
		nearbyEnemies = opponents;
		nearbyZombies = zombies;
	}
	
	public static void updateAllies() throws GameActionException
	{
		// find allies within sensor range
		nearbyAllies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, ourTeam);
		
		// go though and tally up firepower
		RobotInfo me = rc.senseRobot(rc.getID());
		allyTotalDamagePerTurn = ( me.attackPower / (me.type.attackDelay + 1) );
		allyTotalHealth = me.health;
		if (nearbyAllies.length > 0)
		{
			for (RobotInfo friend : nearbyAllies)
			{
				allyTotalDamagePerTurn += ( friend.attackPower / (friend.type.attackDelay + 1) );
				allyTotalHealth += friend.health;
			}
		}
	}
	
	public static boolean tryAvoidBeingShot() throws GameActionException
	{
		collateNearbyRobotInfo();
		
		// first priority is to attack an enemy archon
		if (enemyPriorityTarget != null)
			if (!tryAttackBot(enemyPriorityTarget))
				return tryRetreat();
		
		// deal with the case of zombies nearby
		if (!amISafe)
		{
			if (!tryRetreat())
				return tryAttackSomebody();
			else
				return true;
		}
		
		// did nothing
		return false;
	}
	
	public static boolean tryRetreatFromEveryone() throws GameActionException
	{
		collateNearbyRobotInfo();
		NavSafetyPolicy saftey = new SafetyPolicyAvoidAllUnits();
		
		// first priority is to attack an enemy archon
		if (enemyPriorityTarget != null)
			return tryAttackBot(enemyPriorityTarget);
		
		// deal with the case of zombies nearby
		if (nearbyEnemies.length > 0 || nearbyZombies.length > 0)
			if (!tryRetreat())
				return tryAttackSomebody();
			else
				return true;
		
		// did nothing
		return false;
	}
	
	public static boolean tryRetreatIfOverpowered() throws GameActionException
	{
		collateNearbyRobotInfo();
		
		// first priority is to attack an enemy archon
		if (enemyPriorityTarget != null)
			return tryAttackBot(enemyPriorityTarget);
		
		// deal with the case of zombies nearby
		if (nearbyZombies.length > 0)
		{
			if (amOverpowered())
			{
				// first try to retreat, if we cannot, then attack
				if (!tryRetreat())
					return tryAttackSomebody();
				else
					return true;
			}
			else
			{
				if (willDieInfected())
					return tryRushEnemies();
				else
				{
					if (!tryRetreat())
						return tryAttackSomebody();
					else
						return true;
				}
			}
		}
		
		// deal with the case of enemies nearby
		if (nearbyEnemies.length > 0)
		{
			if (amOverpowered())
			{
				// first try to retreat, if we cannot, then attack
				if (!tryRetreat())
					return tryAttackSomebody();
				else
					return true;
			}
			else
			{
				return tryAttackSomebody();
			}
		}
		
		// did nothing
		return false;
	}
	
	public static void doAvoidDyingInfectedAtAnyCost() throws GameActionException
	{
		collateNearbyRobotInfo();
		
		// check if we're in danger of dying infected
		if (!willDieInfected())
		{
			if (imminentInfection())
			{
				// check if we can retreat to prevent infection
				if (!tryRetreat())
					// otherwise commit suicide
					rc.disintegrate();
			}
			else // we are not in danger
			{
				if (nearbyZombies.length==0)
					tryAttackSomebody();
				else
				{
					if (!tryRetreat())
						tryAttackSomebody();
				}
			}
		}
		else // we will die infected, try to rush enemies
		{
			if (!tryRushEnemies())
				tryAttackSomebody();
		}
		
	}
	
	public static boolean tryKiteZombies(MapLocation target) throws GameActionException
	{
		// fool around avoiding things until we are seen by zombies, then kite toward target
		
		// update enemies
		collateNearbyRobotInfo();
		
		// check if we can be seen by zombies
		if (inZombieSensorRange)
		{
			// if so, kite toward target
			NavSafetyPolicy safety = new SafetyPolicyAvoidZombies(nearbyZombies);
			Nav.goTo(target, safety);
			return true;
		}
		else
		{
			// skittish behavior, avoidance tactics
			// first try to retreat, if we cannot, then attack
			if (!tryRetreat())
				tryAttackSomebody();
			return false;
		}
	}
	
	public static boolean amOverpowered() throws GameActionException
	{
		// add up friendly firepower
		updateAllies();
		
		// compare enemy firepower with ours
		double damageWeInflictBeforeDeath = allyTotalDamagePerTurn / ( rc.getHealth() / (enemyTotalDamagePerTurn + zombieTotalDamagePerTurn) );
		double damageTheyInflictBeforeDeath = enemyTotalDamagePerTurn / (enemyTotalHealth / allyTotalDamagePerTurn);
		return ( damageWeInflictBeforeDeath > damageTheyInflictBeforeDeath );
	}

	public static int howLongCanSurviveCurrentSkirmish() throws GameActionException
	{
		// add up friendly firepower
		updateAllies();
		
		// figure out about how many rounds skirmish will take
		int roundsToWin = (int) ( (enemyTotalHealth + zombieTotalHealth) / allyTotalDamagePerTurn );
		int roundsToLose = (int) ( allyTotalHealth / (enemyTotalDamagePerTurn + zombieTotalDamagePerTurn) );
		
		// return the number of rounds after which the skirmish will be over
		return Math.min(roundsToWin, roundsToLose);
	}
	
	public static boolean imminentInfection() throws GameActionException
	{
		return ( (turnsUntilFirstZombieAttacks < 2) || enemyViperInSightRange);
	}
	
	public static boolean willDieInfected() throws GameActionException
	{
		// get self infection status
		int healingTime = 0; // rc.getInfectedTurns();
		int roundsTillDeath = howLongCanSurviveCurrentSkirmish();
		
		// if another infection is imminent, add to healing time
		if (imminentInfection())
			healingTime += 10;
		
		// return whether we will be infected when we die
		return (roundsTillDeath <= healingTime);

	}
	
	public static boolean tryMove(MapLocation loc, NavSafetyPolicy safety) throws GameActionException
	{
		Nav.goTo(loc, safety);
		if (here.equals(rc.getLocation())) // we didn't move
			return false;
		else
			return true;
	}
	
	public static boolean tryRetreat() throws GameActionException
	{
		if (!rc.isCoreReady())
			return false;
		
		NavSafetyPolicy safety = new SafetyPolicyRecklessAbandon();
		
		// get the retreat direction
		Direction retreatDir = null;
		if (dirToClosestEnemy == Direction.NONE && dirToClosestZombie == Direction.NONE) // no attackers
			return false;
		
		// retreat if we can within the bounds of the safety policy
		MapLocation retreatLoc = here.add(dirToClosestEnemy.opposite()).add(dirToClosestZombie.opposite());
		return (tryMove(retreatLoc, safety));
	}
	
	public static boolean tryRushEnemies() throws GameActionException
	{
		// bum rush enemies in sight range while avoiding zombies
		NavSafetyPolicy safety = new SafetyPolicyAvoidZombies(nearbyZombies);
		if (dirToClosestEnemy != null)
			return tryMove(here.add(dirToClosestEnemy), safety);
		
		return false;
	}
	
	public static boolean tryAttackBot(RobotInfo targetBot) throws GameActionException
	{
		if (targetBot == null)
			return false;
		if (rc.canAttackLocation(targetBot.location))
		{
			rc.attackLocation(targetBot.location);
			return true;
		}
		return false;
	}
	
	public static boolean tryAttackSomebody() throws GameActionException
	{
		// try to attack someone in weapons range
		// this method SHOULD BE CALLED AFTER CALLING collateNearbyRobotInfo()
		// this method can be called from outside without doing anything first, it will just default to shooting at bot with least health
		
		// can we even shoot bro?
		if (!rc.isWeaponReady())
			return false;
		
		// use the info we've collated
		// priority: enemy archon, zombies, enemies
		if (tryAttackBot(enemyPriorityTarget))
			return true;
		if (tryAttackBot(lowestHealthZombie))
			return true;
		if (tryAttackBot(lowestHealthEnemy))
			return true;
		
		// if this is called from outside Micro.java, and the targets are not set, just default to this:
		RobotInfo[] localEnemies = rc.senseHostileRobots(here, rc.getType().attackRadiusSquared);
		if (localEnemies.length == 0)
			return false;
		// find enemy with lowest health and try to attack
		RobotInfo target = localEnemies[0];
		for (RobotInfo bot : localEnemies)
		{
			if (bot.health < target.health)
				target = bot;
		}
		if (tryAttackBot(target))
			return true;
		
		return false;
	}
	
	private static void collateNearbyRobotInfo() throws GameActionException
	{
		// get robot info
		updateEnemyInfo();
		
		// loop through known enemy turret locations
		
		
		// loop through zombies
		zombieTotalDamagePerTurn = 0;
		zombieTotalHealth = 0;
		lowestHealthZombie = null;
		closestZombie = null;
		dirToClosestZombie = Direction.NONE;
		inZombieSensorRange = false;
		if (nearbyZombies.length>0)
		{
			closestZombie = nearbyZombies[0];
			lowestHealthZombie = nearbyZombies[0];
			for (RobotInfo zombie : nearbyZombies)
			{
				// update the closest zombie
				if (here.distanceSquaredTo(zombie.location) < here.distanceSquaredTo(closestZombie.location))
					closestZombie = zombie;
				
				// add up the firepower of relevant zombies
				if (here.distanceSquaredTo(zombie.location) <= zombie.type.attackRadiusSquared)
					zombieTotalDamagePerTurn += (zombie.attackPower / (zombie.type.attackDelay+1));
				
				// keep track of the zombie with lowest health
				if (zombie.health < lowestHealthZombie.health)
					lowestHealthZombie = zombie;
				
				// check if we are in zombie sensor range
				if (inZombieSensorRange == false && here.distanceSquaredTo(zombie.location) <= zombie.type.sensorRadiusSquared)
					inZombieSensorRange = true;
				
				// add up total health
				zombieTotalHealth += zombie.health;
			}
			
			// figure out how long until closest zombie attacks
			RobotInfo[] alliesInClosestZombieSightRange = rc.senseNearbyRobots(closestZombie.location, closestZombie.type.sensorRadiusSquared, ourTeam);
			if (alliesInClosestZombieSightRange.length > 0)
			{
				// loop through to find the ally that the closest zombie will attack
				RobotInfo allyTargetOfClosestZombie = alliesInClosestZombieSightRange[0];
				for (RobotInfo allyTarget : alliesInClosestZombieSightRange)
				{
					if (closestZombie.location.distanceSquaredTo(allyTarget.location) < closestZombie.location.distanceSquaredTo(allyTargetOfClosestZombie.location))
						allyTargetOfClosestZombie = allyTarget;
				}
				
				if (rc.getID() != allyTargetOfClosestZombie.ID)
				{
					// if the closest zombie's target is not me, calculate how many turns it will be engaged with them
					turnsUntilFirstZombieAttacks = (int) ( allyTargetOfClosestZombie.health / (closestZombie.attackPower / (closestZombie.type.attackDelay+1)) );
				}
			}
			
			// compute direction to closest zombie
			dirToClosestZombie = here.directionTo(closestZombie.location);
		}
		
		// loop through enemy team
		enemyTotalDamagePerTurn = 0;
		enemyTotalHealth = 0;
		lowestHealthEnemy = null;
		closestEnemy = null;
		dirToClosestEnemy = Direction.NONE;
		enemyViperInSightRange = false;
		if (nearbyEnemies.length>0)
		{
			// reset priority target
			enemyPriorityTarget = null;
			closestEnemy = nearbyEnemies[0];
			lowestHealthEnemy = nearbyEnemies[0];
			
			// go through enemies, find targets, count up enemy firepower
			for (RobotInfo enemy : nearbyEnemies)
			{
				// look for archon enemy targets
				if (rc.getType().attackRadiusSquared >= here.distanceSquaredTo(enemy.location))
				{
					if (enemy.type == RobotType.ARCHON)
						enemyPriorityTarget = enemy;
				}
				
				// update the closest enemy
				if (here.distanceSquaredTo(enemy.location) < here.distanceSquaredTo(closestEnemy.location))
					closestEnemy = enemy;
				
				// count up enemy firepower
				if (here.distanceSquaredTo(enemy.location) <= enemy.type.attackRadiusSquared)
					enemyTotalDamagePerTurn += (enemy.attackPower / (enemy.type.attackDelay+1));
				
				// keep track of the enemy with lowest health
				if (enemy.health < lowestHealthEnemy.health)
					lowestHealthEnemy = enemy;
				
				// check for vipers
				if (enemy.type == RobotType.VIPER)
					enemyViperInSightRange = true;
				
				// add up total health
				enemyTotalHealth += enemy.health;
			}
			
			// compute direction to closest enemy
			dirToClosestEnemy = here.directionTo(closestEnemy.location);
			
		}
		
		//Debug.setStringSJF("enemy dir = " + dirToClosestEnemy.toString() + ", zombie dir = " + dirToClosestZombie.toString());
		
		// figure out if i am in shooting range of anyone
		boolean enemyInRange = ( closestEnemy != null && here.distanceSquaredTo(closestEnemy.location) <= closestEnemy.type.attackRadiusSquared );
		boolean zombieInRange = ( closestZombie != null && here.distanceSquaredTo(closestZombie.location) <= closestZombie.type.attackRadiusSquared );
		if (enemyInRange || zombieInRange)
			amISafe = false;
		else
			amISafe = true;
	}
	
	public static RobotInfo getClosestRobot(RobotInfo[] nearby, MapLocation loc)
	{
		int minsqdist = 1000;
		RobotInfo closest = null;
		
		for (RobotInfo ri : nearby)
		{
			if (ri.location.distanceSquaredTo(loc) < minsqdist)
			{
				closest = ri;
				minsqdist = ri.location.distanceSquaredTo(loc);
			}
		}
		
		return closest;
	}
	
	public static RobotInfo getClosestTurretTarget(RobotInfo[] nearby, MapLocation loc)
	{
		int minsqdist = 1000;
		RobotInfo closest = null;
		
		for (RobotInfo ri : nearby)
		{
			int dist = ri.location.distanceSquaredTo(loc);
			
			if (dist < minsqdist && dist > 5)
			{
				closest = ri;
				minsqdist = dist;
			}
		}
		
		return closest;
	}
	
	public static MapLocation getUnitCOM(RobotInfo[] nearby)
	{
		int xtot = 0;
		int ytot = 0;
		
		if (nearby.length == 0)
			return here;
		
		for (RobotInfo ri : nearby)
		{
			xtot += ri.location.x;
			ytot += ri.location.y;
		}
		
		xtot /= nearby.length;
		ytot /= nearby.length;
		
		Debug.setStringTS("AA" + new MapLocation(xtot,ytot));
		
		return new MapLocation(xtot,ytot);
	}
}
