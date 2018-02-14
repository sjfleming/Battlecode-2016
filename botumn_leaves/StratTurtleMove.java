package botumn_leaves;

import battlecode.common.*;

import java.util.*;

// turtle move tries to allow units to move around close to or inside a turtle ball
public class StratTurtleMove extends RobotPlayer implements Strategy
{
	public String getName()
	{
		return "Turtle movement";
	}	

	public StratTurtleMove()
	{
	}
	
	public boolean tryTurn() throws GameActionException
	{
		switch (rc.getType())
		{
		case TURRET:
			Action.tryAttackSomeone();
			return true;
			
		case TTM:
			break;
			
		case SCOUT:
			break;
			
		case SOLDIER:
			break;
		}
		return true;
	}
}
