package action;

import info.FairyBattleInfo;
import info.Floor;

import java.util.ArrayList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.http.message.BasicNameValuePair;
import org.apache.http.NameValuePair;
import org.w3c.dom.Document;

import walker.ErrorData;
import walker.Go;
import walker.Info;
import walker.Process;
import walker.Go.LogType;
import walker.Info.EventType;

public class Explore {
	public static final Action Name = Action.EXPLORE;
	private static final String URL_EXPLORE = "http://web.million-arthurs.com/connect/app/exploration/guild_explore?cyt=1";
	private static byte[] response;

	public static boolean run() throws Exception {
		if (Info.StopExploreWhenFairyAlive == true
				&& Process.info.ownFairyKilled == false) {
			Go.log("自己的妖精還沒死，放棄探索！", LogType.INFO);
			return false;
		}

		if (Process.info.isCardFull == true) {
			Go.log("卡片已滿，停止探索", LogType.INFO);
			return false;
		}

		ArrayList<NameValuePair> post = new ArrayList<NameValuePair>();
		post.add(new BasicNameValuePair("area_id", Process.info.front.areaId));
		post.add(new BasicNameValuePair("auto_build", "1"));
		post.add(new BasicNameValuePair("floor_id", Process.info.front.floorId));
		try {
			response = Process.network
					.ConnectToServer(URL_EXPLORE, post, false);
		} catch (Exception ex) {
			if (ex.getMessage().startsWith("302")) {
				Process.info.events.push(Info.EventType.innerMapJump);
				return false;
			}

			ErrorData.currentDataType = ErrorData.DataType.text;
			ErrorData.currentErrorType = ErrorData.ErrorType.ConnectionError;
			ErrorData.text = ex.getMessage();
			throw ex;
		}

		Document doc;
		try {
			doc = Process.ParseXMLBytes(response, Name.name());
		} catch (Exception ex) {
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.ExploreDataError;
			ErrorData.bytes = response;
			throw ex;
		}

		XPathFactory factory = XPathFactory.newInstance();
		XPath xpath = factory.newXPath();
		try {
			String code = xpath.evaluate("/response/header/error/code", doc);
			if (!code.equals("0")) {
				if (code.equals("8000")) {
					Process.info.events.push(Info.EventType.cardFull);

					Process.info.isCardFull = true;
				}

				// 限时秘境消失
				if (xpath.evaluate("/response/header/error/code", doc)
						.endsWith("10000")) {
					Process.info.events.push(EventType.innerMapJump);
				}

				ErrorData.currentErrorType = ErrorData.ErrorType.ExploreResponse;
				ErrorData.currentDataType = ErrorData.DataType.text;
				ErrorData.text = xpath.evaluate(
						"/response/header/error/message", doc);
				return false;
			}

			Process.info.username = xpath.evaluate("//your_data/name", doc);
			Process.info.lv = Integer.parseInt(xpath.evaluate("//town_level",
					doc));
			Process.info.ap = Integer.parseInt(xpath.evaluate("//ap/current",
					doc));
			Process.info.apMax = Integer.parseInt(xpath.evaluate("//ap/max",
					doc));
			Process.info.bc = Integer.parseInt(xpath.evaluate("//bc/current",
					doc));
			Process.info.bcMax = Integer.parseInt(xpath.evaluate("//bc/max",
					doc));
			Process.info.guildId = xpath.evaluate("//your_data/party_id", doc);

			Process.info.SetTimeoutByAction(Name);

			Process.info.exp = Integer.parseInt(xpath.evaluate(
					"//explore/next_exp", doc));

			Process.info.ExploreProgress = xpath.evaluate("//explore/progress",
					doc);
			Process.info.ExploreExp = xpath.evaluate("//explore/gold", doc);
			Process.info.ExploreGold = xpath.evaluate("//explore/get_exp", doc);

			int evt = Integer.parseInt(xpath.evaluate("//explore/event_type",
					doc));
			switch (evt) {
			case 22:
				Process.info.fairy.fairyType = FairyBattleInfo.PRIVATE
						| FairyBattleInfo.SELF;
				Process.info.fairy.fairyName = xpath.evaluate(
						"//ex_fairy/fairy/name", doc);
				Process.info.fairy.fairyLevel = xpath.evaluate(
						"//ex_fairy/fairy/lv", doc);
				Process.info.fairy.serialId = xpath.evaluate(
						"//ex_fairy/fairy/serial_id", doc);
				Process.info.fairy.userId = xpath.evaluate(
						"//ex_fairy/fairy/discoverer_id", doc);
				Process.info.events.push(Info.EventType.privateFairyAppear);
				Process.info.events.push(Info.EventType.recvPFBGood);
				Process.info.ExploreResult = "妖精出現";
				break;
			case 5:
				if (((Boolean) xpath.evaluate("count(//next_floor)>0", doc,
						XPathConstants.BOOLEAN)).booleanValue()) {
					Floor f = new Floor();
					f.areaId = xpath.evaluate("//next_floor/area_id", doc);
					f.floorId = xpath.evaluate("//next_floor/floor_info/id",
							doc);
					f.cost = Integer.parseInt(xpath.evaluate(
							"//next_floor/floor_info/cost", doc));
					Process.info.front = f;
					Process.info.events.push(Info.EventType.gotoFloor);
					Process.info.ExploreResult = "本層跑完";
				} else {
					Process.info.events.push(Info.EventType.areaComplete);
					Process.info.ExploreResult = "全地區跑完";
				}
				break;
			case 12:
				Process.info.ExploreResult = String.format("AP+%d",
						new Object[] { Integer.valueOf(Integer.parseInt(xpath
								.evaluate("//explore/recover", doc))) });

				break;
			case 13:
				Process.info.ExploreResult = String.format("BC+%d",
						new Object[] { Integer.valueOf(Integer.parseInt(xpath
								.evaluate("//explore/recover", doc))) });

				break;
			case 19:
				int delta = Integer.parseInt(xpath.evaluate(
						"//special_item/after_count", doc))
						- Integer.parseInt(xpath.evaluate(
								"//special_item/before_count", doc));

				Process.info.ExploreResult = String.format("收集+%d",
						new Object[] { Integer.valueOf(delta) });
				break;
			case 2:
				Process.info.ExploreResult = "遇到玩家";
				break;
			case 3:
				Process.info.ExploreResult = "獲得妹紙卡";
				break;
			case 15:// 自动合成
				Process.info.ExploreResult = "獲得妹紙卡";
				break;
			case 4:
			case 6:
			case 7:
			case 8:
			case 9:
			case 10:
			case 11:
			case 14:
			case 16:
			case 17:
			case 18:
			case 20:
			case 21:
			default:
				Process.info.ExploreResult = "神馬都沒發生";
			}
		} catch (Exception ex) {
			if (ErrorData.currentErrorType != ErrorData.ErrorType.none)
				throw ex;
			ErrorData.currentDataType = ErrorData.DataType.bytes;
			ErrorData.currentErrorType = ErrorData.ErrorType.ExploreDataParseError;
			ErrorData.bytes = response;
			throw ex;
		}
		return true;
	}

}
