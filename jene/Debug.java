package jene;

import battlecode.common.Clock;

public class Debug extends RobotPlayer
{
	static final boolean DISPLAY_DEBUG = false;
	static String[] auStrings = new String[4];
	static int timer_start;
	
	static final String[] song = {"You","know","it's","all","about","that","bass","/","'bout","that","bass","/","no","treble"};
	static final String[] piece = {"Pawn", "Rook", "Bishop", "Knight", "King", "Queen"};
	static final String rows = "ABCDEFGH";
	
	public static void startTiming()
	{
		timer_start = Clock.getBytecodeNum();
	}
	
	public static void stopTiming(String s)
	{
		int dt = Clock.getBytecodeNum() - timer_start;
		if (dt < 0) // wrapped around
			dt += rc.getType().bytecodeLimit;
		
		System.out.println(s + " time:" + dt);
	}
	
	public static void setStringAK(String s)
	{
		auStrings[0] = s;
		update();
	}
	
	public static void setStringSJF(String s)
	{
		auStrings[1] = s;
		update();
	}
	
	public static void setStringTS(String s)
	{
		auStrings[2] = s;
		update();
	}
	
	public static void setStringRR(String s)
	{
		auStrings[3] = s;
		update();
	}
	
	static void update()
	{
		if (!DISPLAY_DEBUG)
		{
			rc.setIndicatorString(0,piece[rand.nextInt(6)] + " to " + rows.charAt(rand.nextInt(8)) + "" + (rand.nextInt(8)+1));
			rc.setIndicatorString(1,song[rc.getRoundNum()%song.length]);
			return;
		}
		
		rc.setIndicatorString(0, "AK: " + auStrings[0]);
		rc.setIndicatorString(1, "SJF: " + auStrings[1]);
		rc.setIndicatorString(2, "TS: " + auStrings[2] + " || RR: " + auStrings[3]);		
	}
}
