package info;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeSet;

public class BattleDB {
	public static ArrayList<FairyBattleInfo> hadBattleArrayList = new ArrayList<FairyBattleInfo>();
	public static SortedSet<FairyBattleInfo> notBattleSortedSet = new TreeSet<FairyBattleInfo>();
	public static HashMap<String, String> userMap = new HashMap<String, String>();

	// 在用户选择优先妖精的情况下，获取妖精列表为空时，此项为false，下次会优先进行探索
	public static boolean wantBattleFlag = true;
}
