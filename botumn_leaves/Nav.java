package botumn_leaves;

import java.util.ArrayList;

import battlecode.common.*;

public class Nav extends RobotPlayer
{
    private static MapLocation myDest;
    private static DirectionSet myDirs;

    private enum BugState
    {
        DIRECT, BUG
    }

    public enum WallSide
    {
        LEFT, RIGHT
    }

    private static BugState bugState;
    public static WallSide bugWallSide = WallSide.LEFT;
    private static int bugStartDistSq;
    private static Direction bugLastMoveDir;
    private static Direction bugLookStartDir;
    private static int bugRotationCount;
    private static int bugMovesSinceSeenObstacle = 0;

    private static boolean tryMoveDirect() throws GameActionException
    {
        Direction toDest = myDirs.getDirectionTowards(here, myDest);
        
        if (toDest == null)
        	return false;
        
        return Action.tryMove(toDest);
    }

    private static void startBug() throws GameActionException
    {
        bugStartDistSq = here.distanceSquaredTo(myDest);
        bugLastMoveDir = here.directionTo(myDest);
        bugLookStartDir = here.directionTo(myDest);
        bugRotationCount = 0;
        bugMovesSinceSeenObstacle = 0;

        // try to intelligently choose on which side we will keep the wall
        Direction leftTryDir = bugLastMoveDir.rotateLeft();
        for (int i = 0; i < 3; i++)
        {
            if (!myDirs.isValid(leftTryDir))
            	leftTryDir = leftTryDir.rotateLeft();
            else break;
        }
        Direction rightTryDir = bugLastMoveDir.rotateRight();
        for (int i = 0; i < 3; i++)
        {
            if (!myDirs.isValid(rightTryDir)) rightTryDir = rightTryDir.rotateRight();
            else break;
        }
        if (myDest.distanceSquaredTo(here.add(leftTryDir)) < myDest.distanceSquaredTo(here.add(rightTryDir)))
        {
            bugWallSide = WallSide.RIGHT;
        }
        else
        {
            bugWallSide = WallSide.LEFT;
        }
    }

    private static Direction findBugMoveDir() throws GameActionException
    {
        bugMovesSinceSeenObstacle++;
        Direction dir = bugLookStartDir;
        for (int i = 8; i-- > 0;)
        {
            if (myDirs.isValid(dir)) 
            	return dir;
            dir = (bugWallSide == WallSide.LEFT ? dir.rotateRight() : dir.rotateLeft());
            bugMovesSinceSeenObstacle = 0;
        }
        return null;
    }

    private static int numRightRotations(Direction start, Direction end)
    {
        return (end.ordinal() - start.ordinal() + 8) % 8;
    }

    private static int numLeftRotations(Direction start, Direction end)
    {
        return (-end.ordinal() + start.ordinal() + 8) % 8;
    }

    private static int calculateBugRotation(Direction moveDir)
    {
        if (bugWallSide == WallSide.LEFT)
        {
            return numRightRotations(bugLookStartDir, moveDir) - numRightRotations(bugLookStartDir, bugLastMoveDir);
        }
        else
        {
            return numLeftRotations(bugLookStartDir, moveDir) - numLeftRotations(bugLookStartDir, bugLastMoveDir);
        }
    }

    private static boolean bugMove(Direction dir) throws GameActionException
    {
        if (Action.tryMove(dir))
        {
            bugRotationCount += calculateBugRotation(dir);
        	bugLastMoveDir = dir;
        	
        	if (bugWallSide == WallSide.LEFT)
        		bugLookStartDir = dir.rotateLeft().rotateLeft();
        	else 
        		bugLookStartDir = dir.rotateRight().rotateRight();
        	
        	return true;
        }
        else
        {
        	return false;
        }
    }

    private static boolean detectBugIntoEdge() throws GameActionException
    {
        if (bugWallSide == WallSide.LEFT)
        {
            return !rc.onTheMap(here.add(bugLastMoveDir.rotateLeft()));
        }
        else
        {
            return !rc.onTheMap(here.add(bugLastMoveDir.rotateRight()));
        }
    }

    private static void reverseBugWallFollowDir() throws GameActionException
    {
        bugWallSide = (bugWallSide == WallSide.LEFT ? WallSide.RIGHT : WallSide.LEFT);
        startBug();
    }

    private static boolean bugTurn() throws GameActionException
    {
        if (detectBugIntoEdge())
        {
            reverseBugWallFollowDir();
        }
        
        Direction dir = findBugMoveDir();
        
        if (dir != null)
            return bugMove(dir);
        else
        	return false;
    }

    private static boolean canEndBug()
    {
        if (bugMovesSinceSeenObstacle >= 4)
        	return true;
        
        return (bugRotationCount <= 0 || bugRotationCount >= 8) && here.distanceSquaredTo(myDest) <= bugStartDistSq;
    }
    
    private static boolean isBlockedByObstacle()
    {
    	// set dir to the leftmost allowable dir 
    	Direction dir = here.directionTo(myDest);
    	dir = dir.rotateLeft().rotateLeft();
    	
    	// first check for turrets
    	RobotInfo[] adjRobots = rc.senseNearbyRobots(2, ourTeam);
    	for (RobotInfo ri : adjRobots)
    	{
    		if (ri.type == RobotType.TURRET && numRightRotations(dir,here.directionTo(ri.location)) < 5)
    			return true;
    	}
    	
    	// then check for rubble
    	for (int i=0; i<5; i++)
    	{
    		dir = dir.rotateRight();
    		if (rc.senseRubble(here.add(dir)) > GameConstants.RUBBLE_OBSTRUCTION_THRESH)
    			return true;
    	}
    	
    	return false;
    }

    private static boolean bugMove() throws GameActionException
    {

    	// Check if we can stop bugging at the *beginning* of the turn
        if (bugState == BugState.BUG)
        {
            if (canEndBug())
                bugState = BugState.DIRECT;
        }

        // If DIRECT mode, try to go directly to target
        if (bugState == BugState.DIRECT)
        {
            if (tryMoveDirect())
            {
            	// succeeded
            	return true;
            }
            else //if (isBlockedByObstacle())
            {
            	// failed, do bugging only if running into an actual wall
            	// (or a turret!)
                bugState = BugState.BUG;
                startBug();
            }/*
            else
            {
            	// don't move at all
            	return false;
            }*/
        }

        // If that failed, or if bugging, bug
        if (bugState == BugState.BUG)
        {
            return bugTurn();
        }
        
        // shouldn't be here, w/e
        return false;
    }
    
    public static boolean tryGoTo(MapLocation dest, DirectionSet dirs) throws GameActionException
    {
    	if (!rc.isCoreReady())
    		return false;
    	
    	myDirs = dirs;
    	
        if (!dest.equals(myDest))
        {
            myDest = dest;
            bugState = BugState.DIRECT;
        }
        
        if (here.equals(dest))
        	return true;

        return bugMove();
    }
}
