package botumn_leaves;

import battlecode.common.*;

import java.util.*;

public class StratBuilding extends RoboArchon implements Strategy
{
	// which strategy to use
	private Strategy.Type myStrat = null;
	private RobotType buildType = null;
	
	public String getName()
	{
		return "Building " + buildType + " for " + myStrat;
	}
	
	// creating the strategy actually forces us to build, so it better be valid
	public StratBuilding(RobotType robot, Direction dir, Strategy.Type strat) throws GameActionException
	{
		myStrat = strat;
		buildType = robot;
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
