package another_one_bots_the_dust;

import battlecode.common.*;
import java.util.*;

// uses bitmasks to keep track of a list of directions
// (including NONE)
public class DirectionSet
{
	static final Random rand = new Random();
	static final int[] dirOffsets = {1,7,2,6,3,5,4};
	static final int[] dirOffsetsTowards = {1,7};
	
	public int dirs = 0;
	
	// don't really need to do any construction
	public DirectionSet()
	{
	}
	
	private DirectionSet(int ds)
	{
		this.dirs = ds;
	}
	
	public void add(Direction d)
	{
		dirs |= (1<<d.ordinal());
	}
	
	public void remove(Direction d)
	{
		dirs &= ~(1<<d.ordinal());
	}
	
	public DirectionSet clone()
	{
		return new DirectionSet(this.dirs);
	}
	
	public DirectionSet and(DirectionSet d)
	{
		return new DirectionSet(dirs & d.dirs);
	}
	
	public DirectionSet or(DirectionSet d)
	{
		return new DirectionSet(dirs | d.dirs);
	}
	
	public boolean any()
	{
		return dirs != 0;
	}
	
	public boolean isValid(Direction d)
	{
		return ((1 << d.ordinal()) & dirs) > 0;
	}
	
	public Direction getRandomValid()
	{
		if (!any())
			return null;
		
		int nbits = Integer.bitCount(dirs);
		int dir_bit = 1+rand.nextInt(nbits);

		int a = 1;
		
		for (int i=0; i<9; i++)
		{
			if ( (a&dirs) > 0 )
				dir_bit--;
			
			if (dir_bit == 0)
				return Direction.values()[i];
			
			a <<= 1;
		}
		
		return null;
	}
	
	public Direction getDirectionTowards(Direction dir)
	{
		MapLocation loc = new MapLocation(0,0);
		return getDirectionTowards(loc, loc.add(dir));
	}
	
	// returns null if no direction that moves us closer to destination
	public Direction getDirectionTowards(MapLocation from, MapLocation to)
	{
		// if they're just straight up null
		if (to == null || from == null)
			return getRandomValid();
		
		Direction best = from.directionTo(to);
		
		// first handle base case
		if (isValid(best))
			return best;
		
		// how far are we
		int distSq = from.distanceSquaredTo(to);
		
		// try both left and right turns
		// if these didn't work
		int rightDist = distSq;
		int leftDist = distSq;
		
		if (isValid(best.rotateRight()))
			rightDist = from.add(best.rotateRight()).distanceSquaredTo(to);
		if (isValid(best.rotateLeft()))
			leftDist = from.add(best.rotateLeft()).distanceSquaredTo(to);
		
		// pew pew pew
		if (rightDist < leftDist)
			return best.rotateRight();
		else if (leftDist < rightDist)
			return best.rotateLeft();
		else if (leftDist < distSq)
			return rand.nextBoolean() ? best.rotateRight() : best.rotateLeft();
		
		/*
		// still none. look to sides
		int rightRightDist = distSq;
		int leftLeftDist = distSq;
		
		if (isValid(best.rotateRight().rotateRight()))
			rightDist = from.add(best.rotateRight().rotateRight()).distanceSquaredTo(to);
		if (isValid(best.rotateLeft().rotateLeft()))
			leftDist = from.add(best.rotateLeft().rotateLeft()).distanceSquaredTo(to);
		
		// pew pew pew
		if (rightRightDist < leftLeftDist)
			return best.rotateRight().rotateRight();
		else if (leftLeftDist < rightRightDist || leftLeftDist < distSq)
			return best.rotateLeft().rotateLeft();
		*/
		return null;
	}
	
	public static DirectionSet getOddSquares(MapLocation currentLoc)
	{
		return getParitySquares(currentLoc, true);
	}
	
	public static DirectionSet getEvenSquares(MapLocation currentLoc)
	{
		return getParitySquares(currentLoc, false);
	}
	
	public static DirectionSet getParitySquares(MapLocation loc, boolean isOdd)
	{
		if (loc == null)
			return null;

		DirectionSet ds = new DirectionSet();		
		
		Direction d0 = Direction.NORTH;
		
		if (MapUtil.isLocOdd(loc) == isOdd)
		{
			// d0 should be diagonal, and add here
			d0 = d0.rotateRight();
			ds.add(Direction.NONE);
		}
		
		for (int i=0; i<4; i++)
		{
			ds.add(d0);
			d0 = d0.rotateRight().rotateRight();
		}
		
		return ds;
	}
	
	public static DirectionSet makeAll()
	{
		return new DirectionSet(511);

	}
	
	public static DirectionSet makeStay()
	{
		return new DirectionSet(1<<Direction.NONE.ordinal());
	}
}
