package team023;

import battlecode.common.*;

import java.util.*;

public class StratViperRush extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	
	private MapLocation lastDest = null;
	private MapLocation startingLoc = null;
	private MapLocation enemyLoc = null;
	private MapLocation farArchon = null;
	private MapLocation[] enemyArchonLocs = null;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Viper rush: " + lastDest;
	}
	
	public StratViperRush()
	{
		enemyArchonLocs = rc.getInitialArchonLocations(theirTeam);
		enemyLoc = Micro.getClosestLocationTo(enemyArchonLocs,here);
		farArchon = Micro.getFarthestLocationFrom(enemyArchonLocs,here);
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
		if (rc.isWeaponReady() && ((rc.getHealth() <= Micro.getNearbyAllies().length * 13 * rc.getWeaponDelay() 
				&& rc.getInfectedTurns() < rc.getHealth()/(13*Micro.getNearbyAllies().length))
				|| Micro.getEnemyUnits().Turrets > 0))
			rc.attackLocation(here);
		
		// attack
		// (after round 200, rushing vipers will shoot anything)
		if (rc.getRoundNum() > 200 || here.distanceSquaredTo(enemyLoc) < here.distanceSquaredTo(startingLoc))
			Action.tryViperAttack();
        
        RobotInfo[] enemies = Micro.getNearbyEnemies();
        
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
        	{
        		Nav.tryGoTo(enemyturretloc, Micro.getCanMoveDirs());
        	}
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
        	Nav.tryGoTo(enemyLoc, Micro.getCanMoveDirs());
        else
        	Nav.tryGoTo(enemyLoc, Micro.getBestAnyDirs());
        
        return true;

	}
}
