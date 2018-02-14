package another_one_bots_the_dust;

import battlecode.common.*;

import java.util.*;

public class StratViperRush extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	
	private MapLocation lastDest = null;

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
		enemyLoc = enemyArchonLocs[0];
		farArchon = enemyArchonLocs[0];
		for (MapLocation loc : enemyArchonLocs)
		{
			if (here.distanceSquaredTo(loc) < here.distanceSquaredTo(enemyLoc))
				enemyLoc = loc;
			if (here.distanceSquaredTo(loc) > here.distanceSquaredTo(farArchon))
				farArchon = loc;
		}
		Debug.setStringSJF("target = " + enemyLoc.toString());
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
		if (rc.isWeaponReady() && rc.getHealth() <= Micro.getNearbyAllies().length * 13 * rc.getWeaponDelay() && rc.getInfectedTurns() < rc.getHealth()/(13*Micro.getNearbyAllies().length))
		{
			rc.attackLocation(here);
		}
		
		// attack
        Action.tryViperAttack();
        
        RobotInfo[] enemies = Micro.getNearbyEnemies();
        
        // rush turrets to get so close they can't shoot
        UnitCounts count = new UnitCounts(enemies);
        if (count.TurrTTMs > 0)
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
        	//Action.tryRetreatOrShootIfStuck();
        	Debug.setStringRR("retreating");
        	Direction retreatDir = Micro.getBestRetreatDir();
        	if (retreatDir == null)
        		retreatDir = Micro.getBestEscapeDir();
        	if (retreatDir == null)
        		Action.tryViperAttack();
        	else
        	{
        		if (!Nav.tryGoTo(here.add(retreatDir), Micro.getCanMoveDirs()))
        			Action.tryViperAttack();
        	}
        }
        else if (enemies != null && enemies.length > 0)
        {
        	Debug.setStringRR("trying to go to enemy COM");
        	Nav.tryGoTo(Micro.getEnemyCOM(),Micro.getSafeMoveDirs());
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
        			enemyLoc = MapInfo.getExplorationWaypoint();
        	}
        	else
        	{
        		enemyLoc = farArchon;
        	}
        	Debug.setStringSJF("target = " + enemyLoc.toString() + ", enemies = " + enemies.length);
        }
        
        // if there are zombies, try to retreat towards enemy
        if (Micro.getNearbyZombies() != null && Micro.getNearbyZombies().length > 0)
        {
        	Debug.setStringRR("zombie retreat toward enemy loc");
        	Action.tryRetreatTowards(enemyLoc, Micro.getCanMoveDirs());
        }
        
        // try to go to enemy (will dig if necessary)
        //Debug.setStringRR("going to enemy loc");
        Nav.tryGoTo(enemyLoc, Micro.getCanMoveDirs());
        
        return true;

	}
}
