package team023;

import battlecode.common.*;

import java.util.*;

public class StratZDay extends RobotPlayer implements Strategy
{
	public static ArrayList<MapLocation> archonLocations = new ArrayList<MapLocation>();
	public static boolean receivedZDaySignal = false;
	
	public static final int ZDAY_ARCHON_ROUND = 2500;
	public static final int ZDAY_SIGNAL_ROUND = 2700;
	public static final int ZDAY_START_ROUND = ZDAY_SIGNAL_ROUND+5;
	public static final int ZDAY_UNIT_THRESH = 50;
	public static final int ZDAY_TURRET_THRESH = 5;
	public static final int ZDAY_MIN_SAFE_ROUNDS = 50;
	
	public String getName()
	{
		return "Activated Z-Day";
	}
	
	public static boolean tryArchonSendZDay() throws GameActionException
	{
		if (rc.getRoundNum() != ZDAY_SIGNAL_ROUND)
			return false;
		if (Waypoint.ZDayDest == null)
			return false;
		if (rc.getRobotCount() < ZDAY_UNIT_THRESH)
		{
			System.out.println("No zday, insufficient units");
			return false;
		}
		if (Sighting.enemySightedTurrets.elements().size() < ZDAY_TURRET_THRESH)
		{
			System.out.println("No zday, insufficient turrets");
			return false;
		}
		if (roundsSince(RobotPlayer.lastDangerRound) < ZDAY_MIN_SAFE_ROUNDS)
		{
			System.out.println("No zday, not safe enough");
			return false;
		}
		
		// welp, no turning back now. Z-day is here
		Message.sendMessageSignal(MapInfo.fullMapDistanceSq(), Message.Type.FREE_BEER, 1);
		return true;
	}
	
	public boolean tryTurn() throws GameActionException
	{
		MapLocation closestTurret = Sighting.getClosestTurret();
		if (closestTurret == null)
			return false;
		
		// insta-disintegrate if we're too close to our archon
		if (rc.getRoundNum() == ZDAY_START_ROUND)
		{
			if (here.distanceSquaredTo(Micro.getClosestLocationTo(archonLocations, here))
					< here.distanceSquaredTo(closestTurret))
				rc.disintegrate();
			if (rc.getType() == RobotType.SCOUT && here.distanceSquaredTo(closestTurret) > 200)
				rc.disintegrate();
		}
		
		int maxinfected = Math.max(rc.getViperInfectedTurns(),rc.getZombieInfectedTurns());
		
		switch (rc.getType())
		{			
		case VIPER:
			
			Action.tryFriendlyViperAttack();			
			break;
			
		default:
			// shoot at the enemy
			Action.tryAttackEnemy();
			
			// if we are infected, rush with reckless abandon
			if (maxinfected > 0)
				Action.tryAdjacentMoveToward(closestTurret);
			// if we are about to not be infected, make sure we turn into a zombie
			if (maxinfected < 5 && maxinfected > 0)
				rc.disintegrate();
			break;
		}
				
		return true;
	}
}
