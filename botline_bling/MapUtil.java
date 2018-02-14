package botline_bling;

import battlecode.common.*;

public class MapUtil extends RobotPlayer
{
	private static final int[] evenLocOffX = {-1,0,0,1,-2,-2,-1,-1,1,1,2,2,-3,0,0,3,-3,-3,-2,-2,2,2,3,3,-4,-4,-1,-1,1,1,4,4,-5,-4,-4,-3,-3,0,0,3,3,4,4,5,-5,-5,-2,-2,2,2,5,5,-6,-6,-1,-1,1,1,6,6,-5,-5,-4,-4,4,4,5,5,-6,-6,-3,-3,3,3,6,6,-7,0,0,7,-7,-7,-2,-2,2,2,7,7};
	private static final int[] evenLocOffY = {0,-1,1,0,-1,1,-2,2,-2,2,-1,1,0,-3,3,0,-2,2,-3,3,-3,3,-2,2,-1,1,-4,4,-4,4,-1,1,0,-3,3,-4,4,-5,5,-4,4,-3,3,0,-2,2,-5,5,-5,5,-2,2,-1,1,-6,6,-6,6,-1,1,-4,4,-5,5,-5,5,-4,4,-3,3,-6,6,-6,6,-3,3,0,-7,7,0,-2,2,-7,7,-7,7,-2,2};
	private static final int[] oddLocOffX = {-1,-1,1,1,-2,0,0,2,-2,-2,2,2,-3,-3,-1,-1,1,1,3,3,-4,0,0,4,-3,-3,3,3,-4,-4,-2,-2,2,2,4,4,-5,-5,-1,-1,1,1,5,5,-4,-4,4,4,-5,-5,-3,-3,3,3,5,5,-6,0,0,6,-6,-6,-2,-2,2,2,6,6,-7,-7,-5,-5,-1,-1,1,1,5,5,7,7,-6,-6,-4,-4,4,4,6,6};
	private static final int[] oddLocOffY = {-1,1,-1,1,0,-2,2,0,-2,2,-2,2,-1,1,-3,3,-3,3,-1,1,0,-4,4,0,-3,3,-3,3,-2,2,-4,4,-4,4,-2,2,-1,1,-5,5,-5,5,-1,1,-4,4,-4,4,-3,3,-5,5,-5,5,-3,3,0,-6,6,0,-2,2,-6,6,-6,6,-2,2,-1,1,-5,5,-7,7,-7,7,-5,5,-1,1,-4,4,-6,6,-6,6,-4,4};

	
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
