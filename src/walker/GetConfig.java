package walker;

import java.util.HashSet;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import net.Network;

import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import walker.Info.PointType;

public class GetConfig {
	public static void parse(Document doc) throws Exception {
		try {
			XPathFactory factory = XPathFactory.newInstance();
			XPath xpath = factory.newXPath();

			// game version
			Network.GAME_VERSION = xpath.evaluate("/config/gameversion", doc);

			// login info
			Info.LoginId = xpath.evaluate("/config/username", doc);
			Info.LoginPw = xpath.evaluate("/config/password", doc);

			// cards for sell
			NodeList idl = (NodeList) xpath.evaluate("/config/sell_card/id",
					doc, XPathConstants.NODESET);
			Info.CanBeSold = new HashSet<Integer>();
			for (int i = 0; i < idl.getLength(); i++) {
				Node idx = idl.item(i);
				try {
					Info.CanBeSold.add(Integer.valueOf(idx.getFirstChild()
							.getNodeValue()));
				} catch (Exception ex) {
					ex.getMessage();
				}
			}

			// 优先妖精战
			Info.FairyBattleFirst = xpath.evaluate(
					"/config/option/fairy_battle_first", doc).equals("1");

			// BC不足时依旧跑图
			Info.AllowBCInsuffient = xpath.evaluate(
					"/config/option/allow_bc_insuffient", doc).equals("1");

			// 跑日常图
			Info.DailyMap = xpath.evaluate("/config/option/daily_map", doc)
					.equals("1");

			// 日常图编号
			for (String no : xpath.evaluate("/config/option/daily_map_no", doc)
					.split(",")) {
				Info.DailyMapNo.add(Integer.valueOf(no.trim()));
			}

			// 表图跑最低cost图
			Info.MinAPOnly = xpath.evaluate("/config/option/min_ap_only", doc)
					.equals("1");

			// 里图跑最低cost图
			Info.MinAPOnlyInner = xpath.evaluate(
					"/config/option/min_ap_only_inner", doc).equals("1");

			// 优先跑里图
			Info.InnerMapFirst = xpath.evaluate(
					"/config/option/inner_map_first", doc).equals("1");

			// 只跑里图
			Info.InnerMapOnly = xpath.evaluate("/config/option/inner_map_only",
					doc).equals("1");

			// 第一张里图编号
			Info.firstInnerMapNo = Integer.valueOf(xpath.evaluate(
					"/config/option/first_inner_map_no", doc));

			// 自动每隔5分钟刷新地图列表
			Info.AutoRefreshMap = xpath.evaluate(
					"/config/option/auto_refresh_map", doc).equals("1");

			// 升级自动加点
			String pointType = xpath.evaluate("/config/option/auto_add_point",
					doc);
			if ("1".equals(pointType)) {
				Info.AutoAddPointType = PointType.AP;
			} else {
				Info.AutoAddPointType = PointType.BC;
			}

			// 自己妖怪未打死前不继续探索
			Info.StopExploreWhenFairyAlive = xpath.evaluate(
					"/config/option/stop_explore_when_fairy_alive", doc)
					.equals("1");

			// 优先打剩余时间少的妖精
			Info.LessTimeFairyFirst = xpath.evaluate(
					"/config/option/less_time_fairy_first", doc).equals("1");

			// 自动收箱子
			Info.AutoReceiveBox = xpath.evaluate(
					"/config/option/auto_receive_box", doc).equals("1");

			// 自动卖卡
			Info.AutoSellCard = xpath.evaluate("/config/sell_card/enable", doc)
					.equals("1");

			// 最大卖卡金额
			Info.MaxSellCardMoney = Integer.parseInt(xpath.evaluate(
					"/config/sell_card/max_card_money", doc));

			// 跑固定图
			Info.OneMapOnly = xpath.evaluate("/config/map/enable", doc).equals(
					"1");
			if (Info.OneMapOnly == true) {
				Info.OneMapId = Integer.parseInt(xpath.evaluate(
						"/config/map/id", doc));
				Info.OneMapCost = Integer.parseInt(xpath.evaluate(
						"/config/map/cost", doc));
			}

			// 登录时自动消耗伴点
			Info.DropFriendshipPoint = xpath.evaluate(
					"/config/option/drop_friendship_point", doc).equals("1");

			/*
			 * 吃药相关
			 */
			Info.AutoUseAp = xpath.evaluate("/config/use/auto_use_ap", doc)
					.equals("1");
			if (Info.AutoUseAp == true) {
				String half = xpath.evaluate("/config/use/strategy/ap/half",
						doc);
				if (half.equals("0")) {
					Info.AutoApType = Info.AutoUseType.FULL_ONLY;
				} else if (half.equals("1")) {
					Info.AutoApType = Info.AutoUseType.HALF_ONLY;
				} else if (half.equals("2")) {
					Info.AutoApType = Info.AutoUseType.ALL;
				}

				Info.AutoApLow = Integer.parseInt(xpath.evaluate(
						"/config/use/strategy/ap/low", doc));
				Info.AutoApFullLow = Integer.parseInt(xpath.evaluate(
						"/config/use/strategy/ap/full_low", doc));
			}

			Info.AutoUseBc = xpath.evaluate("/config/use/auto_use_bc", doc)
					.equals("1");
			if (Info.AutoUseBc == true) {
				String half = xpath.evaluate("/config/use/strategy/bc/half",
						doc);
				if (half.equals("0")) {
					Info.AutoBcType = Info.AutoUseType.FULL_ONLY;
				} else if (half.equals("1")) {
					Info.AutoBcType = Info.AutoUseType.HALF_ONLY;
				} else if (half.equals("2")) {
					Info.AutoBcType = Info.AutoUseType.ALL;
				}

				Info.AutoBcLow = Integer.parseInt(xpath.evaluate(
						"/config/use/strategy/bc/low", doc));
				Info.AutoBcFullLow = Integer.parseInt(xpath.evaluate(
						"/config/use/strategy/bc/full_low", doc));
			}

			// 调试，输出xml
			Info.Debug = xpath.evaluate("/config/option/debug", doc)
					.equals("1");

			// 外敌卡组
			Info.guildFairyDeck.no = xpath.evaluate(
					"/config/deck/deck_profile[name='GuildFairyDeck']/no", doc);
			Info.guildFairyDeck.battleCost = Integer
					.parseInt(xpath
							.evaluate(
									"/config/deck/deck_profile[name='GuildFairyDeck']/bc",
									doc));

			// 舔妖卡組
			Info.fairyDeck.no = xpath.evaluate(
					"/config/deck/deck_profile[name='FairyDeck']/no", doc);
			Info.fairyDeck.battleCost = Integer.parseInt(xpath.evaluate(
					"/config/deck/deck_profile[name='FairyDeck']/bc", doc));

			// 打觉醒卡組
			Info.rareFairyDeck.no = xpath.evaluate(
					"/config/deck/deck_profile[name='RareFairyDeck']/no", doc);
			Info.rareFairyDeck.battleCost = Integer.parseInt(xpath.evaluate(
					"/config/deck/deck_profile[name='RareFairyDeck']/bc", doc));

			// 尾刀卡組
			Info.koDeck.no = xpath.evaluate(
					"/config/deck/deck_profile[name='KODeck']/no", doc);
			Info.koDeck.battleCost = Integer.parseInt(xpath.evaluate(
					"/config/deck/deck_profile[name='KODeck']/bc", doc));
			Info.killFairyHp = Long.parseLong(xpath.evaluate(
					"/config/deck/deck_profile[name='KODeck']/hp_kill", doc));
		} catch (Exception ex) {
			if (ErrorData.currentErrorType == ErrorData.ErrorType.none)
				throw ex;
		}
	}
}
