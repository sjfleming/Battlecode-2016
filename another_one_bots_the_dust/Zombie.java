package another_one_bots_the_dust;

import battlecode.common.*;

import java.util.*;

public class Zombie extends RobotPlayer
{
	private ZombieSpawnSchedule zSched;
	public int roundsToCount;
	
	private int bigZombieCount = -1;
	private int fastZombieCount = -1;
	private int rangedZombieCount = -1;
	private int stdZombieCount = -1;
	
	public Zombie(int rounds)
	{
		this.zSched = rc.getZombieSpawnSchedule();
		this.roundsToCount = rounds;
	}
	
	private void countEarlyZombies()
	{
		bigZombieCount = 0;
		fastZombieCount = 0;
		rangedZombieCount = 0;
		stdZombieCount = 0;
		
		if (zSched != null)
		{
			int[] zRounds = zSched.getRounds();
			if (zRounds != null && zRounds.length > 0)
			{
				// go through spawns
				for (int round : zRounds)
				{
					// if the spawns are before round we're counting up to
					if (round < roundsToCount)
					{
						// add them to counters
						for (ZombieCount count : zSched.getScheduleForRound(round))
						{
							switch (count.getType())
							{
							case BIGZOMBIE:
								bigZombieCount += count.getCount();
								break;
							case FASTZOMBIE:
								fastZombieCount += count.getCount();
								break;
							case RANGEDZOMBIE:
								rangedZombieCount += count.getCount();
								break;
							case STANDARDZOMBIE:
								stdZombieCount += count.getCount();
								break;
							default:
								break;
							}
						}
					}
				}
			}
		}
	}
	
	public int getNumEarlyBigZombies()
	{
		if (bigZombieCount > -1)
			return bigZombieCount;
		
		countEarlyZombies();
		return bigZombieCount;
	}

	public int getNumEarlyFastZombies()
	{
		if (fastZombieCount > -1)
			return fastZombieCount;
		
		countEarlyZombies();
		return fastZombieCount;
	}

	public int getNumEarlyRangedZombies()
	{
		if (rangedZombieCount > -1)
			return rangedZombieCount;
		
		countEarlyZombies();
		return rangedZombieCount;
	}

	public int getNumEarlyStdZombies()
	{
		if (stdZombieCount > -1)
			return stdZombieCount;
		
		countEarlyZombies();
		return stdZombieCount;
	}
}
