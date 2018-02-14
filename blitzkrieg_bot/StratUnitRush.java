package blitzkrieg_bot;

import battlecode.common.*;

import java.util.*;

public class StratUnitRush extends RobotPlayer implements Strategy
{
	private Strategy overrideStrategy = null;
	private MapLocation lastDest = null;
	private String myTask = "";
	
	public String getName()
	{
		if (overrideStrategy != null)
			return overrideStrategy.getName();

		return "Rushing: " + myTask + " at " + lastDest;
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
		/*
		if (rc.getHealth() < 20)
		{
			overrideStrategy = new StratUnitRetreat();
			overrideStrategy.tryTurn();
			return true;
		}
		*/
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
			myTask = "den";
			lastDest = MapInfo.getClosestDen();
		}
		if (lastDest == null)
		{
			myTask = "enemy";
			lastDest = Sighting.getClosestTurret();
		}
		if (lastDest == null)
		{
			myTask = "rally";
			lastDest = MapInfo.ourArchonCenter;
		}
		
		// this should basically always work if there are enemies
		if (Action.tryAttackSomeone())
			return true;

		// don't move if we just shot, must mean we're doing something right
		if (!rc.isWeaponReady())
			return true;
		
		Nav.tryGoTo(lastDest, Micro.getCanMoveDirs());
		//Rubble.doClearRubble(Rubble.getRandomAdjacentRubble());
		
		return true;
	}
}
