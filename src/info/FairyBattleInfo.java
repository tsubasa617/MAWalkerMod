package info;

import walker.Info;

public class FairyBattleInfo implements Comparable<FairyBattleInfo> {
	public String guildId = "";
	public String userId = "";
	public String deckNo = "0";
	public String Spp = "dummy";

	/**
	 * 妖精（自）：P|S = 6 <br/>
	 * 妖精（自，觉醒）：P|S|R = 7 <br/>
	 * 妖精（他）：P = 4 <br/>
	 * 妖精（他，觉醒）：P|R = 5 <br/>
	 * 外敌： 0
	 */
	public int fairyType = 0;
	public static final int RARE = 1;
	public static final int SELF = 2;
	public static final int PRIVATE = 4;

	public String finder = "";
	public String serialId = "";
	public String fairyName = "";
	public String fairyLevel = "";
	public String discoverer_id = "";
	public String discoverer;
	public String hp;
	public String hp_max;
	public String rare_flg = "0";
	public String time_limit;

	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null) {
			return false;
		}

		if (o instanceof FairyBattleInfo == false) {
			return false;
		}

		FairyBattleInfo fbi = (FairyBattleInfo) o;

		if (this.serialId.equals(fbi.serialId)) {
			// same fairy
			return true;
		}

		return false;
	}

	public int hashCode() {
		return this.serialId != null ? this.serialId.hashCode() : 0;
	}

	public String toString() {
		return String.format("%s %s Lv%s HP:%s/%s 剩餘時間:%s分 卡組:%s",
				this.discoverer, this.fairyName, this.fairyLevel, this.hp,
				this.hp_max, this.time_limit, this.deckNo);
	}

	@Override
	public int compareTo(FairyBattleInfo o) {
		int val = 5;

		if (Info.LessTimeFairyFirst == true) {
			return (Integer.parseInt(this.time_limit) - val
					* Integer.parseInt(this.rare_flg))
					- (Integer.parseInt(o.time_limit) - val
							* Integer.parseInt(o.rare_flg));
		} else if (this.equals(o)) {
			return 0;
		} else {
			return 1;
		}
	}
}
