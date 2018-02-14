package neutered_blitzkrieg_bot;

import battlecode.common.*;

public class StratTurretCombat extends RobotPlayer implements Strategy
{
	// last round that a turret performed an action
	// (unpacking or firing)
	private int lastTurretRound = 0;

	static final int PACK_DELAY = 50;
	
	public double myHealth = RobotType.TURRET.maxHealth;
	
	
	public String getName()
	{
		return "Turtle turret";
	}
	
	public boolean tryTurn() throws GameActionException
	{
		if (rc.getHealth() < myHealth)
		{
			myHealth = rc.getHealth();
			Message.sendSignal(RobotType.TURRET.sensorRadiusSquared*2);
		}

		switch (rc.getType())
		{
		case TURRET:
			turnTurret();
			return true;
		case TTM:
			turnTTM();
			return true;
		default:
			return false;
		}
	}

	public void turnTurret() throws GameActionException
	{
		// try shooting some shit
		if (Action.tryAttackSomeone())
		{
			lastTurretRound = rc.getRoundNum();
			return;
		}

		// if we fired recently, don't pack
		if (rc.getRoundNum() - lastTurretRound < PACK_DELAY || !rc.isCoreReady())
			return;
		
		if (MapUtil.isLocOdd(here))
			return;
		
		rc.pack();
		lastTurretRound = rc.getRoundNum();
	}

	public void turnTTM() throws GameActionException
	{
		if (!rc.isCoreReady())
			return;
		
		// if we see any enemies unpack and turn into a turret
		if (shouldUnpack())
		{
			rc.unpack();
			lastTurretRound = rc.getRoundNum();
			return;
		}
		// otherwise try moving towards the closest good turtle location
		MapLocation loc = MapUtil.findClosestTurtle();
		Nav.tryGoTo(loc, Micro.getSafeMoveDirs());
	}
	
	public boolean shouldUnpack() throws GameActionException
	{
		RobotInfo[] hostiles = Micro.getNearbyHostiles();
		RobotInfo closestHostile = Micro.getClosestUnitTo(hostiles, here);
		
		if (MapUtil.isLocOdd(here))
			return true;
		
		if (closestHostile != null)
			return true;
		else
			return false;		
	}
}

