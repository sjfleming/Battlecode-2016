package another_one_bots_the_dust;

import battlecode.common.*;

public class MapUtil extends RobotPlayer
{
	private static final int[] evenLocOffX = {-1,0,0,1,-2,-2,-1,-1,1,1,2,2,-3,0,0,3,-3,-3,-2,-2,2,2,3,3,-4,-4,-1,-1,1,1,4,4,-5,-4,-4,-3,-3,0,0,3,3,4,4,5,-5,-5,-2,-2,2,2,5,5,-6,-6,-1,-1,1,1,6,6,-5,-5,-4,-4,4,4,5,5,-6,-6,-3,-3,3,3,6,6,-7,0,0,7,-7,-7,-2,-2,2,2,7,7};
	private static final int[] evenLocOffY = {0,-1,1,0,-1,1,-2,2,-2,2,-1,1,0,-3,3,0,-2,2,-3,3,-3,3,-2,2,-1,1,-4,4,-4,4,-1,1,0,-3,3,-4,4,-5,5,-4,4,-3,3,0,-2,2,-5,5,-5,5,-2,2,-1,1,-6,6,-6,6,-1,1,-4,4,-5,5,-5,5,-4,4,-3,3,-6,6,-6,6,-3,3,0,-7,7,0,-2,2,-7,7,-7,7,-2,2};
	private static final int[] oddLocOffX = {-1,-1,1,1,-2,0,0,2,-2,-2,2,2,-3,-3,-1,-1,1,1,3,3,-4,0,0,4,-3,-3,3,3,-4,-4,-2,-2,2,2,4,4,-5,-5,-1,-1,1,1,5,5,-4,-4,4,4,-5,-5,-3,-3,3,3,5,5,-6,0,0,6,-6,-6,-2,-2,2,2,6,6,-7,-7,-5,-5,-1,-1,1,1,5,5,7,7,-6,-6,-4,-4,4,4,6,6};
	private static final int[] oddLocOffY = {-1,1,-1,1,0,-2,2,0,-2,2,-2,2,-1,1,-3,3,-3,3,-1,1,0,-4,4,0,-3,3,-3,3,-2,2,-4,4,-4,4,-2,2,-1,1,-5,5,-5,5,-1,1,-4,4,-4,4,-3,3,-5,5,-5,5,-3,3,0,-6,6,0,-2,2,-6,6,-6,6,-2,2,-1,1,-5,5,-7,7,-7,7,-5,5,-1,1,-4,4,-6,6,-6,6,-4,4};

	public static final int[] allOffsX = {-2,4,2,-1,-2,-2,7,1,-5,-2,-7,-2,0,6,6,6,4,3,-1,-6,7,4,7,0,5,4,5,4,-1,-3,5,-5,-1,0,-5,1,3,7,-3,4,-7,-4,3,3,-6,-2,0,-7,-4,0,4,-3,-4,-4,-5,5,4,-1,1,1,1,-3,-3,4,5,-3,3,6,-5,-2,7,-5,2,-6,3,6,-7,-4,-5,1,-4,2,6,0,0,-4,5,-2,1,0,1,4,-2,6,-3,2,1,1,4,-1,-3,-5,-4,-7,-6,-1,3,3,4,3,-1,4,5,1,-3,-2,-3,-1,-6,-3,-2,-1,-4,0,0,-4,5,-4,5,0,3,-2,2,-6,2,-5,3,-5,-1,-3,-1,2,2,3,-1,-5,-4,2,3,2,-6,2,5,-1,0,-4,-6,1,1,1,0,2,5,6,-1,-2,6,-2,-2,2,2,-3,-6,0,2,1};
	public static final int[] allOffsY = {-6,-4,7,3,3,2,-2,3,5,5,2,-5,-7,-1,0,-2,-6,-1,1,-2,1,-1,0,1,2,6,4,2,2,6,-4,0,-6,2,-3,4,-5,2,1,-5,-1,0,4,3,-1,-7,7,1,-6,-5,-3,-4,2,-3,-4,-3,5,-4,-3,-1,5,-5,2,4,-5,3,-4,-3,-1,7,-1,-2,3,4,0,2,0,6,-5,-2,-5,-3,1,4,6,4,0,1,7,-4,-7,1,-4,3,5,-5,-6,1,-2,5,-6,2,-4,-2,3,-5,-3,5,3,6,-1,0,3,-4,-1,-2,0,4,0,-2,4,-3,-1,3,-6,-2,1,1,-1,-3,-2,6,5,-4,-1,3,2,4,-2,4,0,-6,-7,1,6,1,3,-2,-6,6,-3,-4,5,-7,-1,5,1,0,2,-5,5,2,-2,-4,7,-3,4,-1,0,4,1,-3,2,-2,0,6};

	
	// AK check if map location is odd or even.
	public static boolean isLocOdd(MapLocation loc) {
		return ((loc.x+loc.y)&1)==1;
	}


	// AK check if map location is four-odd or four-even.
	public static boolean isFourOdd(MapLocation loc) {
		if (isLocOdd(loc)) {
			if ((loc.x)%2 == 1) {
				return true;
			}
			else {			
			return false;
			}
		}
		else {
			return false;
		}
	}
	
	public static boolean isGoodTurtle(MapLocation loc) throws GameActionException
	{
		return !rc.isLocationOccupied(loc)
				&& rc.senseRubble(loc) < GameConstants.RUBBLE_OBSTRUCTION_THRESH 
				&& rc.onTheMap(loc);
	}
	
	public static MapLocation findClosestTurtle() throws GameActionException
	{
		boolean isodd = isLocOdd(here);
		
		int[] dx = isodd ? oddLocOffX : evenLocOffX;
		int[] dy = isodd ? oddLocOffY : evenLocOffY;
				
		int finalind;
		
		switch (rc.getType())
		{
		case SCOUT:
			finalind = dx.length;
			break;
		case ARCHON:
			finalind = isodd ? 56:52;
			break;
		default:
			finalind = isodd ? 36:32;
			break;
		}

		//randomize start search direction for first shell
		int randFlipX = rand.nextBoolean() ? -1:1;
		int randFlipY = rand.nextBoolean() ? -1:1;
		
		for (int i=0; i<finalind; i++)
		{
			MapLocation testloc = here.add(dx[i]*randFlipX,dy[i]*randFlipY);
			if (isGoodTurtle(testloc))
			{
				return testloc;
			}
		}
		return null;
	}
}
