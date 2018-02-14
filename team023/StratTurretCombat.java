package team023;

import battlecode.common.*;

public class StratTurretCombat extends RobotPlayer implements Strategy
{
	// last round that a turret performed an action
	// (unpacking or firing)
	private int lastTurretRound = 0;
	private MapLocation lastDest;

	static final int PACK_DELAY = 15;
	
	
	
	public String getName()
	{
		return "Combat turret";
	}
	
	public boolean tryTurn() throws GameActionException
	{
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

		// target priority: nearby enemy > nearby ally > zombie den > our archon stuff
		lastDest = Message.getClosestAllyUnderAttack();
		if (lastDest == null)
			lastDest = MapInfo.getClosestDen();
		if (lastDest == null)
			lastDest = Waypoint.getBestEnemyLocation();
		if (lastDest == null)
			lastDest = MapInfo.ourArchonCenter;
		
		Action.tryGoToSafestOrRetreat(lastDest);
	}
	
	public boolean shouldUnpack() throws GameActionException
	{
		if (Micro.getNearbyHostiles().length > 0 || roundsSince(lastDamageRound)==0)
			return true;
		return false;
	}
}

