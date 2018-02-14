package jene;

import battlecode.common.*;

public class UnitCounts
{
	public int Scouts;
	public int Soldiers;
	public int Turrets;
	public int Archons;
	public int Guards;
	public int Vipers;
	
	public UnitCounts(RobotInfo[] units)
	{
		for (RobotInfo ri : units)
		{
			switch (ri.type)
			{
			case SCOUT:
				Scouts++;
				break;
			case SOLDIER:
				Soldiers++;
				break;
			case TURRET:
			case TTM:
				Turrets++;
				break;
			case ARCHON:
				Archons++;
				break;
			case GUARD:
				Guards++;
				break;
			case VIPER:
				Vipers++;
				break;
			default:
				break;
			}
		}
	}
}
