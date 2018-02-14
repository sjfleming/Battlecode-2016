package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class StratUnitCombat extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	private MapLocation lastDest = null;
	private String myTask = "";
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Fighting: " + myTask + " at " + lastDest;
	}
	
	public boolean tryTurn() throws GameActionException
	{
		// do we have a strategy that takes precedence over this one?
		if (overrideStrategy != null)
		{
			if (overrideStrategy.tryTurn())
				return true;
			else
				overrideStrategy = null;
		}
		
		// retreat to the archon?
		double healthleft = rc.getHealth() - 2*rc.getViperInfectedTurns();
		if (healthleft < 0)
		{
			overrideStrategy = new StratUnitRush();
			overrideStrategy.tryTurn();
			return true;
		}
		if (healthleft < 15 && Micro.getFriendlyUnits().Archons == 0)
		{
			overrideStrategy = new StratUnitRetreat();
			overrideStrategy.tryTurn();
			return true;
		}
		
		// just took heavy mystery damage?
		if (tookHeavyDamageLastRound && Micro.getRoundsUntilDanger() > 10)
		{
			MapLocation retreatDest = here.add(lastMovedDirection.opposite(),10);
			overrideStrategy = new StratTempRetreat(retreatDest, 30);
			overrideStrategy.tryTurn();
			return true;
		}
		
		// are we a viper and do we want to rush? answer: yes
		// TS - answer: no
		/*
		if (rc.getType() == RobotType.VIPER && Zombie.isSpawnRound())
		{
			overrideStrategy = new StratViperRush();
			overrideStrategy.tryTurn();
			return true;
		}
		*/
		
		// any vipers or turrets? rush 'em
		if (Micro.getEnemyUnits().TurrTTMs > 0 || Micro.getEnemyUnits().Vipers > 0)
		{
			for (RobotInfo ri : Micro.getNearbyEnemies())
			{
				if (ri.type == RobotType.TURRET || ri.type == RobotType.TTM || (ri.type == RobotType.VIPER && rc.getViperInfectedTurns() > 0))
				{
					overrideStrategy = new StratTempRush(ri.ID);
					overrideStrategy.tryTurn();
					return true;
				}
			}
		}
		
		// target priority: nearby enemy > nearby ally > zombie den > our archon stuff
		lastDest = null;
		RobotInfo closestUnit = Micro.getClosestUnitTo(Micro.getNearbyHostiles(), here);
		if (closestUnit != null)
		{
			myTask = "enemy";
			lastDest = closestUnit.location;
		}

		if (lastDest == null)
		{
			myTask = "ally";
			lastDest = Message.getClosestAllyUnderAttack();
		}
		if (lastDest == null)
		{
			myTask = "archon";
			lastDest = MapInfo.getClosestNeutralArchon();
		}
		if (lastDest == null)
		{
			myTask = "den";
			lastDest = MapInfo.getClosestDen();
		}
		if (lastDest == null)
		{
			myTask = "enemy";
			//lastDest = Sighting.getClosestTurret();
			lastDest = Waypoint.getBestEnemyLocation();
		}
		if (lastDest == null)
		{
			myTask = "rally";
			lastDest = MapInfo.ourArchonCenter;
		}
		
		DirectionSet bufferDirs = Micro.getBufferDirs();
		bufferDirs = bufferDirs.and(Micro.getTurretSafeDirs());
		
		// if overpowered, kite back
		//if (Micro.amOverpowered())
		//	Action.tryRetreatOrShootIfStuck();
		
		// only shoot if we're safe here
		if (bufferDirs.isValid(Direction.NONE) && Action.tryAttackSomeone())
			return true;
		
		// don't move if we're safe and just shot
		if (bufferDirs.isValid(Direction.NONE) && !rc.isWeaponReady())
		{
			if (Micro.getRoundsUntilDanger() > 10)
			{
				Direction d = bufferDirs.getDirectionTowards(here,lastDest);
				if (d != null)
					Action.tryMove(d);
			}
			return true;
		}
		
		// can we actually move anywhere? then do so
		if (bufferDirs.any())
		{
			Nav.tryGoTo(lastDest, bufferDirs);
		}
		else if (!Micro.getSafeMoveDirs().isValid(Direction.NONE))
		{
			// if we're not safe here, we should move
			lastDest = Waypoint.getClosestFriendlyWaypoint();
			if (lastDest == null)
				lastDest = MapInfo.farthestArchonLoc;
			Action.tryGoToSafestOrRetreat(lastDest);
		}
		
		// or shoot if we couldn't move
		if (!Action.tryAttackSomeone() && rc.isCoreReady())
		{
			MapLocation[] parts = Micro.getNearbyParts();
			if (parts != null && parts.length > 0)
			{
				MapLocation closestLocalPart = Micro.getClosestLocationTo(parts, here);
				if (closestLocalPart != null)
					Rubble.doClearRubble(here.directionTo(closestLocalPart));
			}
			else
			{
				Rubble.doClearRubble(Rubble.getRandomAdjacentRubble());
			}
		}
		
		return true;
	}
}
