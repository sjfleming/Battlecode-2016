package neutered_blitzkrieg_bot;

import battlecode.common.*;

public class UnitCounts
{
	public int Scouts;
	public int Soldiers;
	public int Turrets;
	public int TTMs;
	public int TurrTTMs;
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
				Turrets++;
				break;
			case TTM:
				TTMs++;
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
		TurrTTMs = Turrets + TTMs;
	}
}
