package another_one_bots_the_dust;

import battlecode.common.*;

import java.util.*;

public class StratZDay extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Activated Z-Day";
	}
	
	public static boolean shouldActivate()
	{
		if (rc.getType() == RobotType.ARCHON)
			return (rc.getRoundNum() > 2500 && rc.getRobotCount() > 100);
		else
			return (rc.getRoundNum() > 2700 && rc.getRobotCount() > 100);
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

		MapLocation closestTurret = Sighting.getClosestTurret();
		if (closestTurret == null)
			return false;
		
		MapLocation dest;
		
		int maxinfected = Math.max(rc.getViperInfectedTurns(),rc.getZombieInfectedTurns());
		
		switch (rc.getType())
		{
		case ARCHON:
			dest = Micro.getFarthestLocationFrom(MapInfo.getBunkerLocations(), closestTurret);
			Nav.tryGoTo(dest, Micro.getSafeMoveDirs());
			break;
			
		case VIPER:
			
			Action.tryFriendlyViperAttack();			
			break;
			
		default:
			// shoot at the enemy
			Action.tryAttackEnemy();
			
			// if we are infected, rush with reckless abandon
			if (maxinfected > 0)
				Nav.tryGoTo(closestTurret, Micro.getCanMoveDirs());
			// if we are about to not be infected, make sure we turn into a zombie
			if (maxinfected == 1)
				rc.disintegrate();
			break;
		}
				
		return true;
	}
}
