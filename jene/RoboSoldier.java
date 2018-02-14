package jene;

import battlecode.common.*;

public class RoboSoldier extends RobotPlayer
{
	public static Strategy myStrategy;
	public static double myHealth;
	public static MapLocation lastBallLocation;
	
	public static void init() throws GameActionException
	{
//		if (rand.nextBoolean())
		myStrategy = new BallMoveStrategy(RobotPlayer.myBuilderID, 4, 10);
//		else
//			myStrategy = new MobFightStrat();
		
//		myStrategy = new BallMoveStrategy(RobotPlayer.myBuilderID, 2, 10);
		
		myHealth = rc.getType().maxHealth;
	}
	
	public static void turn() throws GameActionException
	{
//		if (!myStrategy.tryTurn())
//			myStrategy = new FreeUnitStrategy();
		if (rc.getHealth() < myHealth)
		{
			myHealth = rc.getHealth();
			Message.sendSignal(RobotType.SOLDIER.sensorRadiusSquared*2);
		}
		
		MapLocation closestDen = MapInfo.getClosestDen();
		MapLocation visibleDen = visibleDen();
		MapLocation closestAllyUnderAttack = Message.getClosestAllyUnderAttack(); // soldier reported
		if (visibleDen != null && !visibleDen.equals(closestAllyUnderAttack) && !visibleDen.equals(closestDen) && Micro.getNearbyZombies().length < 1)
		{
			Action.tryAttackSomeone();
			Message.sendSignal(RobotType.SOLDIER.sensorRadiusSquared*5);
			closestDen = visibleDen;
		}
		
		if (closestDen != null && here.distanceSquaredTo(closestDen) < 400)
		{
			lastBallLocation = here;
			myStrategy = new MobFightStrat(closestDen);
		}
		else if (closestAllyUnderAttack != null)
		{
			myStrategy = new MobFightStrat(closestAllyUnderAttack);
		}
		
		if (!myStrategy.tryTurn())
		{
			myStrategy = new MobFightStrat();
		}
//			myStrategy = new BallMoveStrategy(RobotPlayer.myBuilderID, 8, 16);
//			
//			// if i see an archon, ball around it
//			RobotInfo[] allies = Micro.getNearbyAllies();
//			for (RobotInfo bot : allies)
//			{
//				if (bot.type == RobotType.ARCHON)
//					myStrategy = new BallMoveStrategy(bot.ID, 8, 16);
//			}
//			
//			// if i don't, go back to last ball location
//			if (lastBallLocation != null && here.distanceSquaredTo(lastBallLocation) > RobotType.SOLDIER.sensorRadiusSquared)
//				Action.tryGoToWithoutBeingShot(lastBallLocation, Micro.getSafeMoveDirs());
//			else
//				myStrategy = new MobFightStrat();
//		}
	}
	
	public static MapLocation visibleDen() throws GameActionException
	{
		RobotInfo[] zombies = Micro.getNearbyZombies();
		
		if (zombies == null || zombies.length == 0)
			return null;
		
		for (RobotInfo ri : zombies)
		{
			if (ri.type == RobotType.ZOMBIEDEN)
				return ri.location;
		}
		return null;
	}
}
	