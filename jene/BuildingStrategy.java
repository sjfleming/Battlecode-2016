package jene;

import battlecode.common.*;

import java.util.*;

public class BuildingStrategy extends RobotPlayer implements Strategy
{
	// which strategy to use
	Strategy.Type myStrat = null;
	
	// creating the strategy actually forces us to build, so it better be valid
	public BuildingStrategy(RobotType robot, Direction dir, Strategy.Type strat) throws GameActionException
	{
		myStrat = strat;
		rc.build(dir, robot);
	}
	
	// takes over until we can move again, then sends a built signal
	// and exits the strategy
	public boolean tryTurn() throws GameActionException
	{
		if (!rc.isCoreReady())
			return true;
		
		Message.sendBuiltMessage(myStrat);
		return false;
	}
}
