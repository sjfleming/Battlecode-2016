package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class MicroBase extends RobotPlayer
{
	private RobotInfo[] nearbyEnemies = null;
	private RobotInfo[] nearbyZombies = null;
	private RobotInfo[] nearbyHostiles = null;
	private RobotInfo[] nearbyAllies = null;
	private MapLocation[] nearbyParts = null;
	
	private DirectionSet canMoveDirs = null;
	private DirectionSet fastMoveDirs = null;
	private DirectionSet safeMoveDirs = null;

	private DirectionSet turretSafeDirs = null;
	private DirectionSet bufferDirs = null;
	private DirectionSet saferMoveDirs = null;
	
	private static final int DIST_MAX = 1000;
	private int[] distToClosestHostile = null;
	
	private Direction bestRetreatDirection = null;
	
	private int roundsUntilDanger = -1;
	private int amOverpowered = -1;
	
	private double allyTotalDamagePerTurn = -1;
	private double hostileTotalDamagePerTurn = -1;
	private double hostileTotalHealth = -1;
	
	private UnitCounts nearbyFriendlyCounts = null;
	private UnitCounts nearbyEnemyCounts = null;
	
	private static final int[] typePriorities = 
		{
			-1,	//0: ZOMBIEDEN
			0,	//1: STANDARDZOMBIE
			2,	//2: RANGEDZOMBIE
			1,	//3: FASTZOMBIE
			3,	//4: BIGZOMBIE
			9,	//5: ARCHON
			2,	//6: SCOUT
			2,	//7: SOLDIER
			1,	//8: GUARD
			10,	//9: VIPER
			10,	//10: TURRET
			8	//11: TTM
		};
	
	private static int[] bufferDist = 
		{
			2,	//0: ZOMBIEDEN
			8,	//1: STANDARDZOMBIE
			8,	//2: RANGEDZOMBIE
			2,	//3: FASTZOMBIE
			8,	//4: BIGZOMBIE
			0,	//5: ARCHON
			2,	//6: SCOUT
			8,	//7: SOLDIER
			8,	//8: GUARD
			8,	//9: VIPER
			2,	//10: TURRET
			2	//11: TTM
		};
	
	private static int[] saferDist = 
		{
			0,	//0: ZOMBIEDEN
			8,	//1: STANDARDZOMBIE
			24,	//2: RANGEDZOMBIE
			13,	//3: FASTZOMBIE
			8,	//4: BIGZOMBIE
			0,	//5: ARCHON
			0,	//6: SCOUT
			24,	//7: SOLDIER
			13,	//8: GUARD
			24,	//9: VIPER
			24,	//10: TURRET
			0	//11: TTM
		};

	private final static int[] archonSaferDist = 
		{
			0,	//0: ZOMBIEDEN
			8,	//1: STANDARDZOMBIE
			24,	//2: RANGEDZOMBIE
			35,	//3: FASTZOMBIE
			8,	//4: BIGZOMBIE
			0,	//5: ARCHON
			0,	//6: SCOUT
			24,	//7: SOLDIER
			13,	//8: GUARD
			24,	//9: VIPER
			35,	//10: TURRET
			0	//11: TTM
		};
	
	private static final int[] scoutSaferDist = 
		{
			0,	//0: ZOMBIEDEN
			8,	//1: STANDARDZOMBIE
			24,	//2: RANGEDZOMBIE
			48,	//3: FASTZOMBIE
			8,	//4: BIGZOMBIE
			0,	//5: ARCHON
			0,	//6: SCOUT
			24,	//7: SOLDIER
			13,	//8: GUARD
			35,	//9: VIPER
			40,	//10: TURRET
			0	//11: TTM
		};
	
	public MicroBase()
	{
		if (rc.getType() == RobotType.SCOUT)
			saferDist = scoutSaferDist;
		else if (rc.getType() == RobotType.ARCHON)
			saferDist = archonSaferDist;
	}
	
	public RobotInfo[] getNearbyEnemies()
	{
		if (nearbyEnemies != null)
			return nearbyEnemies;
		
		nearbyEnemies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, theirTeam);
		return nearbyEnemies;
	}
	
	public RobotInfo[] getNearbyAllies()
	{
		if (nearbyAllies != null)
			return nearbyAllies;
		
		nearbyAllies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, ourTeam);
		return nearbyAllies;
	}
	
	public RobotInfo[] getNearbyZombies()
	{
		if (nearbyZombies != null)
			return nearbyZombies;
		
		nearbyZombies = rc.senseNearbyRobots(rc.getType().sensorRadiusSquared, Team.ZOMBIE);
		return nearbyZombies;
	}
	
	public RobotInfo[] getNearbyHostiles()
	{
		if (nearbyHostiles != null)
			return nearbyHostiles;
		
		nearbyHostiles = rc.senseHostileRobots(here, rc.getType().sensorRadiusSquared);
		return nearbyHostiles;
	}
	
	public MapLocation[] getNearbyParts()
	{
		if (nearbyParts != null)
			return nearbyParts;
		
		nearbyParts = rc.sensePartLocations(rc.getType().sensorRadiusSquared);
		return nearbyParts;
	}
		
	// this gets called by other functions to compile various stats and info
	// before they return the computed values
	private void computeSafetyStats()
	{
		// already done
		if (distToClosestHostile != null)
			return;
		
		RobotInfo[] nearby = getNearbyHostiles();
		
		// this is used for best retreat direction calculation
		distToClosestHostile = new int[9];
		
		safeMoveDirs = new DirectionSet();
		saferMoveDirs = new DirectionSet();
		bufferDirs = new DirectionSet();
		canMoveDirs = new DirectionSet();
		fastMoveDirs = new DirectionSet();
		turretSafeDirs = Sighting.getTurretSafeDirs(distToClosestHostile);
		
		for (Direction d : Direction.values())
		{
			if (d == Direction.OMNI)
				continue;
			
			// skip can't move directions, except keep none
			if (d != Direction.NONE && !rc.canMove(d))
				continue;
			
			canMoveDirs.add(d);
			if (rc.senseRubble(here.add(d)) < GameConstants.RUBBLE_SLOW_THRESH)
				fastMoveDirs.add(d);
			
			MapLocation testloc = here.add(d);
			
			int closestDistSq = distToClosestHostile[d.ordinal()];
			// if it wasn't set by turretsafedirs
			if (closestDistSq == 0)
				closestDistSq = DIST_MAX;
			
			boolean isThisSquareSafe = true;
			boolean isThisSquareSafer = true;
			boolean isThisSquareBuffer = true;
			
			for (RobotInfo ri : nearby)
			{				
				int distSq = testloc.distanceSquaredTo(ri.location);

				if (distSq <= bufferDist[ri.type.ordinal()])
					isThisSquareBuffer = false;
				
				if (distSq <= saferDist[ri.type.ordinal()])
					isThisSquareSafer = false;
				
				if (ri.attackPower == 0)
					continue;

				if (distSq < closestDistSq)
					closestDistSq = distSq;

				if (distSq <= ri.type.attackRadiusSquared)
				{
					isThisSquareSafe = false;
					if (ri.type == RobotType.TURRET)
						turretSafeDirs.remove(d);
				}
			}
			
			distToClosestHostile[d.ordinal()] = closestDistSq;
			if (isThisSquareBuffer)
				bufferDirs.add(d);
			
			if (isThisSquareSafe)
				safeMoveDirs.add(d);
			
			if (isThisSquareSafer)
				saferMoveDirs.add(d);
		}
		
		// scouts can go anywhere
		if (rc.getType() == RobotType.SCOUT)
			fastMoveDirs = canMoveDirs.clone();
		// obviously, if either is in turret range, it is neither
		// safe nor safer
		safeMoveDirs = safeMoveDirs.and(turretSafeDirs);
		saferMoveDirs = saferMoveDirs.and(turretSafeDirs);
	}
	
	// get number of rounds until we are in danger
	public int getRoundsUntilDanger()
	{
		if (roundsUntilDanger >= 0)
			return roundsUntilDanger;
		
		roundsUntilDanger = 100;
		
		for (RobotInfo ri : getNearbyHostiles())
		{
			int dangerTime = 100;
			
			switch (ri.type)
			{
			// noncombatants
			case ZOMBIEDEN:
			case ARCHON:
			case SCOUT:
				break;
				
			case TTM:
				dangerTime = (int)ri.coreDelay + GameConstants.TURRET_TRANSFORM_DELAY;
				break;
				
			// ranged units
			case SOLDIER:
			case RANGEDZOMBIE:
			case VIPER:
				// time to shoot us is rounds until
				// coreDelay + cooldownDelay
				MapLocation closer = ri.location;
				double dangerAccum = 0;
				while (closer.distanceSquaredTo(here) > ri.type.attackRadiusSquared)
				{
					Direction d = closer.directionTo(here);
					closer = closer.add(d);
					dangerAccum += ri.type.movementDelay*(d.isDiagonal() ? 1.4 : 1.0);
				}
				
				// if they have to move, include cooldown and current core
				if (dangerAccum > 0)
					dangerAccum += ri.type.cooldownDelay + ri.coreDelay - ri.type.movementDelay;
				else // otherwise just weapon delay
					dangerAccum += ri.weaponDelay;
				
				dangerTime = (int)dangerAccum;
				break;
				
			// unranged units
			case BIGZOMBIE:
			case FASTZOMBIE:
			case GUARD:
			case STANDARDZOMBIE:
				// move one square closer to enemy
				// the square enemy needs to get to to attack us
				MapLocation onecloser = here.add(here.directionTo(ri.location));
				int dx = Math.abs(onecloser.x-ri.location.x);
				int dy = Math.abs(onecloser.y-ri.location.y);

				// number of longest straight steps + 1.4 * number of diagonal steps
				// scaling of movement delay to get here
				double effDistanceTo = Math.min(dx, dy)*0.4 + Math.max(dx, dy); // verified
				
				dangerTime = (int)(ri.coreDelay + ri.type.movementDelay*(effDistanceTo-1) + ri.type.cooldownDelay);
				
				break;
				
			case TURRET:
				if (here.distanceSquaredTo(ri.location) <= ri.type.attackRadiusSquared)
					dangerTime = (int)(ri.weaponDelay);
				break;
			}
			
			if (dangerTime < roundsUntilDanger)
			{
				roundsUntilDanger = dangerTime;
				bestRetreatDirection = ri.location.directionTo(here);
			}
		}
		
		if (roundsUntilDanger < 0)
			roundsUntilDanger = 0;
		
		if (!Micro.getTurretSafeDirs().isValid(Direction.NONE))
			roundsUntilDanger = 0;
		
		if (roundsUntilDanger < 5)
			lastDangerRound = rc.getRoundNum();
		if (roundsUntilDanger > 20)
			lastSafeRound = rc.getRoundNum();
			
		
		return roundsUntilDanger;
	}
	
	// how many rounds until we can shoot and move at once
	public int getRoundsUntilShootAndMove()
	{
		return (int) ( Math.floor(rc.getWeaponDelay()) + Math.floor(rc.getCoreDelay() - 1) );
	}
	
	// compute which direction moves us the farthest from the closest enemy
	// (trying to use safe squares first)
	public Direction getBestEscapeDir()
	{
		DirectionSet dirs = getSafeMoveDirs().clone();
		
		// if there are no safe moves, just do something
		if (!dirs.any())
			dirs = getCanMoveDirs().clone();
		
		dirs.remove(Direction.NONE);
		
		if (!dirs.any())
			return null;
		
		Direction bestDir = null;
		int distToClosest = 0;
		
		for (Direction d : Direction.values())
		{
			if (!dirs.isValid(d))
				continue;
			
			if (distToClosestHostile[d.ordinal()] > distToClosest)
			{
				distToClosest = distToClosestHostile[d.ordinal()];
				bestDir = d;
			}
		}
		
		if (distToClosest == DIST_MAX)
			return null;
		
		return bestDir;
	}
	
	public Direction getBestRetreatDir()
	{
		// force recomputing of this
		getRoundsUntilDanger();
		
		return bestRetreatDirection;
	}
	
	public DirectionSet getBufferDirs()
	{
		computeSafetyStats();
		return bufferDirs;
	}

	public DirectionSet getTurretSafeDirs()
	{
		computeSafetyStats();
		return turretSafeDirs;
	}

	public DirectionSet getSafeMoveDirs()
	{
		computeSafetyStats();
		return safeMoveDirs;
	}
	
	public DirectionSet getSaferMoveDirs()
	{
		computeSafetyStats();
		return saferMoveDirs;
	}
	
	// get directions we can move in
	public DirectionSet getCanMoveDirs()
	{
		computeSafetyStats();
		return canMoveDirs;
	}
	
	public DirectionSet getCanFastMoveDirs()
	{
		computeSafetyStats();
		return fastMoveDirs;
	}
	
	// tries to return the best directions we can go that are safe
	public DirectionSet getBestSafeDirs()
	{
		Micro.computeSafetyStats();
		
		// start with safer, then safe, then just good ol' dirs
		DirectionSet dirs = saferMoveDirs.and(fastMoveDirs);
		if (dirs.any())
			return dirs;
		dirs = saferMoveDirs;
		if (dirs.any())
			return dirs;
		dirs = safeMoveDirs.and(fastMoveDirs);
		if (dirs.any())
			return dirs;

		dirs = safeMoveDirs;
		
		return dirs;
	}
	
	public DirectionSet getBestBufferDirs()
	{
		Micro.computeSafetyStats();
		// buffer dirs with fast move, then with regular move
		DirectionSet dirs = bufferDirs.and(fastMoveDirs).and(turretSafeDirs);
		if (dirs.any())
			return dirs;
		dirs = bufferDirs.and(turretSafeDirs);
		if (dirs.any())
			return dirs;
		
		return bufferDirs;
	}
	
	public DirectionSet getBestAnyDirs()
	{
		Micro.computeSafetyStats();
		
		// start with safer, then safe, then just good ol' dirs
		DirectionSet dirs = saferMoveDirs.and(fastMoveDirs);
		if (dirs.any())
			return dirs;
		dirs = saferMoveDirs;
		if (dirs.any())
			return dirs;
		dirs = safeMoveDirs.and(fastMoveDirs);
		if (dirs.any())
			return dirs;
		dirs = safeMoveDirs;
		if (dirs.any())
			return dirs;
		// out of safe moves, make it as non-dangerous as possible
		// by staying out of turret range
		dirs = turretSafeDirs.and(fastMoveDirs);
		if (dirs.any())
			return dirs;
		dirs = fastMoveDirs;
		if (dirs.any())
			return dirs;
		
		dirs = canMoveDirs;
		
		return dirs;
	}
	
	public double getAllyTotalDamagePerTurn() throws GameActionException
	{
		if (allyTotalDamagePerTurn != -1) // already did this calculation, just return the value
			return allyTotalDamagePerTurn;
		
		nearbyAllies = getNearbyAllies();
		
		allyTotalDamagePerTurn = rc.getType().attackPower / rc.getType().attackDelay;
		
		if (nearbyAllies == null || nearbyAllies.length == 0)
			return allyTotalDamagePerTurn;
		
		if (getNearbyHostiles().length != 0)
		{
			MapLocation closestHostileLoc = getClosestUnitTo(nearbyHostiles,here).location;
			for (RobotInfo ri : nearbyAllies)
			{
				allyTotalDamagePerTurn += ri.attackPower / ri.type.attackDelay / ((ri.location.distanceSquaredTo(closestHostileLoc)-ri.type.attackRadiusSquared)*1.4*ri.type.movementDelay + ri.type.cooldownDelay);
			}
		}
		else
		{
			for (RobotInfo ri : nearbyAllies)
			{
				allyTotalDamagePerTurn += ri.attackPower / ri.type.attackDelay;
			}
		}
		
		return allyTotalDamagePerTurn;
	}
	
	public double getHostileTotalDamagePerTurn() throws GameActionException
	{
		if (hostileTotalDamagePerTurn != -1) // already did this calculation, just return the value
			return hostileTotalDamagePerTurn;
		
		nearbyHostiles = getNearbyHostiles();
		
		hostileTotalDamagePerTurn = 0;
		
		if (nearbyHostiles == null || nearbyHostiles.length == 0)
			return hostileTotalDamagePerTurn;
		
		for (RobotInfo ri : nearbyHostiles)
		{
			if (here.distanceSquaredTo(ri.location) <= ri.type.attackRadiusSquared+1)
				hostileTotalDamagePerTurn += ri.type.attackPower / ri.type.attackDelay;
		}
		
		return hostileTotalDamagePerTurn;
	}
	
	public double getHostileTotalHealth() throws GameActionException
	{
		if (hostileTotalHealth != -1) // already did this calculation, just return the value
			return hostileTotalHealth;
		
		nearbyHostiles = getNearbyHostiles();
		hostileTotalHealth = 0;
		
		if (nearbyHostiles == null || nearbyHostiles.length == 0)
			return hostileTotalHealth;
		
		for (RobotInfo ri : nearbyHostiles)
		{
			hostileTotalHealth += ri.health;
		}
		
		return hostileTotalHealth;
	}
	
	public boolean amOverpowered() throws GameActionException
	{
		if (amOverpowered != -1) // already did this calculation, just return the value
			return (amOverpowered == 1);
		
		// calculate relevant stuff
		hostileTotalDamagePerTurn = getHostileTotalDamagePerTurn();
		//System.out.println("hostileTotalDamagePerTurn = " + hostileTotalDamagePerTurn);
		
		if (hostileTotalDamagePerTurn == 0)
		{
			amOverpowered = 0;
			return false;
		}
		
		allyTotalDamagePerTurn = getAllyTotalDamagePerTurn();
		hostileTotalHealth = getHostileTotalHealth();
		
		// compare enemy firepower with ours
		double damageWeInflictBeforeDeath = allyTotalDamagePerTurn * ( rc.getHealth() / (hostileTotalDamagePerTurn) );
		double damageTheyInflictBeforeDeath = hostileTotalDamagePerTurn * (hostileTotalHealth / allyTotalDamagePerTurn);
		if ( damageWeInflictBeforeDeath > damageTheyInflictBeforeDeath ) // (allyTotalDamagePerTurn > hostileTotalDamagePerTurn)
			amOverpowered = 0; // we are more powerful
		else
			amOverpowered = 1; // we are overpowered
		
		return (amOverpowered == 1);
	}
	
	public boolean isInDanger()
	{
		return (getRoundsUntilDanger() < 10);
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
	
	public UnitCounts getFriendlyUnits()
	{
		if (this.nearbyFriendlyCounts == null)
			this.nearbyFriendlyCounts = new UnitCounts(this.getNearbyAllies());
		return this.nearbyFriendlyCounts;
	}
	
	public UnitCounts getEnemyUnits()
	{
		if (this.nearbyEnemyCounts == null)
			this.nearbyEnemyCounts = new UnitCounts(this.getNearbyEnemies());
		return this.nearbyEnemyCounts;
	}
	
	public MapLocation getAllyCOM()
	{
		return getUnitCOM(this.getNearbyAllies());
	}
	
	public MapLocation getEnemyCOM()
	{
		return getUnitCOM(this.getNearbyEnemies());
	}
	
	public MapLocation getZombieCOM()
	{
		return getUnitCOM(this.getNearbyZombies());
	}
	
	public MapLocation getUnitCOM(RobotInfo[] nearby)
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
		
		return new MapLocation(xtot,ytot);
	}
	
	public RobotInfo getClosestUnitTo(RobotInfo[] nearby, MapLocation loc)
	{
		if (nearby.length == 0)
			return null;
		
		RobotInfo closest = null;
		int closestDistSq = 1000;
		
		for (RobotInfo ri : nearby)
		{
			int distSq = loc.distanceSquaredTo(ri.location);
			if (distSq < closestDistSq)
			{
				closest = ri;
				closestDistSq = distSq;
			}
		}
		
		return closest;
	}
	
	public MapLocation getClosestLocationTo(MapLocation[] locs, MapLocation center)
	{
		int minDistSq = 1000000;
		MapLocation closest = null;
		
		for (MapLocation ml : locs)
		{
			int dsq = ml.distanceSquaredTo(center);
			if (dsq < minDistSq)
			{
				minDistSq = dsq;
				closest = ml;
			}
		}
		
		return closest;
	}
	
	public MapLocation getClosestLocationTo(List<MapLocation> locs, MapLocation center)
	{
		int minDistSq = 1000000;
		MapLocation closest = null;
		
		for (MapLocation ml : locs)
		{
			int dsq = ml.distanceSquaredTo(center);
			if (dsq < minDistSq)
			{
				minDistSq = dsq;
				closest = ml;
			}
		}
		
		return closest;
	}
	
	public MapLocation getFarthestLocationFrom(List<MapLocation> locs, MapLocation center)
	{
		int maxDistSq = 0;
		MapLocation farthest = null;
		
		for (MapLocation ml : locs)
		{
			int dsq = ml.distanceSquaredTo(center);
			if (dsq > maxDistSq)
			{
				maxDistSq = dsq;
				farthest = ml;
			}
		}
		
		return farthest;
	}
	
	public MapLocation getFarthestLocationFrom(MapLocation[] locs, MapLocation center)
	{
		int maxDistSq = 0;
		MapLocation farthest = null;
		
		for (MapLocation ml : locs)
		{
			int dsq = ml.distanceSquaredTo(center);
			if (dsq > maxDistSq)
			{
				maxDistSq = dsq;
				farthest = ml;
			}
		}
		
		return farthest;
	}
	
	public RobotInfo getLowestHealth(RobotInfo[] bots)
	{
		RobotInfo target = null;
		
		for (RobotInfo ri : bots)
		{
			if (target == null || ri.health < target.health)
				target = ri;
			if (ri.type == RobotType.ARCHON)
			{
				target = ri;
				break; // archon targets take priority over ALL
			}
		}
		
		return target;
	}
	
	public RobotInfo getLowestHealthInMyRange(RobotInfo[] bots)
	{
		RobotInfo target = null;
		
		for (RobotInfo ri : bots)
		{
			if (here.distanceSquaredTo(ri.location) <= rc.getType().attackRadiusSquared)
			{
				if (target == null || ri.health < target.health)
					target = ri;
				if (ri.type == RobotType.ARCHON)
				{
					target = ri;
					break; // archon targets take priority over ALL
				}
			}
		}
		
		return target;
	}
	
	public RobotInfo getHighestPriorityTarget(RobotInfo[] bots)
	{
		RobotInfo target = null;
		
		for (RobotInfo ri : bots)
		{
			// can't attack this dude
			if (here.distanceSquaredTo(ri.location) > rc.getType().attackRadiusSquared)
				continue;
			
			// no target yet
			if (target == null)
			{
				target = ri;
				continue;
			}
			
			// archon targets take priority over ALL
			if (ri.type == RobotType.ARCHON)
			{
				target = ri;
				break;
			}
			
			double power = ri.attackPower/ri.type.attackDelay;
			double tgtpower = target.attackPower/target.type.attackDelay;
			// ri's damage/turn is less than current target, do not prefer
			if (power < tgtpower - 1e-8)
				continue;
			
			// greater than current, do prefer
			if (power > tgtpower + 1e-8)
			{
				target = ri;
				continue;
			}
		
			// they are the same, compare health
			if (ri.health < target.health)
				target = ri;
		}
		
		return target;
	}
	
	public DirectionSet getCanBuildDirectionSet(RobotType nextRobotType) throws GameActionException
	{
		if (nextRobotType == null)
			return new DirectionSet();
		
		// check nearby open squares
		DirectionSet valid = new DirectionSet();
		for (Direction dir : Direction.values()) // check all Directions around
		{
			if (rc.canBuild(dir, nextRobotType))
				valid.add(dir); // add this direction to the DirectionSet of valid build directions
		}
		return valid;
	}
	
	public RobotInfo getViperTarget(RobotInfo[] bots)
	{
		RobotInfo target = null;
		
		if (bots == null)
			return null;
		
		boolean attackScouts = (rc.getRoundNum() < 1000);
		
		for (RobotInfo ri : bots)
		{
			// can't attack this dude
			if (here.distanceSquaredTo(ri.location) > rc.getType().attackRadiusSquared)
				continue;
			
			if (!attackScouts && ri.type == RobotType.SCOUT)
				continue;
			
			// no target yet
			if (target == null)
			{
				target = ri;
				continue;
			}
			
			if (ri.viperInfectedTurns < target.viperInfectedTurns && ri.type != RobotType.ARCHON)
				target = ri;
				
		}
		
		if (target == null && bots.length > 0) // if there's only an archon, shoot it anyway
			target = bots[0];
		
		if (!attackScouts && target != null && target.type == RobotType.SCOUT)
			target = null;
		
		return target;
	}
}
