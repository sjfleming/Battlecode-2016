package neutered_blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class StratViperRush extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	
	private MapLocation lastDest = null;
	private MapLocation startingLoc = null;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Viper rush: " + lastDest;
	}
	
	public StratViperRush()
	{
		startingLoc = here;
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
		
		// shoot self if almost dead
		if (rc.isWeaponReady() && rc.getHealth() <= Micro.getNearbyAllies().length * 13 * rc.getWeaponDelay() 
				&& rc.getInfectedTurns() < rc.getHealth()/(13*Micro.getNearbyAllies().length))
			rc.attackLocation(here);
		
		// attack
		// (after round 200, rushing vipers will shoot anything)
		Action.tryViperAttack();
		
		// do we have a strategy that takes precedence over this one?
		if (overrideStrategy != null)
		{
			if (overrideStrategy.tryTurn())
				return true;
			else
				overrideStrategy = null;
		}
		
		// any vipers or turrets? rush 'em
		if (Micro.getEnemyUnits().TurrTTMs > 0)
		{
			for (RobotInfo ri : Micro.getNearbyEnemies())
			{
				if (ri.type == RobotType.TURRET || ri.type == RobotType.TTM)
				{
					overrideStrategy = new StratTempRush(ri.ID);
					overrideStrategy.tryTurn();
					return true;
				}
			}
		}
		
		// rush targets are visible enemies, then enemy locations, then random
		lastDest = null;
		RobotInfo closestUnit = Micro.getClosestUnitTo(Micro.getNearbyEnemies(), here);
		if (closestUnit != null)
			lastDest = closestUnit.location;

		if (lastDest == null)
			lastDest = Waypoint.getBestEnemyLocation();

		if (lastDest == null)
			lastDest = Waypoint.getRandomRetreatWaypoint();
		
		
		// use buffer dirs (so we can shoot enemies) if there are uninfected enemies and not too many of them
		DirectionSet bufferDirs = Micro.getBufferDirs();
		bufferDirs = bufferDirs.and(Micro.getTurretSafeDirs());
		
		int numuninfected = 0;
		int numcanattack = 0;
		for (RobotInfo ri : rc.senseNearbyRobots(rc.getType().attackRadiusSquared, theirTeam))
		{
			if (ri.viperInfectedTurns < 3)
				numuninfected++;
			if (ri.type.canAttack())
				numcanattack++;
		}
		
		// too many units or nobody to shoot, try to move safely
		if (numuninfected == 0 || numcanattack > 2 || !bufferDirs.any())
		{
			// try to move safely
			if (Action.tryGoToSafestOrRetreat(lastDest))
				return true;
		}

		// otherwise shoot
		Action.tryViperAttack();
		
		// and then buffer move
		Nav.tryGoTo(lastDest, bufferDirs);
		
		// if overpowered, kite back
		//if (Micro.amOverpowered())
		//	Action.tryRetreatOrShootIfStuck();
		
		// shoot if we're safe here
		/*
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
		*/
        /*RobotInfo[] enemies = Micro.getNearbyEnemies();
        
        // rush turrets to get so close they can't shoot
        UnitCounts count = Micro.getEnemyUnits();
        
        if (rc.getRoundNum() < 200 && count.TurrTTMs > 0)
        {
        	MapLocation enemyturretloc = null;
        	for (RobotInfo ri : enemies)
        	{
        		if (ri.type == RobotType.TURRET || ri.type == RobotType.TTM)
        		{
        			enemyturretloc = ri.location;
        			continue;
        		}
        	}
        	if (enemyturretloc != null)
        		Nav.tryGoTo(enemyturretloc, Micro.getCanMoveDirs());
        }
        
        // try to have only one enemy in attack range at a time
        if (enemies != null && enemies.length > 1)
        {
    		Direction retreatDir = Micro.getBestEscapeDir();
        	if (retreatDir == null)
        		Action.tryViperAttack();
        	else
        	{
        		if (!Nav.tryGoTo(here.add(retreatDir), Micro.getBestAnyDirs()))
        			Action.tryViperAttack();
        	}
        }
        else if (enemies != null && enemies.length > 0 && here.distanceSquaredTo(enemyLoc) < here.distanceSquaredTo(startingLoc))
        {
        	Debug.setStringRR("trying to go to enemy COM");
        	Nav.tryGoTo(Micro.getEnemyCOM(),Micro.getCanMoveDirs());
        }
        else if (here.distanceSquaredTo(enemyLoc) < 10 && (enemies == null || enemies.length == 0))
        {
        	Debug.setStringRR("close to enemy loc and no enemies");
        	if (enemyLoc.equals(farArchon))
        	{
        		farArchon = null;
        	}
        	if (farArchon == null)
        	{
        		enemyLoc = Message.recentEnemySignal;
        		if (enemyLoc == null)
        			enemyLoc = MapInfo.getRandomLocation();
        	}
        	else
        	{
        		enemyLoc = farArchon;
        	}
        }
        
        if (rc.getRoundNum() > 200)
        {
        	// go attack best enemy that we know about
        	enemyLoc = Waypoint.getBestEnemyLocation();
        	if (enemyLoc == null)
        		enemyLoc = Waypoint.getRandomRetreatWaypoint();
        }
        
        // try to go to enemy (will dig if necessary)
        if (rc.getRoundNum() < 200)
        	Nav.tryGoTo(enemyLoc, Micro.getBestAnyDirs());
        else
        	Nav.tryGoTo(enemyLoc, Micro.getBestSafeDirs());
        
        return true;*/

		
		return true;

	}
}
